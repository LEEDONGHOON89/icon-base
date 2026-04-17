package com.icon.agent.store;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;

/**
 * JDBC 수집기 전용 하이워터마크 저장소.
 * FilePositionStore와 동일한 atomic write 방식(tmp → fsync → rename)으로 crash-safe하게 저장한다.
 *
 * 저장 경로: data/{targetId}/{collectorId}/watermark.dat
 *
 * [2026-03-05] JdbcCollector가 FilePositionStore를 공용으로 사용하던 구조에서 분리 신규 생성.
 *              FileCollector  → FilePositionStore  (positions.dat)
 *              JdbcCollector  → JdbcWatermarkStore (watermark.dat)  ← 이 클래스
 */
public class JdbcWatermarkStore {

    private static final Logger log = LoggerFactory.getLogger(JdbcWatermarkStore.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Path storeFile;
    private final Path tmpFile;

    /**
     * @param targetId    대상 ID (e.g. "systemA")
     * @param collectorId 수집기 ID (e.g. "67ed8fa9-...")
     */
    public JdbcWatermarkStore(String targetId, String collectorId) throws IOException {
        this("data", targetId, collectorId);
    }

    public JdbcWatermarkStore(String rootPath, String targetId, String collectorId) throws IOException {
        Path dir = Path.of(rootPath).resolve(targetId).resolve(collectorId);
        Files.createDirectories(dir);
        this.storeFile = dir.resolve("watermark.dat");
        this.tmpFile   = dir.resolve("watermark.dat.tmp");
        log.info("JDBC 워터마크 저장소 초기화: {}", storeFile);
    }

    /**
     * 저장된 워터마크를 로드한다.
     * 파일이 없거나 파손된 경우 null을 반환한다 (초기값은 config에서 결정).
     */
    public JdbcWatermarkRecord load() {
        if (!Files.exists(storeFile)) {
            log.info("워터마크 파일 없음 ({}) — 초기값으로 시작", storeFile);
            return null;
        }
        try {
            JdbcWatermarkRecord record = JSON.readValue(storeFile.toFile(), JdbcWatermarkRecord.class);
            log.info("워터마크 로드: {} → {}", storeFile, record);
            return record;
        } catch (JsonParseException e) {
            log.warn("워터마크 파일 파손 ({}) — 초기값으로 시작: {}", storeFile, e.getMessage());
            backupCorrupted();
            return null;
        } catch (IOException e) {
            log.warn("워터마크 파일 읽기 실패 ({}) — 초기값으로 시작: {}", storeFile, e.getMessage());
            return null;
        }
    }

    /**
     * 워터마크를 원자적으로 저장한다 (tmp → fsync → atomic rename).
     */
    public void save(JdbcWatermarkRecord record) throws IOException {
        record.setLastUpdatedMs(System.currentTimeMillis());
        // 1. tmp 파일에 쓰기
        byte[] json = JSON.writeValueAsBytes(record);
        Files.write(tmpFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        // 2. fsync — 프로세스 크래시 후에도 tmp 파일 내용 보장
        try (FileChannel ch = FileChannel.open(tmpFile, StandardOpenOption.WRITE)) {
            ch.force(true);
        }
        // 3. atomic rename tmp → dat
        Files.move(tmpFile, storeFile,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
        log.debug("워터마크 저장 완료: {}", storeFile);
    }

    public Path getStoreFile() {
        return storeFile;
    }

    private void backupCorrupted() {
        Path backup = storeFile.resolveSibling("watermark.dat.corrupt." + System.currentTimeMillis());
        try {
            Files.move(storeFile, backup, StandardCopyOption.REPLACE_EXISTING);
            log.info("파손된 워터마크 파일 백업: {}", backup);
        } catch (IOException e) {
            log.warn("파손 파일 백업 실패: {}", e.getMessage());
        }
    }
}

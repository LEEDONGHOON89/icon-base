package com.icon.agent.store;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * File-based, crash-safe position store.
 * Stores a JSON map to: data/{targetId}/{collectorId}/positions.dat
 *
 * [2026-02-25] collectorId 기반 경로 지원 추가:
 *   - 기존: data/{targetId}/positions.dat  (target 단위 공유 → 다중 수집기 간 덮어쓰기 문제)
 *   - 변경: data/{targetId}/{collectorId}/positions.dat  (수집기별 독립 저장)
 *   여러 FileCollector가 동일 target에 속할 때 서로의 위치 정보를 덮어쓰는
 *   Race Condition을 방지한다.
 *
 * Write protocol (atomic + fsync):
 * 1. Serialize JSON to positions.dat.tmp
 * 2. FileChannel.force(true) — fsync to disk
 * 3. Files.move(tmp, dat, ATOMIC_MOVE, REPLACE_EXISTING)
 *
 * On load, catches JsonParseException for corrupted files and treats as empty
 * (safe restart).
 */
public class FilePositionStore implements PositionStore {

    private static final Logger log = LoggerFactory.getLogger(FilePositionStore.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, PositionRecord>> TYPE_REF = new TypeReference<>() {
    };

    private final Path storeFile;
    private final Path tmpFile;

    /**
     * [2026-02-25] 수집기별 개별 저장소 생성자.
     * 저장 경로: data/{targetId}/{collectorId}/positions.dat
     * 다중 FileCollector 간 위치 정보 덮어쓰기 방지
     */
    public FilePositionStore(String targetId, String collectorId) throws IOException {
        this("data", targetId, collectorId);
    }

    /**
     * [2026-02-25] 수집기별 개별 저장소 생성자 (rootPath 지정).
     * 저장 경로: {rootPath}/{targetId}/{collectorId}/positions.dat
     *
     * 테스트 코드에서는 TempDir을 rootPath로, targetId + collectorId를 서브 경로로 사용한다.
     * 운영 코드에서는 rootPath="data" 고정 후 targetId/collectorId 조합으로 저장한다.
     */
    public FilePositionStore(String rootPath, String targetId, String collectorId) throws IOException {
        // [2026-02-25] rootPath가 절대 경로인 경우(테스트용 TempDir 등) Path.of를 올바르게 처리
        Path base = Path.of(rootPath);
        Path dir = base.resolve(targetId).resolve(collectorId);
        Files.createDirectories(dir);
        this.storeFile = dir.resolve("positions.dat");
        this.tmpFile = dir.resolve("positions.dat.tmp");
        log.info("위치 저장소 초기화: {}", storeFile);
    }

    @Override
    public Map<String, PositionRecord> loadAll() throws IOException {
        if (!Files.exists(storeFile)) {
            log.info("No position file found at {} — starting fresh", storeFile);
            return new HashMap<>();
        }
        try {
            Map<String, PositionRecord> result = JSON.readValue(storeFile.toFile(), TYPE_REF);
            log.info("Loaded {} position record(s) from {}", result.size(), storeFile);
            return result;
        } catch (JsonParseException e) {
            log.warn("Position file {} is corrupted ({}). Treating as empty — will restart from beginning.",
                    storeFile, e.getMessage());
            // Rename corrupted file so we don't lose it, but start fresh
            Path corrupt = storeFile.resolveSibling("positions.dat.corrupt." + System.currentTimeMillis());
            try {
                Files.move(storeFile, corrupt, StandardCopyOption.REPLACE_EXISTING);
                log.info("Corrupted file renamed to {}", corrupt);
            } catch (IOException rename) {
                log.warn("Could not rename corrupted file: {}", rename.getMessage());
            }
            return new HashMap<>();
        }
    }

    @Override
    public void saveAll(Map<String, PositionRecord> positions) throws IOException {
        log.info("Saving {} position record(s) to {}", positions.size(), storeFile);
        // 1. Write to temp file
        byte[] json = JSON.writeValueAsBytes(positions);
        Files.write(tmpFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // 2. fsync the temp file to guarantee durability before rename
        try (FileChannel ch = FileChannel.open(tmpFile, StandardOpenOption.WRITE)) {
            ch.force(true);
        }

        // 3. Atomic rename: ATOMIC_MOVE is best-effort on some filesystems but still
        // safer than direct write
        Files.move(tmpFile, storeFile,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);

        log.debug("Persisted {} position record(s) to {}", positions.size(), storeFile);
    }

    @Override
    public void update(String key, PositionRecord record, Map<String, PositionRecord> currentPositions)
            throws IOException {
        record.setLastUpdatedMs(System.currentTimeMillis());
        currentPositions.put(key, record);
        saveAll(currentPositions);
    }

    public Path getStoreFile() {
        return storeFile;
    }
}

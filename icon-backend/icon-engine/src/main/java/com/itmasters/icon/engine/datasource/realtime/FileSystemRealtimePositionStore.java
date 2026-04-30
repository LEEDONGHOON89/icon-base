package com.itmasters.icon.engine.datasource.realtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Persists file position records for FILE_SYSTEM_REALTIME collectors to a JSON file.
 *
 * Store path: data/file-realtime/{dataSourceId}/positions.json
 *
 * Crash-safe write protocol:
 * 1. Write to positions.json.tmp
 * 2. FileChannel.force(true) -- fsync
 * 3. ATOMIC_MOVE to positions.json
 *
 * On corruption: archive to positions.json.corrupt.{timestamp} and start fresh.
 */
@Slf4j
@Component
public class FileSystemRealtimePositionStore {

    private static final String ROOT_DIR = "data/file-realtime";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, FileSystemRealtimePositionRecord>> TYPE_REF =
            new TypeReference<>() {};

    public Map<String, FileSystemRealtimePositionRecord> load(String dataSourceId) {
        Path storeFile = resolveStoreFile(dataSourceId);
        if (!Files.exists(storeFile)) {
            log.debug("[{}] No position file found -- starting fresh", dataSourceId);
            return new HashMap<>();
        }
        try {
            Map<String, FileSystemRealtimePositionRecord> result =
                    MAPPER.readValue(storeFile.toFile(), TYPE_REF);
            log.debug("[{}] Position file loaded -- {} files", dataSourceId, result.size());
            return result;
        } catch (Exception e) {
            log.warn("[{}] Position file corrupted ({}), resetting.", dataSourceId, e.getMessage());
            archiveCorrupted(storeFile);
            return new HashMap<>();
        }
    }

    public void save(String dataSourceId, Map<String, FileSystemRealtimePositionRecord> positions) {
        Path dir = resolveStoreDir(dataSourceId);
        Path storeFile = dir.resolve("positions.json");
        Path tmpFile = dir.resolve("positions.json.tmp");
        try {
            Files.createDirectories(dir);

            byte[] json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(positions);
            Files.write(tmpFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            try (FileChannel ch = FileChannel.open(tmpFile, StandardOpenOption.WRITE)) {
                ch.force(true);
            }

            Files.move(tmpFile, storeFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

            log.debug("[{}] Position file saved -- {} files", dataSourceId, positions.size());
        } catch (IOException e) {
            log.error("[{}] Position file save failed: {}", dataSourceId, e.getMessage(), e);
        }
    }

    public Path resolveStoreFile(String dataSourceId) {
        return resolveStoreDir(dataSourceId).resolve("positions.json");
    }

    // [2026-04-22] 파일 읽기 위치 초기화 — 처음부터 재수집 지원
    public void delete(String dataSourceId) {
        Path storeFile = resolveStoreFile(dataSourceId);
        try {
            if (Files.deleteIfExists(storeFile)) {
                log.info("[{}] Position file deleted for reset", dataSourceId);
            } else {
                log.info("[{}] Position file not found (already reset or never existed)", dataSourceId);
            }
        } catch (IOException e) {
            log.error("[{}] Failed to delete position file: {}", dataSourceId, e.getMessage(), e);
            throw new RuntimeException("파일 위치 정보 삭제 실패: " + e.getMessage(), e);
        }
    }

    private Path resolveStoreDir(String dataSourceId) {
        return Path.of(ROOT_DIR, dataSourceId);
    }

    private void archiveCorrupted(Path storeFile) {
        Path corrupt = storeFile.resolveSibling(
                "positions.json.corrupt." + System.currentTimeMillis());
        try {
            Files.move(storeFile, corrupt, StandardCopyOption.REPLACE_EXISTING);
            log.info("Corrupted position file archived: {}", corrupt);
        } catch (IOException e) {
            log.warn("Failed to archive corrupted position file: {}", e.getMessage());
        }
    }
}
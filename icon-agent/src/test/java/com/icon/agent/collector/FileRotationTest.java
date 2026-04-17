package com.icon.agent.collector;

import com.icon.agent.config.FileCollectorConfig;
import com.icon.agent.queue.RecordQueue;
import com.icon.agent.store.FilePositionStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileRotationTest {

    @TempDir
    Path tempDir;

    @Test
    void testRotationDetection() throws Exception {
        Path logFile = tempDir.resolve("app.log");
        Files.writeString(logFile, "line1\nline2\n");

        RecordQueue queue = new RecordQueue("test", 100);
        // [2026-02-25] 수집기별 개별 store 경로 적용: (rootPath, targetId, collectorId)
        FilePositionStore store = new FilePositionStore(tempDir.toString(), "test", "collector-0");
        FileCollectorConfig config = new FileCollectorConfig();
        config.setDirectory(tempDir.toString());
        config.setFileNamePattern("app.log");
        config.setPollIntervalMs(100);

        FileCollector collector = new FileCollector("test", config, queue, store);

        // 1. Initial poll
        collector.poll();
        assertEquals(2, queue.size());

        // 2. Add more data
        Files.writeString(logFile, "line3\n", java.nio.file.StandardOpenOption.APPEND);
        collector.poll();
        assertEquals(3, queue.size());

        // 3. Simulate rename rotation (logrotate style)
        Path rotatedFile = tempDir.resolve("app.log.1");
        Files.move(logFile, rotatedFile);
        Files.writeString(logFile, "new_line1\n"); // New file with same name

        collector.poll();
        // Should detect rotation and read from start of new file
        assertEquals(4, queue.size());

        // 4. Simulate copytruncate rotation
        Files.writeString(logFile, "short\n", java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        collector.poll();
        // Should detect that file shrank and reset offset
        assertEquals(5, queue.size());
    }
}

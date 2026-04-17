package com.icon.agent.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RestartResumeTest {

    @TempDir
    Path tempDir;

    @Test
    void testSaveAndLoadSurvivesRestart() throws IOException {
        String targetId = "systemA";
        // [2026-02-25] 수집기별 개별 store 경로 적용: (rootPath, targetId, collectorId)
        FilePositionStore store = new FilePositionStore(tempDir.toString(), targetId, "collector-0");

        Map<String, PositionRecord> positions = new HashMap<>();
        positions.put("file1", new PositionRecord("inode1", "/logs/app.log", 1024));
        positions.put("jdbc:target", new PositionRecord("jdbc:target", "SELECT...", 0));
        positions.get("jdbc:target").setJdbcLastValue("2026-02-23");

        // Save
        store.saveAll(positions);

        // Simulate "Restart" by creating a new store instance pointing to same dir
        // [2026-02-25] 동일 collectorId로 재생성 → 같은 경로의 positions.dat 로드
        FilePositionStore newStore = new FilePositionStore(tempDir.toString(), targetId, "collector-0");
        Map<String, PositionRecord> loaded = newStore.loadAll();

        assertEquals(2, loaded.size());
        assertEquals(1024, loaded.get("file1").getOffset());
        assertEquals("inode1", loaded.get("file1").getFileKey());
        assertEquals("2026-02-23", loaded.get("jdbc:target").getJdbcLastValue());
    }

    @Test
    void testAtomicWriteConsistency() throws IOException {
        String targetId = "systemB";
        // [2026-02-25] 수집기별 개별 store 경로 적용: (rootPath, targetId, collectorId)
        FilePositionStore store = new FilePositionStore(tempDir.toString(), targetId, "collector-0");

        Map<String, PositionRecord> positions = new HashMap<>();
        positions.put("key1", new PositionRecord("k1", "p1", 500));

        store.saveAll(positions);

        // Verify file exists
        assertTrue(store.getStoreFile().toFile().exists());

        // Update and save again
        positions.get("key1").setOffset(600);
        store.saveAll(positions);

        Map<String, PositionRecord> loaded = store.loadAll();
        assertEquals(600, loaded.get("key1").getOffset());
    }
}

package com.icon.agent.store;

import java.io.IOException;
import java.util.Map;

/**
 * Contract for a durable position store.
 * All implementations must be crash-safe.
 */
public interface PositionStore {

    /**
     * Load all stored positions. Returns an empty map if no data exists
     * or if the file is corrupted (treat as fresh start).
     */
    Map<String, PositionRecord> loadAll() throws IOException;

    /**
     * Atomically persist all positions to durable storage.
     * Must survive process crash (fsync before rename).
     */
    void saveAll(Map<String, PositionRecord> positions) throws IOException;

    /**
     * Convenience method: update a single record and persist.
     */
    void update(String key, PositionRecord record, Map<String, PositionRecord> currentPositions) throws IOException;
}

package com.icon.agent.batch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.icon.agent.collector.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A batch of records ready to deliver via RPC.
 */
public class Batch {

    private String batchId;
    private String targetId;
    private List<Record> records;
    private long createdAt;

    public Batch() {
    }

    @JsonCreator
    public Batch(@JsonProperty("targetId") String targetId,
            @JsonProperty("records") List<Record> records) {
        this.batchId = UUID.randomUUID().toString();
        this.targetId = targetId;
        this.records = records != null ? List.copyOf(records) : new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public String getBatchId() {
        return batchId;
    }

    public String getTargetId() {
        return targetId;
    }

    public List<Record> getRecords() {
        return records;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public int size() {
        return records.size();
    }

    @Override
    public String toString() {
        return "Batch{id='" + batchId + "', target='" + targetId + "', size=" + records.size() + '}';
    }
}

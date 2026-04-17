package com.itmasters.icon.engine.datasource.realtime;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Stores the last-read byte position for an incremental file collector.
 *
 * Serialized to positions.json.
 * Uses file key (inode on Linux, path+creationTime on Windows) to detect file rotation.
 */
public class FileSystemRealtimePositionRecord {

    @JsonProperty("filePath")
    private String filePath;

    /**
     * Unique file key.
     * Linux: inode-based, Windows: path@creationTime.
     * Used for rotation detection (rename / copytruncate).
     */
    @JsonProperty("fileKey")
    private String fileKey;

    /** Last-read byte offset. */
    @JsonProperty("offset")
    private long offset;

    /**
     * CSV header column list.
     * Parsed from the first line of the file on initial scan,
     * reused on subsequent reads.
     * Null for LOG/JSON formats.
     */
    @JsonProperty("headers")
    private List<String> headers;

    /** Timestamp of last position update (epoch ms). */
    @JsonProperty("lastUpdatedMs")
    private long lastUpdatedMs;

    public FileSystemRealtimePositionRecord() {
    }

    public FileSystemRealtimePositionRecord(String filePath, String fileKey, long offset) {
        this.filePath = filePath;
        this.fileKey = fileKey;
        this.offset = offset;
        this.lastUpdatedMs = System.currentTimeMillis();
    }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileKey() { return fileKey; }
    public void setFileKey(String fileKey) { this.fileKey = fileKey; }

    public long getOffset() { return offset; }
    public void setOffset(long offset) { this.offset = offset; }

    public List<String> getHeaders() { return headers; }
    public void setHeaders(List<String> headers) { this.headers = headers; }

    public long getLastUpdatedMs() { return lastUpdatedMs; }
    public void setLastUpdatedMs(long lastUpdatedMs) { this.lastUpdatedMs = lastUpdatedMs; }

    @Override
    public String toString() {
        return "FileSystemRealtimePositionRecord{filePath='" + filePath
                + "', fileKey='" + fileKey
                + "', offset=" + offset + '}';
    }
}
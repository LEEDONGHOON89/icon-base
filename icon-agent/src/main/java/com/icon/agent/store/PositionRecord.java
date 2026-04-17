package com.icon.agent.store;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 파일 수집기 전용 위치 레코드.
 *
 * [2026-03-05] JDBC watermark 필드(jdbcLastValue, jdbcLastValue2) 제거.
 *              JdbcWatermarkRecord/JdbcWatermarkStore 로 분리하여
 *              이 클래스는 FileCollector 전용(fileKey, filePath, offset)으로만 사용.
 */
public class PositionRecord {

    /**
     * Serialized file key (inode on Linux, volume+file-id on NTFS). Used for
     * rotation detection.
     */
    @JsonProperty("fileKey")
    private String fileKey;

    /** Canonical path at time of last read. Used as fallback on Windows. */
    @JsonProperty("filePath")
    private String filePath;

    /** Byte offset within the file at the last successful read. */
    @JsonProperty("offset")
    private long offset;

    /** Timestamp (epoch ms) of last write. */
    @JsonProperty("lastUpdatedMs")
    private long lastUpdatedMs;

    public PositionRecord() {
    }

    public PositionRecord(String fileKey, String filePath, long offset) {
        this.fileKey = fileKey;
        this.filePath = filePath;
        this.offset = offset;
        this.lastUpdatedMs = System.currentTimeMillis();
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getLastUpdatedMs() {
        return lastUpdatedMs;
    }

    public void setLastUpdatedMs(long lastUpdatedMs) {
        this.lastUpdatedMs = lastUpdatedMs;
    }

    @Override
    public String toString() {
        return "PositionRecord{fileKey='" + fileKey + "', filePath='" + filePath
                + "', offset=" + offset + "'}";
    }
}

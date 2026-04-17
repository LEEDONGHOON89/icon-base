package com.icon.agent.collector;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * A single collected data record.
 */
public class Record {

    public enum Source {
        FILE, JDBC
    }

    @JsonProperty("targetId")
    private String targetId;

    @JsonProperty("collectorId")
    private String collectorId;

    @JsonProperty("source")
    private Source source;

    @JsonProperty("sourceRef")
    private String sourceRef; // file path or table name

    @JsonProperty("content")
    private String content;

    @JsonProperty("collectedAt")
    private String collectedAt;

    @JsonProperty("metadata")
    private Map<String, String> metadata;

    public Record() {
    }

    @JsonCreator
    public Record(
            @JsonProperty("targetId") String targetId,
            @JsonProperty("collectorId") String collectorId,
            @JsonProperty("source") Source source,
            @JsonProperty("sourceRef") String sourceRef,
            @JsonProperty("content") String content,
            @JsonProperty("metadata") Map<String, String> metadata) {
        this.targetId = targetId;
        this.collectorId = collectorId;
        this.source = source;
        this.sourceRef = sourceRef;
        this.content = content;
        this.collectedAt = Instant.now().toString();
        this.metadata = metadata;
    }

    public String getTargetId() {
        return targetId;
    }

    public Source getSource() {
        return source;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public String getContent() {
        return content;
    }

    public String getCollectedAt() {
        return collectedAt;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public String getCollectorId() {
        return collectorId;
    }

    @Override
    public String toString() {
        return "Record{target='" + targetId + "', source=" + source
                + ", ref='" + sourceRef + "', len=" + content.length() + '}';
    }
}

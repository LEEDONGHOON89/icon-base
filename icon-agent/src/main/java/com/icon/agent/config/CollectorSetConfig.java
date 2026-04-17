package com.icon.agent.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * 단일 대상(target)의 파일 및 JDBC 수집기 설정을 그룹화한다.
 * [2026-02-25] 타입 구분자를 이용한 통합 수집기 목록 지원으로 업데이트
 */
public class CollectorSetConfig {

    // [2026-02-25] config.yaml의 통합 수집기 목록 지원 추가
    @JsonProperty("collectors")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = FileCollectorConfig.class, name = "FILE"),
        @JsonSubTypes.Type(value = JdbcCollectorConfig.class, name = "JDBC")
    })
    private List<CollectorConfig> collectors = new ArrayList<>();

    // [2026-02-25] 하위 호환성 유지를 위해 보존
    @JsonProperty("file")
    private List<FileCollectorConfig> file = new ArrayList<>();

    // [2026-02-25] 하위 호환성 유지를 위해 보존
    @JsonProperty("jdbc")
    private JdbcCollectorConfig jdbc = new JdbcCollectorConfig();

    /** 대상별 메모리 내 레코드 최대 버퍼 수 */
    @JsonProperty("queueCapacity")
    private int queueCapacity = 10000;

    /** 전송 배치당 최대 레코드 수 */
    @JsonProperty("maxBatchSize")
    private int maxBatchSize = 500;

    /** 불완전한 배치를 플러시하기 전 최대 대기 시간 (밀리초) */
    @JsonProperty("maxBatchMs")
    private long maxBatchMs = 2000;

    // [2026-02-25] 통합 수집기 목록 getter 추가
    public List<CollectorConfig> getCollectors() {
        return collectors;
    }

    // [2026-02-25] 통합 수집기 목록 setter 추가
    public void setCollectors(List<CollectorConfig> collectors) {
        this.collectors = collectors;
    }

    public List<FileCollectorConfig> getFile() {
        return file;
    }

    public void setFile(List<FileCollectorConfig> file) {
        this.file = file;
    }

    public JdbcCollectorConfig getJdbc() {
        return jdbc;
    }

    public void setJdbc(JdbcCollectorConfig jdbc) {
        this.jdbc = jdbc;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    public long getMaxBatchMs() {
        return maxBatchMs;
    }

    public void setMaxBatchMs(long maxBatchMs) {
        this.maxBatchMs = maxBatchMs;
    }
}

package com.icon.agent.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 대상별 설정: id, RPC 엔드포인트, 수집기 목록.
 * [2026-02-25] collectors를 List<CollectorConfig>로 직접 매핑하도록 변경
 *              (config.yaml의 collectors 배열 구조와 일치시킴)
 */
public class TargetConfig {

    @JsonProperty("id")
    private String id;

    @JsonProperty("rpc")
    private RpcConfig rpc = new RpcConfig();

    // [2026-02-25] config.yaml의 collectors 배열을 직접 역직렬화하기 위해
    //              CollectorSetConfig 객체 대신 List<CollectorConfig>로 변경
    @JsonProperty("collectors")
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = FileCollectorConfig.class, name = "FILE"),
        @JsonSubTypes.Type(value = JdbcCollectorConfig.class, name = "JDBC")
    })
    private List<CollectorConfig> collectors = new ArrayList<>();

    // [2026-02-25] 배치/큐 설정을 TargetConfig 레벨로 이동 (기존 CollectorSetConfig에서 이동)
    /** 대상별 메모리 내 레코드 최대 버퍼 수 */
    @JsonProperty("queueCapacity")
    private int queueCapacity = 10000;

    /** 전송 배치당 최대 레코드 수 */
    @JsonProperty("maxBatchSize")
    private int maxBatchSize = 500;

    /** 불완전한 배치를 플러시하기 전 최대 대기 시간 (밀리초) */
    @JsonProperty("maxBatchMs")
    private long maxBatchMs = 2000;

    // [2026-02-25] 1번: 배치 최대 바이트 크기 추가 (0이면 제한 없음)
    // 레코드 건수(maxBatchSize)와 함께 적용, 먼저 도달하는 기준으로 배치 분할
    /** 배치당 최대 바이트 크기. 기본값: 1MB (0=제한 없음) */
    @JsonProperty("maxBatchBytes")
    private long maxBatchBytes = 1_048_576;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RpcConfig getRpc() {
        return rpc;
    }

    public void setRpc(RpcConfig rpc) {
        this.rpc = rpc;
    }

    /** 수집기 목록. YAML 역직렬화 시 null일 수 있으므로 null-safe 반환 */
    public List<CollectorConfig> getCollectors() {
        return collectors != null ? collectors : Collections.emptyList();
    }

    public void setCollectors(List<CollectorConfig> collectors) {
        this.collectors = collectors != null ? collectors : new ArrayList<>();
    }

    // [2026-02-25] 큐/배치 설정 getter/setter 추가
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

    // [2026-02-25] 1번: maxBatchBytes getter/setter 추가
    public long getMaxBatchBytes() {
        return maxBatchBytes;
    }

    public void setMaxBatchBytes(long maxBatchBytes) {
        this.maxBatchBytes = maxBatchBytes;
    }
}

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
    // [2026-04-22] 튜닝 가이드 기본값: 500건 (소량 100, 대량 5000)
    private int maxBatchSize = 500;

    /** 불완전한 배치를 플러시하기 전 최대 대기 시간 (밀리초) */
    @JsonProperty("maxBatchMs")
    // [2026-04-22] 튜닝 가이드 기본값: 5000ms (폴링 주기 1분 환경에서 응답성과 효율 균형)
    private long maxBatchMs = 5000;

    // [2026-02-25] 1번: 배치 최대 바이트 크기 추가 (0이면 제한 없음)
    // 레코드 건수(maxBatchSize)와 함께 적용, 먼저 도달하는 기준으로 배치 분할
    /** 배치당 최대 바이트 크기. 기본값: 512KB (0=제한 없음) */
    @JsonProperty("maxBatchBytes")
    // [2026-04-22] 튜닝 가이드 기본값: 512KB (메모리 절약, 1MB → 512KB)
    private long maxBatchBytes = 524_288;

    // [2026-04-21] 스풀 크기 제한 — 디스크 고갈 방지
    /** 스풀 디렉토리 최대 파일 수. 기본값: 10000 (0=제한 없음) */
    @JsonProperty("maxSpoolFiles")
    private int maxSpoolFiles = 10_000;

    /** 스풀 디렉토리 최대 크기(MB). 기본값: 2048 MB (0=제한 없음) */
    @JsonProperty("maxSpoolSizeMb")
    private long maxSpoolSizeMb = 2048;

    // [2026-04-21] 전송 속도 제한 — 서버 부하 최소화
    /** 초당 최대 배치 전송 수. 기본값: 10 (서버 부하 방지) */
    @JsonProperty("maxBatchesPerSecond")
    // [2026-04-22] 튜닝 가이드 기본값: 10 batch/s (0=무제한 → 10으로 변경)
    private int maxBatchesPerSecond = 10;

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

    // [2026-04-21] 스풀 크기 제한 getter/setter
    public int getMaxSpoolFiles() {
        return maxSpoolFiles;
    }

    public void setMaxSpoolFiles(int maxSpoolFiles) {
        this.maxSpoolFiles = maxSpoolFiles;
    }

    public long getMaxSpoolSizeMb() {
        return maxSpoolSizeMb;
    }

    public void setMaxSpoolSizeMb(long maxSpoolSizeMb) {
        this.maxSpoolSizeMb = maxSpoolSizeMb;
    }

    // [2026-04-21] Rate Limit getter/setter
    public int getMaxBatchesPerSecond() {
        return maxBatchesPerSecond;
    }

    public void setMaxBatchesPerSecond(int maxBatchesPerSecond) {
        this.maxBatchesPerSecond = maxBatchesPerSecond;
    }
}

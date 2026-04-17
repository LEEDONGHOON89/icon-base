package com.icon.agent.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 대상별 JDBC 수집기 설정 (선택사항).
 * [2026-02-25] CollectorConfig 인터페이스 구현 및 config.yaml 신규 필드 지원으로 업데이트
 */
public class JdbcCollectorConfig implements CollectorConfig {

    // [2026-02-25] 통합 수집기 목록 지원을 위한 id 필드 추가
    @JsonProperty("id")
    private String id;

    // [2026-02-25] 통합 수집기 목록 지원을 위한 type 필드 추가
    @JsonProperty("type")
    private String type = "JDBC";

    // [2026-02-25] 통합 수집기 목록 지원을 위한 name 필드 추가
    @JsonProperty("name")
    private String name;

    @JsonProperty("enabled")
    private boolean enabled = false;

    @JsonProperty("url")
    private String url;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    /** 마지막으로 확인한 값을 위한 ? 플레이스홀더가 포함된 SQL 쿼리 */
    @JsonProperty("query")
    private String query;

    /** 하이워터마크 기준 컬럼명 */
    // field1 : ? 위치 1의 추적 컬럼명 (결과에서 읽어 다음 poll watermark로 사용)
    @JsonProperty("field1")
    private String field1;

    // field2 : ? 위치 2의 추적 컬럼명
    @JsonProperty("field2")
    private String field2;

    // [2026-02-25] field1_type 추가 — ? 파라미터 바인딩 타입 (STRING | TIMESTAMP | NUMBER)
    @JsonProperty("field1_type")
    private String field1Type;

    // [2026-02-25] field2_type 추가 — ? 파라미터 바인딩 타입
    @JsonProperty("field2_type")
    private String field2Type;

    // [2026-02-25] field1_value 추가
    @JsonProperty("field1_value")
    private String field1Value;

    // [2026-02-25] field2_value 추가
    @JsonProperty("field2_value")
    private String field2Value;

    /** 폴링 주기 (밀리초) */
    @JsonProperty("pollIntervalMs")
    private long pollIntervalMs = 5000;

    // [2026-02-25] CollectorConfig 인터페이스 구현을 위한 maxLinesPerPoll 추가
    @JsonProperty("maxLinesPerPoll")
    private int maxLinesPerPoll = 1000;

    // [2026-02-25] 3번: 단일 레코드 content 최대 바이트 크기 (0이면 제한 없음)
    // JDBC에서 BLOB/CLOB 컬럼 등 대용량 데이터 대비
    /** 레코드 content 최대 바이트. 기본값: 512KB (0=제한 없음) */
    @JsonProperty("maxRecordBytes")
    private int maxRecordBytes = 524_288;

    // [2026-02-25] CollectorConfig 인터페이스 구현
    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // [2026-02-25] CollectorConfig 인터페이스 구현
    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // [2026-02-25] CollectorConfig 인터페이스 구현
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // [2026-02-25] CollectorConfig 인터페이스 구현
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    // field1 getter/setter
    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    // field2 getter/setter
    public String getField2() {
        return field2;
    }

    public void setField2(String field2) {
        this.field2 = field2;
    }

    // [2026-02-25] field1_type getter/setter 추가
    public String getField1Type() {
        return field1Type != null ? field1Type.toUpperCase() : "STRING";
    }

    public void setField1Type(String field1Type) {
        this.field1Type = field1Type;
    }

    // [2026-02-25] field2_type getter/setter 추가
    public String getField2Type() {
        return field2Type != null ? field2Type.toUpperCase() : "STRING";
    }

    public void setField2Type(String field2Type) {
        this.field2Type = field2Type;
    }

    // [2026-02-25] field1_value getter/setter 추가
    public String getField1Value() {
        return field1Value;
    }

    public void setField1Value(String field1Value) {
        this.field1Value = field1Value;
    }

    // [2026-02-25] field2_value getter/setter 추가
    public String getField2Value() {
        return field2Value;
    }

    public void setField2Value(String field2Value) {
        this.field2Value = field2Value;
    }

    // [2026-02-25] CollectorConfig 인터페이스 구현
    @Override
    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    // [2026-02-25] CollectorConfig 인터페이스 구현
    @Override
    public int getMaxLinesPerPoll() {
        return maxLinesPerPoll;
    }

    public void setMaxLinesPerPoll(int maxLinesPerPoll) {
        this.maxLinesPerPoll = maxLinesPerPoll;
    }

    // [2026-02-25] 3번: maxRecordBytes getter/setter 추가
    @Override
    public int getMaxRecordBytes() {
        return maxRecordBytes;
    }

    public void setMaxRecordBytes(int maxRecordBytes) {
        this.maxRecordBytes = maxRecordBytes;
    }
}

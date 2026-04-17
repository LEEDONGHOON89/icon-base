package com.icon.agent.store;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JDBC 수집기 전용 하이워터마크 레코드.
 * field1 / field2 각각의 마지막 수집 기준값을 저장한다.
 *
 * 저장 경로: data/{targetId}/{collectorId}/watermark.dat
 *
 * [2026-03-05] FilePositionStore/PositionRecord 혼용 구조에서 분리 신규 생성.
 *              PositionRecord에 혼재하던 jdbcLastValue/jdbcLastValue2 필드를
 *              value1/value2 로 전용화하여 JdbcWatermarkStore와 함께 사용.
 */
public class JdbcWatermarkRecord {

    /** field1 마지막 watermark 값 (? 위치 1) */
    @JsonProperty("value1")
    private String value1;

    /** field2 마지막 watermark 값 (? 위치 2, null이면 미사용) */
    @JsonProperty("value2")
    private String value2;

    /** 마지막 저장 시각 (epoch ms) */
    @JsonProperty("lastUpdatedMs")
    private long lastUpdatedMs;

    public JdbcWatermarkRecord() {
    }

    public JdbcWatermarkRecord(String value1, String value2) {
        this.value1 = value1;
        this.value2 = value2;
        this.lastUpdatedMs = System.currentTimeMillis();
    }

    public String getValue1() {
        return value1;
    }

    public void setValue1(String value1) {
        this.value1 = value1;
    }

    public String getValue2() {
        return value2;
    }

    public void setValue2(String value2) {
        this.value2 = value2;
    }

    public long getLastUpdatedMs() {
        return lastUpdatedMs;
    }

    public void setLastUpdatedMs(long lastUpdatedMs) {
        this.lastUpdatedMs = lastUpdatedMs;
    }

    @Override
    public String toString() {
        return "JdbcWatermarkRecord{value1='" + value1 + "', value2='" + value2
                + "', lastUpdatedMs=" + lastUpdatedMs + "}";
    }
}

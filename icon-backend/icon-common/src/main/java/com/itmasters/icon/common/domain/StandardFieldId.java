package com.itmasters.icon.common.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * 표준 필드 식별자 (standard_fields.standard_field_id)
 * - DB의 standard_fields 테이블 field_name과 1:1로 매핑되어야 함
 * - 문자열 하드코딩을 줄이고, 오타/불일치 방지 목적
 *
 * 주의: 실제 운영 DB와 목록이 다를 수 있으므로,
 *  - 필요 시 부팅 시점에 DB와의 싱크를 검증하는 헬스를 추가하세요.
 */
public enum StandardFieldId {
    // 핵심 공통 키
    CUSTOMER_ID("customer_id"),
    USER_ID("user_id"),
    ACCOUNT_ID("account_id"),
    DEVICE_ID("device_id"),

    // 거래 기본
    TRANSACTION_DATETIME("transaction_datetime"),
    TRANSACTION_DATE("transaction_date"),
    TRANSACTION_TIME("transaction_time"),
    TRANSACTION_TYPE("transaction_type"),
    TRANSACTION_AMOUNT("transaction_amount"),
    CREATED_AT("created_at"),

    // 네트워크/세션
    IP_ADDRESS("ip_address"),
    CLIENT_IP("client_ip"),
    REMOTE_IP("remote_ip"),
    SESSION_ID("session_id"),
    LOGIN_DT("login_dt"),
    SESSION_START_TIME("session_start_time"),
    LOGIN_RESULT("login_result"),
    AUTH_STATUS("auth_status"),
    LOGIN_STATUS("login_status"),

    // 국가/위치
    TRANSACTION_COUNTRY("transaction_country"),
    COUNTRY_CODE("country_code"),
    LOCATION("location"),

    // 수취/송금 계좌
    SENDER_ACCOUNT("sender_account"),
    RECEIVER_ACCOUNT("receiver_account"),

    // 이벤트 구분
    EVENT_KIND("event_kind");

    private final String id;
    StandardFieldId(String id) { this.id = id; }
    public String id() { return id; }

    public static Optional<StandardFieldId> from(String fieldName) {
        if (fieldName == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(v -> v.id.equalsIgnoreCase(fieldName))
                .findFirst();
    }

    @Override
    public String toString() { return id; }
}


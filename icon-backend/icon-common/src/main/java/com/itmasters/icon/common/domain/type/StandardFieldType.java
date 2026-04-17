package com.itmasters.icon.common.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 표준 필드 타입 정의
 * Evaluator에서 사용하는 표준 필드명을 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum StandardFieldType {
    // 사용자 식별
    CUSTOMER_ID("customer_id", "고객 ID", FieldDataType.STRING),
    USER_ID("user_id", "사용자 ID", FieldDataType.STRING),

    // 시간 관련
    TRANSACTION_DATETIME("transaction_datetime", "거래 일시", FieldDataType.DATETIME),
    EVENT_DATETIME("event_datetime", "이벤트 일시", FieldDataType.DATETIME),
    TRANSACTION_TIME("transaction_time", "거래 시간", FieldDataType.TIME),

    // 위치 관련
    ACCESS_COUNTRY("access_country", "접속 국가", FieldDataType.STRING),
    COUNTRY_CODE("country_code", "국가 코드", FieldDataType.STRING),

    // 거래 관련
    TRANSACTION_AMOUNT("transaction_amount", "거래 금액", FieldDataType.DECIMAL),
    TRANSACTION_TYPE("transaction_type", "거래 유형", FieldDataType.STRING),

    // 이벤트 관련
    EVENT_TYPE("event_type", "이벤트 타입", FieldDataType.STRING);

    private final String fieldName;
    private final String description;
    private final FieldDataType dataType;

    /**
     * 필드명으로 StandardFieldType 찾기
     */
    public static StandardFieldType fromFieldName(String fieldName) {
        if (fieldName == null) {
            return null;
        }

        for (StandardFieldType type : values()) {
            if (type.fieldName.equalsIgnoreCase(fieldName)) {
                return type;
            }
        }
        return null;
    }
}

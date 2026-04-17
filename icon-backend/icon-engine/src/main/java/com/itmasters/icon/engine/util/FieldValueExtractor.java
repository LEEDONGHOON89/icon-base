package com.itmasters.icon.engine.util;

import com.itmasters.icon.common.domain.type.StandardFieldType;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

/**
 * Context에서 표준 필드 값을 추출하는 유틸리티
 * 하드코딩된 필드명 대신 표준 필드 타입을 사용하여 값을 추출합니다.
 */
@Slf4j
public class FieldValueExtractor {

    /**
     * Context에서 문자열 값 추출
     * 표준 필드명으로 시도하고, 없으면 대체 필드명들을 시도합니다.
     *
     * @param context           데이터 컨텍스트
     * @param standardFieldType 표준 필드 타입
     * @param alternativeFields 대체 필드명 목록 (우선순위 순)
     * @return 추출된 값 (없으면 null)
     */
    public static String extractString(Map<String, Object> context,
                                       StandardFieldType standardFieldType,
                                       String... alternativeFields) {
        // 1. 표준 필드명으로 시도
        Object value = context.get(standardFieldType.getFieldName());
        if (value != null) {
            log.trace("표준 필드 발견: {} = {}", standardFieldType.getFieldName(), value);
            return value.toString();
        }

        // 2. 대체 필드명들로 시도
        for (String altField : alternativeFields) {
            value = context.get(altField);
            if (value != null) {
                log.debug("대체 필드 발견: {} = {} (표준: {})",
                        altField, value, standardFieldType.getFieldName());
                return value.toString();
            }
        }

        log.debug("필드를 찾을 수 없음: {} (대체: {})",
                standardFieldType.getFieldName(), String.join(", ", alternativeFields));
        return null;
    }

    /**
     * 고객 ID 추출
     * 표준: customer_id
     * 대체: CUS_ID, user_id, USER_ID
     */
    public static String extractCustomerId(Map<String, Object> context) {
        return extractString(context, StandardFieldType.CUSTOMER_ID,
                "CUS_ID", "user_id", "USER_ID");
    }

    /**
     * 사용자 ID 추출 (customer_id와 동일한 로직)
     */
    public static String extractUserId(Map<String, Object> context) {
        return extractCustomerId(context);
    }

    /**
     * 거래 일시 추출
     * 표준: transaction_datetime
     * 대체: TRX_DT, event_datetime, EVENT_DT
     */
    public static LocalDateTime extractTransactionDatetime(Map<String, Object> context) {
        // 1. 표준 필드명으로 시도
        Object value = context.get(StandardFieldType.TRANSACTION_DATETIME.getFieldName());
        if (value == null) {
            value = context.get("TRX_DT");
        }
        if (value == null) {
            value = context.get("event_datetime");
        }
        if (value == null) {
            value = context.get("EVENT_DT");
        }

        if (value == null) {
            log.debug("거래 일시 필드를 찾을 수 없음");
            return LocalDateTime.now(); // 기본값: 현재 시간
        }

        // 타입 변환
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        } else if (value instanceof String) {
            try {
                return LocalDateTime.parse((String) value);
            } catch (Exception e) {
                log.warn("거래 일시 파싱 실패: {}", value);
                return LocalDateTime.now();
            }
        }

        return LocalDateTime.now();
    }

    /**
     * 거래 시간만 추출 (LocalTime)
     * 표준: transaction_time
     * 대체: TRX_TIME, transaction_datetime에서 시간 부분만 추출
     */
    public static LocalTime extractTransactionTime(Map<String, Object> context) {
        // 1. transaction_time 필드 시도
        Object timeValue = context.get(StandardFieldType.TRANSACTION_TIME.getFieldName());
        if (timeValue == null) {
            timeValue = context.get("TRX_TIME");
        }

        if (timeValue != null) {
            if (timeValue instanceof LocalTime) {
                return (LocalTime) timeValue;
            } else if (timeValue instanceof String) {
                try {
                    return LocalTime.parse((String) timeValue);
                } catch (Exception e) {
                    log.warn("거래 시간 파싱 실패: {}", timeValue);
                }
            }
        }

        // 2. transaction_datetime에서 시간 부분만 추출
        LocalDateTime dateTime = extractTransactionDatetime(context);
        return dateTime != null ? dateTime.toLocalTime() : LocalTime.now();
    }

    /**
     * 접속 국가 추출
     * 표준: access_country
     * 대체: ACCESS_COUNTRY, COUNTRY, country, COUNTRY_CODE, country_code
     */
    public static String extractAccessCountry(Map<String, Object> context) {
        return extractString(context, StandardFieldType.ACCESS_COUNTRY,
                "ACCESS_COUNTRY", "COUNTRY", "country", "COUNTRY_CODE", "country_code");
    }

    /**
     * 이벤트 타입 추출
     * 표준: event_type
     * 대체: EVENT_TYPE, eventType
     */
    public static String extractEventType(Map<String, Object> context) {
        return extractString(context, StandardFieldType.EVENT_TYPE,
                "EVENT_TYPE", "eventType");
    }

    /**
     * 거래 금액 추출
     * 표준: transaction_amount
     * 대체: TRX_AMT, AMOUNT, amount
     */
    public static Double extractTransactionAmount(Map<String, Object> context) {
        String amountStr = extractString(context, StandardFieldType.TRANSACTION_AMOUNT,
                "TRX_AMT", "AMOUNT", "amount");

        if (amountStr == null) {
            return null;
        }

        try {
            return Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            log.warn("거래 금액 파싱 실패: {}", amountStr);
            return null;
        }
    }
}

package com.itmasters.icon.engine.derivedfield.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.derivedfield.strategy.context.ComputationContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * DATETIME_DIFF_SECONDS Strategy
 *
 * 이벤트 데이터 내의 두 datetime 필드 간 차이를 초 단위로 계산합니다.
 *
 * 설정 예시:
 * {
 *   "field1": "approval_datetime",     // 끝 시간 (나중 시간)
 *   "field2": "approval_request_dt"    // 시작 시간 (이전 시간)
 * }
 *
 * 반환값:
 * - 정수: field1 - field2 (초 단위, 0 이상)
 * - null: 계산 불가 (데이터 부족 또는 파싱 실패)
 *
 * 예시:
 * - approval_datetime: "2025-12-20 14:00:05"
 * - approval_request_dt: "2025-12-20 14:00:00"
 * - 결과: 5 (초)
 */
@Slf4j
@Component("engineDatetimeDiffSecondsStrategy")
@RequiredArgsConstructor
public class DatetimeDiffSecondsStrategy implements FieldComputationStrategy {

    /**
     * computation_config JSON 키 정의
     */
    @Getter
    @RequiredArgsConstructor
    public enum ConfigKey {
        FIELD1("field1", null),  // 끝 시간 (필수)
        FIELD2("field2", null);  // 시작 시간 (필수)

        private final String key;
        private final String defaultValue;

        /**
         * JsonNode에서 값을 추출하거나 기본값 반환
         */
        public String getValueOrDefault(JsonNode config) {
            if (!config.has(key)) {
                return defaultValue;
            }
            JsonNode node = config.get(key);
            return node.isNull() ? defaultValue : node.asText();
        }
    }

    private final ObjectMapper objectMapper;

    @Override
    public Object compute(ComputationContext context) {
        try {
            String configJson = context.getComputationConfigJson();
            JsonNode config = objectMapper.readTree(configJson);

            // 설정 값 추출
            String field1Name = ConfigKey.FIELD1.getValueOrDefault(config);
            String field2Name = ConfigKey.FIELD2.getValueOrDefault(config);

            // 필수 필드 검증
            if (field1Name == null || field1Name.trim().isEmpty()) {
                log.warn("DATETIME_DIFF_SECONDS: field1 설정 누락");
                return null;
            }
            if (field2Name == null || field2Name.trim().isEmpty()) {
                log.warn("DATETIME_DIFF_SECONDS: field2 설정 누락");
                return null;
            }

            // 이벤트 데이터에서 필드 값 추출
            String field1Value = context.getStringValue(field1Name);
            String field2Value = context.getStringValue(field2Name);

            if (field1Value == null || field1Value.trim().isEmpty()) {
                log.debug("DATETIME_DIFF_SECONDS: field1 '{}' 값이 없음", field1Name);
                return null;
            }
            if (field2Value == null || field2Value.trim().isEmpty()) {
                log.debug("DATETIME_DIFF_SECONDS: field2 '{}' 값이 없음", field2Name);
                return null;
            }

            // DateTime 파싱
            LocalDateTime dateTime1 = parseDateTime(field1Value);
            LocalDateTime dateTime2 = parseDateTime(field2Value);

            if (dateTime1 == null) {
                log.warn("DATETIME_DIFF_SECONDS: field1 파싱 실패 '{}'", field1Value);
                return null;
            }
            if (dateTime2 == null) {
                log.warn("DATETIME_DIFF_SECONDS: field2 파싱 실패 '{}'", field2Value);
                return null;
            }

            // 초 단위 차이 계산 (field1 - field2)
            long secondsDiff = ChronoUnit.SECONDS.between(dateTime2, dateTime1);

            // 음수인 경우 0으로 처리 (field2가 field1보다 미래인 경우)
            if (secondsDiff < 0) {
                log.warn("DATETIME_DIFF_SECONDS: 음수 결과. field1={}, field2={}, diff={}",
                        field1Value, field2Value, secondsDiff);
                secondsDiff = 0;
            }

            log.debug("DATETIME_DIFF_SECONDS: field1={}, field2={}, seconds={}",
                    field1Value, field2Value, secondsDiff);

            return (int) secondsDiff;

        } catch (Exception e) {
            log.error("DATETIME_DIFF_SECONDS 계산 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * DateTime 문자열을 LocalDateTime으로 파싱
     *
     * 지원 형식:
     * - "2025-12-20 14:00:05"
     * - "2025-12-20T14:00:05"
     * - "2025-12-20 14:00:05.123"
     * - "2025-12-20T14:00:05.123"
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }

        try {
            // ISO DateTime 형식: "2025-12-20T14:00:05" 또는 "2025-12-20T14:00:05.123"
            if (dateTimeStr.contains("T")) {
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }

            // 공백 구분 형식: "2025-12-20 14:00:05" 또는 "2025-12-20 14:00:05.123"
            if (dateTimeStr.contains(" ")) {
                // 밀리초 포함 여부 확인
                if (dateTimeStr.contains(".")) {
                    return LocalDateTime.parse(dateTimeStr,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                } else {
                    return LocalDateTime.parse(dateTimeStr,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
            }

            // 기본 파싱 시도
            return LocalDateTime.parse(dateTimeStr);

        } catch (DateTimeParseException e) {
            log.warn("DateTime 파싱 실패: {}", dateTimeStr, e);
            return null;
        }
    }

    @Override
    public String getSupportedComputationType() {
        return "DATETIME_DIFF_SECONDS";
    }
}

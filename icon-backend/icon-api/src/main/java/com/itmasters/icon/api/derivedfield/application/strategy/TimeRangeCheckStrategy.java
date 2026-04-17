package com.itmasters.icon.api.derivedfield.application.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.api.derivedfield.application.strategy.context.ComputationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * TIME_RANGE_CHECK Strategy
 *
 * 시간 필드 값을 기준으로 특정 시간 범위에 포함되는지 확인하여 boolean 결과 반환
 *
 * @see computation_type_configs 테이블에서 config_schema 및 config_example 참조
 *
 * 설정 파라미터:
 * - source_field (필수): 시간 필드명 (HH:mm, HH:mm:ss, yyyy-MM-dd HH:mm:ss 형식 지원)
 * - start_time (필수): 시작 시간 (이상, >=)
 * - end_time (필수): 종료 시간 (미만, <). 자정 넘김 가능
 *
 * 예시:
 * - start_time: "19:00", end_time: "06:00" → 19:00~05:59 (야간)
 * - start_time: "09:00", end_time: "18:00" → 09:00~17:59 (업무 시간)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimeRangeCheckStrategy implements FieldComputationStrategy {

    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public Object compute(ComputationContext context) {
        try {
            String configJson = context.getComputationConfigJson();
            JsonNode config = objectMapper.readTree(configJson);

            // 설정 값 추출
            String sourceField = config.get("source_field").asText();
            String startTimeStr = config.get("start_time").asText();
            String endTimeStr = config.get("end_time").asText();

            // 시간 필드 값 추출
            String timeValue = context.getStringValue(sourceField);
            if (timeValue == null || timeValue.trim().isEmpty()) {
                log.warn("Time field '{}' is null or empty", sourceField);
                return false;
            }

            // 시간 파싱
            LocalTime eventTime = parseTime(timeValue);
            LocalTime startTime = LocalTime.parse(startTimeStr, TIME_FORMATTER);
            LocalTime endTime = LocalTime.parse(endTimeStr, TIME_FORMATTER);

            // 시간 범위 체크
            boolean inRange = isInTimeRange(eventTime, startTime, endTime);

            log.debug("TimeRangeCheck - eventTime: {}, range: {}-{}, result: {}",
                     eventTime, startTime, endTime, inRange);

            return inRange;

        } catch (Exception e) {
            log.error("Failed to compute TIME_RANGE_CHECK: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 시간 문자열 파싱 (여러 형식 지원)
     * - "HH:mm" → LocalTime
     * - "HH:mm:ss" → LocalTime
     * - "yyyy-MM-dd HH:mm:ss" → LocalTime (시간 부분만 추출)
     */
    private LocalTime parseTime(String timeValue) {
        // HH:mm 형식 우선 시도
        try {
            return LocalTime.parse(timeValue, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            // ISO 형식 시도 (HH:mm:ss)
            try {
                return LocalTime.parse(timeValue);
            } catch (DateTimeParseException e2) {
                // "yyyy-MM-dd HH:mm:ss" 형식에서 시간 부분만 추출
                try {
                    if (timeValue.contains(" ")) {
                        String timePart = timeValue.split(" ")[1]; // "HH:mm:ss" 부분만 추출
                        return LocalTime.parse(timePart);
                    }
                    throw new IllegalArgumentException("Invalid time format: " + timeValue +
                                                     ". Expected HH:mm, HH:mm:ss, or yyyy-MM-dd HH:mm:ss");
                } catch (Exception e3) {
                    throw new IllegalArgumentException("Invalid time format: " + timeValue +
                                                     ". Expected HH:mm, HH:mm:ss, or yyyy-MM-dd HH:mm:ss");
                }
            }
        }
    }

    /**
     * 시간 범위 체크
     * @param eventTime 체크할 시간
     * @param startTime 시작 시간 (이상, >=)
     * @param endTime 종료 시간 (미만, <)
     * @return 범위 내 포함 여부
     */
    private boolean isInTimeRange(LocalTime eventTime, LocalTime startTime, LocalTime endTime) {
        if (startTime.isBefore(endTime)) {
            // 일반적인 경우 (예: 09:00 - 18:00)
            // eventTime >= startTime && eventTime < endTime
            return !eventTime.isBefore(startTime) && eventTime.isBefore(endTime);
        } else {
            // 자정을 넘는 경우 (예: 19:00 - 06:00)
            // eventTime >= startTime || eventTime < endTime
            return !eventTime.isBefore(startTime) || eventTime.isBefore(endTime);
        }
    }

    @Override
    public String getSupportedComputationType() {
        return "TIME_RANGE_CHECK";
    }
}

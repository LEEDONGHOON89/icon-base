package com.itmasters.icon.engine.evaluator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.common.domain.RuleType;
import com.itmasters.icon.engine.evaluator.RuleEvaluator;
import com.itmasters.icon.engine.util.FieldValueExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

/**
 * 시간대 기반 룰 평가기
 * 특정 시간대 활동 감지
 * 예: 심야 시간대(22:00-02:00) 활동
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimeBasedEvaluator implements RuleEvaluator {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public boolean evaluate(Map<String, Object> context, String ruleConfig) {
        try {
            JsonNode config = objectMapper.readTree(ruleConfig);
            
            // 설정값 추출
            String startTime = config.get("startTime").asText();  // "22:00"
            String endTime = config.get("endTime").asText();      // "02:00"
            
            // context에서 이벤트 시간 추출 (FieldValueExtractor 사용)
            LocalTime eventTime = FieldValueExtractor.extractTransactionTime(context);
            
            // 시간대 체크
            boolean result = isInTimeRange(eventTime, startTime, endTime);
            
            log.debug("Time-based check - eventTime: {}, range: {}-{}, result: {}", 
                     eventTime, startTime, endTime, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error evaluating time-based rule: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private boolean isInTimeRange(LocalTime eventTime, String startStr, String endStr) {
        LocalTime start = LocalTime.parse(startStr);
        LocalTime end = LocalTime.parse(endStr);
        
        if (start.isBefore(end)) {
            // 일반적인 경우 (예: 09:00 - 18:00)
            return !eventTime.isBefore(start) && !eventTime.isAfter(end);
        } else {
            // 자정을 넘는 경우 (예: 22:00 - 02:00)
            return !eventTime.isBefore(start) || !eventTime.isAfter(end);
        }
    }

    @Override
    public String getSupportedType() {
        return RuleType.TIME_BASED.name();
    }
}
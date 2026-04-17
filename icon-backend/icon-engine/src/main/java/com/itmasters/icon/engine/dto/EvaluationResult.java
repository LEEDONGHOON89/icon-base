package com.itmasters.icon.engine.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 룰 평가 결과 DTO
 */
@Getter
@Builder
@ToString
public class EvaluationResult {
    
    private final String ruleId;
    private final String ruleName;
    private final boolean matched;
    private final String message;
    private final Long rowNumber;
    private final LocalDateTime evaluatedAt;
    
    // 추가 정보
    private final String fieldValue;
    private final String expectedValue;
    private final String operator;
    
    // 매치된 데이터
    private final Map<String, Object> matchedData;
    
    // 원본 데이터 (매핑 전 데이터)
    private final Map<String, Object> originalData;
    
    public static EvaluationResult matched(String ruleId, String ruleName, Long rowNumber, String message) {
        return EvaluationResult.builder()
                .ruleId(ruleId)
                .ruleName(ruleName)
                .matched(true)
                .rowNumber(rowNumber)
                .message(message)
                .evaluatedAt(LocalDateTime.now())
                .build();
    }
    
    public static EvaluationResult notMatched(String ruleId, String ruleName, Long rowNumber) {
        return EvaluationResult.builder()
                .ruleId(ruleId)
                .ruleName(ruleName)
                .matched(false)
                .rowNumber(rowNumber)
                .evaluatedAt(LocalDateTime.now())
                .build();
    }
}
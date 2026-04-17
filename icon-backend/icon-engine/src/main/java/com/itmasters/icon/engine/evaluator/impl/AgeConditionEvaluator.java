package com.itmasters.icon.engine.evaluator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.common.domain.RuleType;
import com.itmasters.icon.engine.evaluator.RuleEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 나이 조건 룰 평가기
 * 사용자 나이만 체크 (금액 조건 없음)
 * 예: 65세 이상
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgeConditionEvaluator implements RuleEvaluator {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public boolean evaluate(Map<String, Object> context, String ruleConfig) {
        try {
            JsonNode config = objectMapper.readTree(ruleConfig);
            
            // 설정값 추출
            String operator = config.get("operator").asText();  // >, >=, <, <=, =
            int ageThreshold = config.get("age").asInt();
            
            // context에서 나이 추출
            Integer userAge = extractAge(context);
            
            if (userAge == null) {
                log.debug("User age not found in context");
                return false;
            }
            
            // 연산자별 평가
            boolean result = evaluateAge(userAge, operator, ageThreshold);
            
            log.debug("Age condition check - age: {}, operator: {}, threshold: {}, result: {}", 
                     userAge, operator, ageThreshold, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error evaluating age condition rule: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private Integer extractAge(Map<String, Object> context) {
        // 다양한 필드명 시도
        Object age = context.get("CUSTOMER_AGE");
        if (age == null) {
            age = context.get("customer_age");
        }
        if (age == null) {
            age = context.get("AGE");
        }
        if (age == null) {
            age = context.get("age");
        }
        
        if (age instanceof Number) {
            return ((Number) age).intValue();
        } else if (age instanceof String) {
            try {
                return Integer.parseInt((String) age);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse age: {}", age);
            }
        }
        
        return null;
    }
    
    private boolean evaluateAge(int userAge, String operator, int threshold) {
        switch (operator) {
            case ">":
                return userAge > threshold;
            case ">=":
                return userAge >= threshold;
            case "<":
                return userAge < threshold;
            case "<=":
                return userAge <= threshold;
            case "=":
            case "==":
                return userAge == threshold;
            case "!=":
                return userAge != threshold;
            default:
                log.warn("Unknown operator: {}", operator);
                return false;
        }
    }
    
    @Override
    public String getSupportedType() {
        return RuleType.AGE_CONDITION.name();
    }
}
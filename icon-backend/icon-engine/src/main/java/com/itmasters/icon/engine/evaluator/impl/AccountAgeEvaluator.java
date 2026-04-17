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
 * 계좌 나이 체크 룰 평가기
 * 계좌 개설 후 경과 시간만 체크 (활동 조건 없음)
 * 예: 계좌 개설 후 7일 이내
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountAgeEvaluator implements RuleEvaluator {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public boolean evaluate(Map<String, Object> context, String ruleConfig) {
        try {
            JsonNode config = objectMapper.readTree(ruleConfig);
            
            // 설정값 추출
            String operator = config.get("operator").asText();  // <, <=, >, >=
            int days = config.get("days").asInt();  // 일수
            
            // context에서 계좌 나이 추출
            Integer accountAge = extractAccountAge(context);
            
            if (accountAge == null) {
                log.debug("Account age not found in context");
                return false;
            }
            
            // 연산자별 평가
            boolean result = evaluateAge(accountAge, operator, days);
            
            log.debug("Account age check - age: {} days, operator: {}, threshold: {} days, result: {}", 
                     accountAge, operator, days, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error evaluating account age rule: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private Integer extractAccountAge(Map<String, Object> context) {
        // NEW_ACCOUNT_DAYS 필드 확인
        Object age = context.get("NEW_ACCOUNT_DAYS");
        if (age == null) {
            age = context.get("new_account_days");
        }
        if (age == null) {
            age = context.get("ACCOUNT_AGE_DAYS");
        }
        if (age == null) {
            age = context.get("account_age_days");
        }
        
        if (age instanceof Number) {
            return ((Number) age).intValue();
        } else if (age instanceof String) {
            try {
                return Integer.parseInt((String) age);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse account age: {}", age);
            }
        }
        
        return null;
    }
    
    private boolean evaluateAge(int accountAge, String operator, int threshold) {
        switch (operator) {
            case "<":
                return accountAge < threshold;
            case "<=":
                return accountAge <= threshold;
            case ">":
                return accountAge > threshold;
            case ">=":
                return accountAge >= threshold;
            case "=":
            case "==":
                return accountAge == threshold;
            case "!=":
                return accountAge != threshold;
            default:
                log.warn("Unknown operator: {}", operator);
                return false;
        }
    }
    
    @Override
    public String getSupportedType() {
        return RuleType.ACCOUNT_AGE.name();
    }
}
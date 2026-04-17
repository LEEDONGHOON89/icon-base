package com.itmasters.icon.engine.evaluator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.common.domain.RuleType;
import com.itmasters.icon.engine.evaluator.RuleEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 잔액 비율 룰 평가기
 * 계좌 잔액 대비 거래 금액 비율 체크
 * 예: 잔액 대비 30% 이상 금액 이체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceRatioEvaluator implements RuleEvaluator {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public boolean evaluate(Map<String, Object> context, String ruleConfig) {
        try {
            JsonNode config = objectMapper.readTree(ruleConfig);
            
            // 설정값 추출
            String operator = config.get("operator").asText();  // >, >=, <, <=
            double ratioThreshold = config.get("ratio").asDouble();  // 비율 (0.3 = 30%)
            
            // context에서 거래 금액과 잔액 추출
            BigDecimal transactionAmount = extractAmount(context);
            BigDecimal balance = extractBalance(context);
            
            if (transactionAmount == null || balance == null) {
                log.debug("Transaction amount or balance not found in context");
                return false;
            }
            
            // 잔액이 0인 경우 처리
            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("Balance is zero or negative: {}", balance);
                return false;
            }
            
            // 비율 계산
            BigDecimal ratio = transactionAmount.divide(balance, 4, RoundingMode.HALF_UP);
            double ratioValue = ratio.doubleValue();
            
            // 연산자별 평가
            boolean result = evaluateRatio(ratioValue, operator, ratioThreshold);
            
            log.debug("Balance ratio check - amount: {}, balance: {}, ratio: {:.2%}, " +
                     "operator: {}, threshold: {:.2%}, result: {}", 
                     transactionAmount, balance, ratioValue, operator, ratioThreshold, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error evaluating balance ratio rule: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private BigDecimal extractAmount(Map<String, Object> context) {
        // 다양한 필드명 시도 - 계산된 금액 우선 (음수 포함)
        Object amount = context.get("transaction_calc_amount");  // 계산된 금액 (음수 포함, 우선)
        if (amount == null) {
            amount = context.get("TRX_CAL_AMT");  // 계산된 금액
        }
        if (amount == null) {
            amount = context.get("transaction_amount");  // 절대값 금액
        }
        if (amount == null) {
            amount = context.get("TRX_AMT");
        }
        if (amount == null) {
            amount = context.get("AMOUNT");
        }

        return toBigDecimal(amount);
    }
    
    private BigDecimal extractBalance(Map<String, Object> context) {
        // 잔액 필드 - 거래 전 잔액 우선
        Object balance = context.get("before_balance");  // 거래 전 잔액 (우선)
        if (balance == null) {
            balance = context.get("after_balance");  // 거래 후 잔액
        }
        if (balance == null) {
            balance = context.get("BAL_AMT");
        }
        if (balance == null) {
            balance = context.get("BALANCE");
        }
        if (balance == null) {
            balance = context.get("balance");
        }

        return toBigDecimal(balance);
    }
    
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof Number) {
            return new BigDecimal(value.toString());
        } else if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse number: {}", value);
                return null;
            }
        }
        
        return null;
    }
    
    private boolean evaluateRatio(double ratio, String operator, double threshold) {
        switch (operator) {
            case ">":
                return ratio > threshold;
            case ">=":
                return ratio >= threshold;
            case "<":
                return ratio < threshold;
            case "<=":
                return ratio <= threshold;
            case "=":
            case "==":
                return Math.abs(ratio - threshold) < 0.0001;  // 부동소수점 비교
            case "!=":
                return Math.abs(ratio - threshold) >= 0.0001;
            default:
                log.warn("Unknown operator: {}", operator);
                return false;
        }
    }
    
    @Override
    public String getSupportedType() {
        return RuleType.BALANCE_RATIO.name();
    }
}
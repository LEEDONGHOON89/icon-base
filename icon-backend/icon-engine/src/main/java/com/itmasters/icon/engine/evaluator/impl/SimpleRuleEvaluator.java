package com.itmasters.icon.engine.evaluator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.common.domain.RuleType;
import com.itmasters.icon.engine.evaluator.RuleEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 단순 비교 룰 평가기
 * 기존 룰 평가 로직을 그대로 사용 (현재 데이터만으로 평가)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleRuleEvaluator implements RuleEvaluator {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public boolean evaluate(Map<String, Object> context, String ruleConfig) {
        try {
            JsonNode config = objectMapper.readTree(ruleConfig);
            
            // field 또는 fieldName 둘 다 지원
            String field = null;
            if (config.has("fieldName")) {
                field = config.get("fieldName").asText();
            } else if (config.has("field")) {
                field = config.get("field").asText();
            }
            
            if (field == null) {
                log.error("No 'field' or 'fieldName' found in rule config: {}", ruleConfig);
                return false;
            }
            
            String operator = config.get("operator").asText();
            JsonNode valueNode = config.get("value");
            
            log.debug("Evaluating rule - field: {}, operator: {}, value: {}", field, operator, valueNode);
            
            // context에서 필드 값 가져오기
            Object fieldValue = context.get(field);
            
            if (fieldValue == null) {
                log.debug("Field {} not found in context. Available fields: {}", field, context.keySet());
                return false;
            }
            
            log.debug("Field value found: {} = {}", field, fieldValue);
            
            // 연산자별 평가
            return evaluateCondition(fieldValue, operator, valueNode);
            
        } catch (Exception e) {
            log.error("Error evaluating simple rule: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private boolean evaluateCondition(Object fieldValue, String operator, JsonNode valueNode) {
        switch (operator) {
            case "=":
            case "==":
            case "EQUALS":
                return compareEquals(fieldValue, valueNode);
            case "!=":
                return !compareEquals(fieldValue, valueNode);
            case ">":
                return compareNumeric(fieldValue, valueNode) > 0;
            case ">=":
            case "GREATER_THAN_OR_EQUALS":
                return compareNumeric(fieldValue, valueNode) >= 0;
            case "<=":
            case "LESS_THAN_OR_EQUALS":
                return compareNumeric(fieldValue, valueNode) <= 0;
            case "IS_TRUE":
                return isTrueValue(fieldValue, valueNode);
            case "IS_FALSE":
                return !isTrueValue(fieldValue, valueNode);
            case "contains":
                return fieldValue.toString().contains(valueNode.asText());
            case "startsWith":
                return fieldValue.toString().startsWith(valueNode.asText());
            case "endsWith":
                return fieldValue.toString().endsWith(valueNode.asText());
            case "in":
                return checkInList(fieldValue, valueNode);
            case "notIn":
                return !checkInList(fieldValue, valueNode);
            case "between":
                return checkBetween(fieldValue, valueNode);
            case "isNull":
                return fieldValue == null;
            case "isNotNull":
                return fieldValue != null;
            default:
                log.warn("Unknown operator: {}", operator);
                return false;
        }
    }
    
    private boolean compareEquals(Object fieldValue, JsonNode valueNode) {
        if (fieldValue instanceof Number && valueNode.isNumber()) {
            return toBigDecimal(fieldValue).compareTo(toBigDecimal(valueNode.numberValue())) == 0;
        }
        return fieldValue.toString().equals(valueNode.asText());
    }
    
    private int compareNumeric(Object fieldValue, JsonNode valueNode) {
        try {
            BigDecimal fieldNum = toBigDecimal(fieldValue);
            BigDecimal valueNum = toBigDecimal(valueNode.asText()); // numberValue() 대신 asText() 사용
            
            log.debug("Comparing numbers: {} ({}) vs {} ({})", 
                fieldNum, fieldValue.getClass().getSimpleName(),
                valueNum, valueNode.asText());
            
            int result = fieldNum.compareTo(valueNum);
            log.debug("Comparison result: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Error in numeric comparison: fieldValue={}, valueNode={}", fieldValue, valueNode, e);
            return -1; // 비교 실패 시 false가 되도록
        }
    }
    
    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof Number) {
            return new BigDecimal(value.toString());
        } else {
            return new BigDecimal(value.toString());
        }
    }
    
    private boolean checkInList(Object fieldValue, JsonNode valueNode) {
        if (valueNode.isArray()) {
            for (JsonNode item : valueNode) {
                if (compareEquals(fieldValue, item)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean checkBetween(Object fieldValue, JsonNode valueNode) {
        if (valueNode.isObject() && valueNode.has("min") && valueNode.has("max")) {
            BigDecimal fieldNum = toBigDecimal(fieldValue);
            BigDecimal min = toBigDecimal(valueNode.get("min").numberValue());
            BigDecimal max = toBigDecimal(valueNode.get("max").numberValue());
            return fieldNum.compareTo(min) >= 0 && fieldNum.compareTo(max) <= 0;
        }
        return false;
    }
    
    private boolean isTrueValue(Object fieldValue, JsonNode valueNode) {
        // 여러 형태의 true 값 지원
        if (fieldValue instanceof Boolean) {
            return (Boolean) fieldValue;
        }
        
        String strValue = fieldValue.toString().toLowerCase();
        return "true".equals(strValue) || "1".equals(strValue) || "yes".equals(strValue) || "y".equals(strValue);
    }
    
    @Override
    public String getSupportedType() {
        return RuleType.SIMPLE.name();
    }
}
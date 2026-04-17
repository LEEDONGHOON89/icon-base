package com.itmasters.icon.common.domain.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itmasters.icon.common.domain.rule.condition.RuleCondition;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ParseCondition {
    private final ObjectMapper objectMapper;
    private final ConditionSerializationVisitor visitor;

    public ParseCondition(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.visitor = new ConditionSerializationVisitor(objectMapper);
    }


    /**
     * String 을 RuleCondition로 변환
     * RuleField를 활용하여 JSON을 RuleCondition으로 변환
     * fieldName을 보고 적절한 RuleCondition 구현체를 자동 선택
     */
    public RuleCondition parseConditionWithRuleField(String jsonData) throws JsonProcessingException {
        JsonNode node = objectMapper.readTree(jsonData);

        // 필수 필드 추출 - fieldName 또는 field 지원 (backward compatibility)
        String fieldName = extractFieldName(node);
        String operatorStr = extractOperator(node);
        RuleOperator operator = findOperatorByCode(operatorStr);

        // 값 추출 (타입에 따라 다르게 처리)
        JsonNode valueNode = node.get("value");

        if (valueNode == null) {
            throw new IllegalArgumentException("No value field found in condition data");
        }

        // 값의 실제 타입에 따라 처리
        Object value;
        if (valueNode.isArray()) {
            value = objectMapper.convertValue(valueNode, Object[].class);
        } else if (valueNode.isNumber()) {
            value = valueNode.numberValue();
        } else {
            value = valueNode.asText();
        }

        // RuleField를 통해 필드 타입 확인하고 적절한 조건 생성
        return RuleConditionFactory.createCondition(fieldName, operator, value);
    }

    /**
     * JSON에서 필드명 추출 - fieldName 또는 field 지원
     */
    private String extractFieldName(JsonNode node) {
        JsonNode fieldNameNode = node.get("fieldName");
        if (fieldNameNode != null && !fieldNameNode.isNull()) {
            return fieldNameNode.asText();
        }
        
        JsonNode fieldNode = node.get("field");
        if (fieldNode != null && !fieldNode.isNull()) {
            return fieldNode.asText();
        }
        
        throw new IllegalArgumentException("No fieldName or field found in condition data");
    }

    /**
     * JSON에서 연산자 추출
     */
    private String extractOperator(JsonNode node) {
        JsonNode operatorNode = node.get("operator");
        if (operatorNode == null || operatorNode.isNull()) {
            throw new IllegalArgumentException("No operator field found in condition data");
        }
        return operatorNode.asText();
    }


    /**
     * 연산자 코드로 RuleOperator 찾기 (대소문자 구분 없음)
     * 우선 symbol로 검색 후, 없으면 name으로 검색
     */
    private RuleOperator findOperatorByCode(String code) {
        // 먼저 symbol로 검색 (예: "=" -> EQUALS)
        for (RuleOperator operator : RuleOperator.values()) {
            if (operator.getSymbol().equalsIgnoreCase(code)) {
                return operator;
            }
        }
        
        // symbol로 찾지 못하면 enum name으로 검색 (예: "EQUALS" -> EQUALS)
        for (RuleOperator operator : RuleOperator.values()) {
            if (operator.name().equalsIgnoreCase(code)) {
                return operator;
            }
        }
        
        throw new IllegalArgumentException("Unknown operator: " + code);
    }


    /**
     * RuleCondition을 JSON으로 직렬화
     * 통일된 JSON 구조 사용: {fieldName, operator, value}
     */
    public String serializeConditionWithRuleField(RuleCondition condition) throws JsonProcessingException {
        // Visitor 패턴을 사용하여 조건을 직렬화
        ObjectNode node = condition.accept(visitor);
        return objectMapper.writeValueAsString(node);
    }
}

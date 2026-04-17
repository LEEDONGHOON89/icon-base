package com.itmasters.icon.api.derivedfield.application.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.api.derivedfield.application.strategy.context.ComputationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * FIELD_COMPARISON Strategy
 *
 * 단일 필드를 고정값 목록과 비교하여 boolean 결과 반환
 *
 * @see computation_type_configs 테이블에서 config_schema 및 config_example 참조
 *
 * 설정 파라미터:
 * - source_field (필수): 비교할 필드명
 * - logic (필수): 비교 로직 (EQUALS, NOT_EQUALS, STARTS_WITH, NOT_STARTS_WITH, IN, NOT_IN, CONTAINS, MATCHES)
 * - values (필수): 비교할 값 목록
 * - and (선택): 추가 AND 조건 (source_field, logic, values 동일 구조)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FieldComparisonStrategy implements FieldComputationStrategy {

    private final ObjectMapper objectMapper;

    @Override
    public Object compute(ComputationContext context) {
        try {
            String configJson = context.getComputationConfigJson();
            JsonNode config = objectMapper.readTree(configJson);

            // 1. 메인 조건 평가
            boolean mainResult = evaluateCondition(context, config);

            // 2. AND 조건이 있으면 추가 평가
            if (config.has("and")) {
                JsonNode andCondition = config.get("and");
                boolean andResult = evaluateCondition(context, andCondition);
                return mainResult && andResult;
            }

            return mainResult;

        } catch (Exception e) {
            log.error("Failed to compute FIELD_COMPARISON: {}", e.getMessage(), e);
            throw new IllegalStateException("FIELD_COMPARISON computation failed", e);
        }
    }

    /**
     * 단일 조건 평가
     */
    private boolean evaluateCondition(ComputationContext context, JsonNode condition) {
        String sourceField = condition.get("source_field").asText();
        String logic = condition.get("logic").asText();
        JsonNode valuesNode = condition.get("values");

        String fieldValue = context.getStringValue(sourceField);

        return switch (logic) {
            case "EQUALS" -> equalsAny(fieldValue, valuesNode);
            case "NOT_EQUALS" -> !equalsAny(fieldValue, valuesNode);
            case "STARTS_WITH" -> startsWithAny(fieldValue, valuesNode);
            case "NOT_STARTS_WITH" -> !startsWithAny(fieldValue, valuesNode);
            case "IN" -> inAny(fieldValue, valuesNode);
            case "NOT_IN" -> !inAny(fieldValue, valuesNode);
            case "CONTAINS" -> containsAny(fieldValue, valuesNode);
            case "MATCHES" -> matchesAny(fieldValue, valuesNode);
            default -> throw new IllegalArgumentException("Unsupported logic: " + logic);
        };
    }

    private boolean equalsAny(String value, JsonNode values) {
        for (JsonNode node : values) {
            if (value.equals(node.asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean startsWithAny(String value, JsonNode values) {
        for (JsonNode node : values) {
            if (value.startsWith(node.asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean inAny(String value, JsonNode values) {
        return equalsAny(value, values);
    }

    private boolean containsAny(String value, JsonNode values) {
        for (JsonNode node : values) {
            if (value.contains(node.asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAny(String value, JsonNode values) {
        for (JsonNode node : values) {
            if (value.matches(node.asText())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getSupportedComputationType() {
        return "FIELD_COMPARISON";
    }
}

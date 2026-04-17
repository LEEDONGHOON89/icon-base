package com.itmasters.icon.engine.derivedfield.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.derivedfield.strategy.context.ComputationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * FIELD_EQUALS_FIELD Strategy
 *
 * 두 필드의 값을 비교하여 결과를 반환합니다.
 * 주로 자기승인 여부(requester_id == approver_id) 판단에 사용됩니다.
 *
 * @see computation_type_configs 테이블에서 config_schema 및 config_example 참조
 *
 * 설정 파라미터:
 * - field1 (필수): 첫 번째 필드명
 * - field2 (필수): 두 번째 필드명
 * - true_value (선택): 같을 때 반환값 (기본: true)
 * - false_value (선택): 다를 때 반환값 (기본: false)
 * - ignore_case (선택): 대소문자 무시 여부 (기본: false)
 *
 * 결과:
 * - field1 == field2 → true_value
 * - field1 != field2 → false_value
 * - field1 또는 field2가 null/empty → false_value
 */
@Slf4j
@Component("engineFieldEqualsFieldStrategy")
@RequiredArgsConstructor
public class FieldEqualsFieldStrategy implements FieldComputationStrategy {

    private final ObjectMapper objectMapper;

    @Override
    public Object compute(ComputationContext context) {
        try {
            String configJson = context.getComputationConfigJson();
            JsonNode config = objectMapper.readTree(configJson);

            // 필수 설정값 추출
            String field1Name = config.get("field1").asText();
            String field2Name = config.get("field2").asText();

            // 선택적 설정값 추출
            Object trueValue = config.has("true_value")
                ? parseValue(config.get("true_value"))
                : true;
            Object falseValue = config.has("false_value")
                ? parseValue(config.get("false_value"))
                : false;
            boolean ignoreCase = config.has("ignore_case")
                && config.get("ignore_case").asBoolean();

            // 필드 값 추출
            String value1 = context.getStringValue(field1Name);
            String value2 = context.getStringValue(field2Name);

            // null 또는 빈 문자열 체크
            if (value1 == null || value1.isEmpty() || value2 == null || value2.isEmpty()) {
                log.debug("FIELD_EQUALS_FIELD - field1: '{}', field2: '{}' (one or both empty) -> {}",
                    value1, value2, falseValue);
                return falseValue;
            }

            // 비교
            boolean isEqual = ignoreCase
                ? value1.equalsIgnoreCase(value2)
                : value1.equals(value2);

            Object result = isEqual ? trueValue : falseValue;

            log.debug("FIELD_EQUALS_FIELD - {}='{}', {}='{}', ignoreCase={}, result={}",
                field1Name, value1, field2Name, value2, ignoreCase, result);

            return result;

        } catch (Exception e) {
            log.error("Failed to compute FIELD_EQUALS_FIELD: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * JsonNode 값을 적절한 타입으로 변환
     */
    private Object parseValue(JsonNode node) {
        if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isNumber()) {
            return node.asInt();
        } else {
            return node.asText();
        }
    }

    @Override
    public String getSupportedComputationType() {
        return "FIELD_EQUALS_FIELD";
    }
}

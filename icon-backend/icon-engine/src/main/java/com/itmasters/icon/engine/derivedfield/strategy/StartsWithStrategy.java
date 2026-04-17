package com.itmasters.icon.engine.derivedfield.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.derivedfield.strategy.context.ComputationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * STARTS_WITH Strategy
 *
 * 필드 값이 특정 접두사로 시작하는지 확인합니다.
 * 주로 ID 패턴 매칭에 사용됩니다. (예: EMP로 시작하면 직원, CORP로 시작하면 법인)
 *
 * 설정 파라미터:
 * - field (필수): 검사할 필드명
 * - prefix (필수): 시작 문자열 (예: "EMP", "CORP")
 * - true_value (선택): 매칭 시 반환값 (기본: true)
 * - false_value (선택): 미매칭 시 반환값 (기본: false)
 * - ignore_case (선택): 대소문자 무시 여부 (기본: false)
 *
 * 사용 예:
 * - beneficiary_owner_id가 "EMP"로 시작하면 직원계좌
 * - customer_id가 "CORP"로 시작하면 법인계좌
 */
@Slf4j
@Component("engineStartsWithStrategy")
@RequiredArgsConstructor
public class StartsWithStrategy implements FieldComputationStrategy {

    private final ObjectMapper objectMapper;

    @Override
    public Object compute(ComputationContext context) {
        try {
            String configJson = context.getComputationConfigJson();
            JsonNode config = objectMapper.readTree(configJson);

            // 필수 설정값 추출
            String fieldName = config.get("field").asText();
            String prefix = config.get("prefix").asText();

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
            String fieldValue = context.getStringValue(fieldName);

            // null 또는 빈 문자열 체크
            if (fieldValue == null || fieldValue.isEmpty()) {
                log.debug("STARTS_WITH - field '{}' is null or empty -> {}", fieldName, falseValue);
                return falseValue;
            }

            // 접두사 체크
            boolean startsWith = ignoreCase
                ? fieldValue.toUpperCase().startsWith(prefix.toUpperCase())
                : fieldValue.startsWith(prefix);

            Object result = startsWith ? trueValue : falseValue;

            log.debug("STARTS_WITH - {}='{}', prefix='{}', ignoreCase={}, result={}",
                fieldName, fieldValue, prefix, ignoreCase, result);

            return result;

        } catch (Exception e) {
            log.error("Failed to compute STARTS_WITH: {}", e.getMessage(), e);
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
        return "STARTS_WITH";
    }
}

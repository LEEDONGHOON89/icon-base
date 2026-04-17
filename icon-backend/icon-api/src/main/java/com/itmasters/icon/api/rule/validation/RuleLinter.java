package com.itmasters.icon.api.rule.validation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class RuleLinter {
    private final ObjectMapper objectMapper;

    public void validateWhereJson(String whereJson) {
        if (whereJson != null && !whereJson.isBlank()) {
            validateWhere(whereJson);
        }
    }

    private void validateWhere(String whereJson) {
        if (whereJson.trim().startsWith("[")) {
            List<Map<String, Object>> list = parseList(whereJson, "where_json");
            for (Map<String, Object> p : list) validatePredicate(p);
        } else if (whereJson.trim().startsWith("{")) {
            Map<String, Object> p = parseObject(whereJson, "where_json");
            validatePredicate(p);
        } else {
            throw new IllegalArgumentException("조건(where_json)은 JSON 객체 또는 배열 형식이어야 합니다. 예: {\"fieldName\":\"...\",\"operator\":\"...\",\"value\":...}");
        }
    }

    private void validatePredicate(Map<String, Object> p) {
        String field = str(p.get("fieldName"));
        String op = str(p.get("operator"));

        if (field.isEmpty() && op.isEmpty()) {
            throw new IllegalArgumentException("룰 조건에 필드명(fieldName)과 연산자(operator)가 모두 필요합니다.");
        } else if (field.isEmpty()) {
            throw new IllegalArgumentException("룰 조건에 필드명(fieldName)이 필요합니다.");
        } else if (op.isEmpty()) {
            throw new IllegalArgumentException("룰 조건에 연산자(operator)가 필요합니다.");
        }

        // Aggregates 전용 연산자는 룰에서 금지 (시퀀스는 집계에서만 지원)
        if ("SEQUENCE_WITHIN".equalsIgnoreCase(op)) {
            throw new IllegalArgumentException("SEQUENCE_WITHIN 연산자는 룰에서 사용할 수 없습니다. 집계(Aggregate)에서만 사용 가능합니다.");
        }
    }

    private Map<String, Object> parseObject(String json, String name) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>(){});
        } catch (Exception e) { throw new IllegalArgumentException(name + " must be JSON object", e); }
    }

    private List<Map<String, Object>> parseList(String json, String name) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>(){});
        } catch (Exception e) { throw new IllegalArgumentException(name + " must be JSON array", e); }
    }

    private String str(Object o) { return o == null ? "" : o.toString(); }
}

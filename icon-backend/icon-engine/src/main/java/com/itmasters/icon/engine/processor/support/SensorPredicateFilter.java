package com.itmasters.icon.engine.processor.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineEventStreamEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.SensorEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.SensorRepository;
import com.itmasters.icon.engine.evaluator.impl.SimpleRuleEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * predicate_sensor_id 또는 where_json 조건을 기반으로 이벤트를 필터링하는 헬퍼.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensorPredicateFilter {

    private final SensorRepository sensorRepository;
    private final SimpleRuleEvaluator simpleRuleEvaluator;
    private final ObjectMapper objectMapper;

    public List<EngineEventStreamEntity> filter(String predicateSensorId
            , List<EngineEventStreamEntity> events
            , RuleEntity ruleEntity) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }

        if (predicateSensorId == null || predicateSensorId.isBlank()) {
            String whereJson = ruleEntity.getWhereJson();
            if (whereJson != null && !whereJson.isBlank()) {
                log.info("✅ predicate_rule_id는 NULL이지만 where_json 있음 - where_json으로 필터링 ({}건)", events.size());
                return filterByWhereJson(events, whereJson);
            }
            log.info("✅ predicate_rule_id와 where_json 모두 NULL - 필터링 없이 모든 이벤트 사용 ({}건)", events.size());
            return events;
        }

        SensorEntity sensor = sensorRepository.findBySensorId(predicateSensorId).orElse(null);
        if (sensor != null) {
            String sensorWhereJson = sensor.getWhereJson();
            if (sensorWhereJson != null && !sensorWhereJson.isBlank()) {
                log.info("🔍 [FILTER-DEBUG] Sensor predicate found: {} - where_json 적용", sensor.getSensorId());
                List<EngineEventStreamEntity> result = filterByWhereJson(events, sensorWhereJson);
                return result;
            }
            log.warn("🔍 [FILTER-DEBUG] Sensor {} has no where_json - 필터링 없이 모든 이벤트 반환", predicateSensorId);
            return List.of();
        }

        log.warn("⚠️ predicate_sensor_id={} 를 찾을 수 없어 탐지를 건너뜁니다.", predicateSensorId);
        return Collections.emptyList();
    }

    private List<EngineEventStreamEntity> filterByWhereJson(List<EngineEventStreamEntity> events, String whereJson) {
        if (whereJson == null || whereJson.isBlank()) {
            return events;
        }

        List<EngineEventStreamEntity> matchedEvents = new ArrayList<>();
        try {
            JsonNode whereNode = objectMapper.readTree(whereJson);
            for (EngineEventStreamEntity e : events) {
                boolean matched = evaluateWhereNode(e.getEventData(), whereNode);
                if (matched) {
                    matchedEvents.add(e);
                }
            }
        } catch (Exception ex) {
            log.error("⚠️ where_json 파싱/평가 실패 - whereJson={}, error={}", whereJson, ex.getMessage(), ex);
            return Collections.emptyList();
        }
        return matchedEvents;
    }

    private boolean evaluateWhereNode(Map<String, Object> eventData, JsonNode whereNode) {
        if (whereNode.isArray()) {
            for (JsonNode conditionNode : whereNode) {
                if (!evaluateCondition(eventData, conditionNode)) {
                    return false;
                }
            }
            return true;
        }
        if (whereNode.isObject()) {
            if (whereNode.has("fieldName") || whereNode.has("field")) {
                return evaluateCondition(eventData, whereNode);
            }
            return evaluateSimpleConditions(eventData, whereNode);
        }
        return false;
    }

    private boolean evaluateCondition(Map<String, Object> eventData, JsonNode conditionNode) {
        try {
            String conditionJson = objectMapper.writeValueAsString(conditionNode);
            return simpleRuleEvaluator.evaluate(eventData, conditionJson);
        } catch (Exception e) {
            log.error("조건 평가 실패: {}", e.getMessage());
            return false;
        }
    }

    private boolean evaluateSimpleConditions(Map<String, Object> eventData, JsonNode whereNode) {
        try {
            var fields = whereNode.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                String fieldName = entry.getKey();
                JsonNode expectedValueNode = entry.getValue();
                Object actualValue = eventData.get(fieldName);

                if (expectedValueNode.isNull()) {
                    if (actualValue != null) {
                        return false;
                    }
                } else {
                    Object expectedValue;
                    if (expectedValueNode.isBoolean()) {
                        expectedValue = expectedValueNode.asBoolean();
                    } else if (expectedValueNode.isNumber()) {
                        expectedValue = expectedValueNode.numberValue();
                    } else {
                        expectedValue = expectedValueNode.asText();
                    }
                    if (!Objects.equals(actualValue, expectedValue)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.error("간단한 조건 평가 실패: {}", e.getMessage());
            return false;
        }
    }
}

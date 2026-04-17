package com.itmasters.icon.engine.service;

import com.itmasters.icon.common.domain.rule.RuleConditionFactory;
import com.itmasters.icon.common.domain.rule.condition.RuleCondition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineEventStreamEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineRuleEntity;
import com.itmasters.icon.engine.evaluator.RuleEvaluator;
import com.itmasters.icon.engine.evaluator.RuleEvaluatorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * 룰 평가 서비스
 * 룰 타입별로 적절한 평가기를 선택하여 평가 수행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEvaluationService {
    private final EventStreamService eventStreamService;
    private final ObjectMapper objectMapper;
    private final java.util.concurrent.ConcurrentHashMap<String, RuleCondition> conditionCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 룰 평가 수행
     *
     * @param rule    평가할 룰
     * @param context 평가 컨텍스트 (현재 데이터)
     * @return 룰 매칭 여부 true: 조건포함, false: 미포함
     */
    public boolean evaluateRule(EngineRuleEntity rule, Map<String, Object> context) {
        try {
            // 조건 데이터에서 RuleCondition 객체 생성
            RuleCondition condition = parseCondition(rule);

            // 이력 필요 조건은 자동으로 이력 조회 경로로 라우팅
            if (condition.requiresHistory()) {
                // groupKey는 해당 축(fieldName)의 현재 값으로 파생
                String field = condition.getFieldName();
                Object keyVal = field == null ? null : context.get(field);
                if (keyVal == null) {
                    log.warn("[RULE] requiresHistory but groupKey missing - ruleId: {}, field: {}", rule.getRuleId(), field);
                    return false;
                }

                String groupKey = String.valueOf(keyVal);
                // 시간 윈도우(분) 값은 condition.getValue()에서 가져옴
                int minutes;
                try {
                    Object v = condition.getValue();
                    minutes = (v instanceof Number) ? ((Number) v).intValue() : Integer.parseInt(String.valueOf(v));
                } catch (Exception e) {
                    log.error("[RULE] invalid minutes for history condition - ruleId: {}, value: {}", rule.getRuleId(), condition.getValue());
                    return false;
                }

                java.time.LocalDateTime anchor = resolveAnchorTime(context);
                java.util.List<com.itmasters.icon.engine.adapter.out.persistence.entity.EngineEventStreamEntity> eventHistory =
                        eventStreamService.getEventsWithinWindow(groupKey, anchor, minutes);

                boolean result = condition.matchesWithHistory(context, eventHistory);
                log.debug("[RULE] history evaluation - ruleId: {}, field: {}, groupKey: {}, minutes: {}, history: {}, result: {}",
                        rule.getRuleId(), field, groupKey, minutes, eventHistory == null ? 0 : eventHistory.size(), result);
                return result;
            }

            // 기본 조건 평가
            return condition.matches(context);
        } catch (Exception e) {
            log.error("Error evaluating rule {}: {}", rule.getRuleId(), e.getMessage(), e);
            return false;
        }
    }


    /**
     * 조건 데이터 JSON을 RuleCondition 객체로 파싱
     */
    private RuleCondition parseCondition(String conditionData) {
        try {
            String data = conditionData;
            // Prefer new where_json if present in rule JSON string form (passed via conditionData when calling overloaded)
            String trimmed = data == null ? "" : data.trim();
            if (trimmed.startsWith("[")) {
                // 복합 조건 (AND): [{fieldName,operator,value}, ...]
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = objectMapper.readValue(data, java.util.List.class);
                List<RuleCondition> children = new ArrayList<>();
                for (Map<String, Object> m : items) {
                    String f = (String) m.get("fieldName");
                    String op = (String) m.get("operator");
                    Object v = m.get("value");
                    children.add(RuleConditionFactory.createCondition(f,
                            com.itmasters.icon.common.domain.rule.RuleOperator.fromSymbol(op), v));
                }
                return new com.itmasters.icon.common.domain.rule.condition.CompositeCondition(children);
            } else if (trimmed.startsWith("{")) {
                // 객체 형태: {"operator": "AND", "conditions": [...]} 또는 {"fieldName": ..., "operator": ..., "value": ...}
                @SuppressWarnings("unchecked")
                Map<String, Object> root = objectMapper.readValue(data, Map.class);
                String rootOperator = (String) root.get("operator");
                
                // AND/OR 복합 조건 처리
                if (rootOperator != null && ("AND".equalsIgnoreCase(rootOperator) || "OR".equalsIgnoreCase(rootOperator))) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> conditionsList = (List<Map<String, Object>>) root.get("conditions");
                    if (conditionsList == null || conditionsList.isEmpty()) {
                        throw new RuntimeException("conditions array is missing or empty for operator: " + rootOperator);
                    }
                    List<RuleCondition> children = new ArrayList<>();
                    for (Map<String, Object> m : conditionsList) {
                        String f = (String) m.get("fieldName");
                        String op = (String) m.get("operator");
                        Object v = m.get("value");
                        children.add(RuleConditionFactory.createCondition(f,
                                com.itmasters.icon.common.domain.rule.RuleOperator.fromSymbol(op), v));
                    }
                    return new com.itmasters.icon.common.domain.rule.condition.CompositeCondition(children);
                } else {
                    // 단일 조건 (객체 형태)
                    String fieldName = (String) root.get("fieldName");
                    String operatorName = (String) root.get("operator");
                    Object value = root.get("value");
                    return RuleConditionFactory.createCondition(fieldName,
                            com.itmasters.icon.common.domain.rule.RuleOperator.fromSymbol(operatorName), value);
                }
            } else {
                throw new RuntimeException("Unknown condition format: " + trimmed);
            }
        } catch (Exception e) {
            log.error("Failed to parse condition data: {}", conditionData, e);
            throw new RuntimeException("Invalid condition data", e);
        }
    }

    // Overload: require where_json on entity (no fallback)
    private RuleCondition parseCondition(com.itmasters.icon.engine.adapter.out.persistence.entity.EngineRuleEntity rule) {
        String json = rule.getWhereJson();
        if (json == null || json.isBlank()) {
            throw new RuntimeException("Missing where_json for rule " + rule.getRuleId());
        }
        String key = rule.getRuleId() + "|" + Integer.toHexString(json.hashCode());
        return conditionCache.computeIfAbsent(key, k -> parseCondition(json));
    }
    
    /**
     * 이력 기반 룰 평가 수행 (Event Stream 데이터 활용)
     *
     * @param rule        평가할 룰
     * @param context     현재 데이터 컨텍스트
     * @param groupKey    그룹 키 (customer_id, session_id 등)
     * @param timeWindowMinutes 조회할 이력 시간 범위 (분)
     * @param maxHistoryEvents  조회할 최대 이벤트 수
     * @return 룰 매칭 여부
     */
    public boolean evaluateRuleWithHistory(EngineRuleEntity rule, 
                                         Map<String, Object> context,
                                         String groupKey,
                                         int timeWindowMinutes,
                                         int maxHistoryEvents) {
        try {
            // 조건 데이터에서 RuleCondition 객체 생성
            RuleCondition condition = parseCondition(rule);
            
            // 이력 데이터가 필요한 조건인지 확인 (예: COUNT_WITHIN, SUM_WITHIN 등)
            if (!condition.requiresHistory()) {
                log.debug("Rule {} does not require history, using basic evaluation", rule.getRuleId());
                return condition.matches(context);
            }
            
            // 이력 데이터 조회 (현재 이벤트 시각 기준 윈도우)
            java.time.LocalDateTime anchor = resolveAnchorTime(context);
            List<EngineEventStreamEntity> eventHistory = eventStreamService.getEventsWithinWindow(
                groupKey,
                anchor,
                timeWindowMinutes
            );
            if (maxHistoryEvents > 0 && eventHistory.size() > maxHistoryEvents) {
                eventHistory = eventHistory.subList(0, maxHistoryEvents);
            }
            
            log.debug("Rule {} (domain: {}, operator: {}) history evaluation - groupKey: {}, history events: {}", 
                     rule.getRuleId(), rule.getDomain(), rule.getOperator(), groupKey, eventHistory.size());
            
            // 이력 기반 평가 수행 (TODO: 이력 기반 평가 로직 구현 필요)
            boolean result = condition.matchesWithHistory(context, eventHistory);
            
            log.debug("Rule {} (domain: {}, operator: {}) history evaluation result: {}", 
                     rule.getRuleId(), rule.getDomain(), rule.getOperator(), result);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error evaluating rule {} with history: {}", rule.getRuleId(), e.getMessage(), e);
            return false;
        }
    }

    private java.time.LocalDateTime resolveAnchorTime(Map<String, Object> context) {
        Object dt = context.get("transaction_datetime");
        if (dt instanceof java.time.LocalDateTime ldt) return ldt;
        if (dt instanceof String s) {
            try { return java.time.LocalDateTime.parse(s.replace(" ", "T")); } catch (Exception ignore) {}
        }
        Object evt = context.get("event_dt");
        if (evt instanceof java.time.LocalDateTime ldt2) return ldt2;
        if (evt instanceof String s2) {
            try { return java.time.LocalDateTime.parse(s2.replace(" ", "T")); } catch (Exception ignore) {}
        }
        return java.time.LocalDateTime.now();
    }
    
    /**
     * 여러 룰을 이력 기반으로 평가하고 매칭된 룰 목록 반환
     *
     * @param rules       평가할 룰 목록
     * @param context     현재 데이터 컨텍스트
     * @param groupKey    그룹 키 (customer_id, session_id 등)
     * @param timeWindowMinutes 조회할 이력 시간 범위 (분)
     * @param maxHistoryEvents  조회할 최대 이벤트 수
     * @return 매칭된 룰 목록
     */
    public List<EngineRuleEntity> evaluateRulesWithHistory(List<EngineRuleEntity> rules,
                                                          Map<String, Object> context,
                                                          String groupKey,
                                                          int timeWindowMinutes,
                                                          int maxHistoryEvents) {
        return rules.stream()
                .filter(rule -> {
                    try {
                        return evaluateRuleWithHistory(rule, context, groupKey, timeWindowMinutes, maxHistoryEvents);
                    } catch (Exception e) {
                        log.error("Failed to evaluate rule {} with history: {}", rule.getRuleId(), e.getMessage());
                        return false;
                    }
                })
                .toList();
    }
    
    /**
     * 스마트 룰 평가: 평가기가 이력을 필요로 하면 이력 기반으로, 아니면 기본 방법으로 평가
     *
     * @param rule        평가할 룰
     * @param context     현재 데이터 컨텍스트
     * @param groupKey    그룹 키 (이력 기반 평가 시 필요)
     * @param timeWindowMinutes 조회할 이력 시간 범위 (분)
     * @param maxHistoryEvents  조회할 최대 이벤트 수
     * @return 룰 매칭 여부
     */
    public boolean evaluateRuleSmart(EngineRuleEntity rule,
                                   Map<String, Object> context,
                                   String groupKey,
                                   int timeWindowMinutes,
                                   int maxHistoryEvents) {
        
        // 조건 데이터에서 RuleCondition 객체 생성
        RuleCondition condition = parseCondition(rule);
        
        // 이력이 필요한지 확인하고 적절한 평가 방법 선택
        if (condition.requiresHistory()) {
            int window = timeWindowMinutes;
            try {
                Object v = condition.getValue();
                if (v != null) {
                    String s = v.toString();
                    String[] parts = s.split(",");
                    if (condition.getOperator() == com.itmasters.icon.common.domain.rule.RuleOperator.SEQUENCE_WITHIN) {
                        // prev,next,minutes
                        if (parts.length >= 3) window = Integer.parseInt(parts[2].trim());
                    } else {
                        // minutes,rest
                        if (parts.length >= 1) window = Integer.parseInt(parts[0].trim());
                    }
                }
            } catch (Exception ignore) {}
            return evaluateRuleWithHistory(rule, context, groupKey, window, maxHistoryEvents);
        } else {
            return evaluateRule(rule, context);
        }
    }
}

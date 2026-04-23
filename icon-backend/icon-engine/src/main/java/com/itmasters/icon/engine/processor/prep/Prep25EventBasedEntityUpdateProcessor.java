package com.itmasters.icon.engine.processor.prep;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityAttributeEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineEventStreamEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityAttributeRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityUpdateRuleRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EventStreamRepository;
import com.itmasters.icon.engine.dto.Step2Result;
import com.itmasters.icon.engine.util.ExpressionEvaluator;
import com.itmasters.icon.entity.EntityUpdateRuleEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * PREP-2.5: 이벤트 기반 엔티티 업데이트 프로세서

 * 책임:
 * - event_stream에 저장된 이벤트를 기반으로 entity_attributes 업데이트
 * - entity_update_rules (trigger_type='EVENT')에 정의된 규칙 적용
 * - 동적 표현식 평가를 통한 유연한 업데이트 로직

 * 실행 시점:
 * - PREP-2 (Load) 이후, PREP-3 (Transform) 이전
 * - event_stream에 데이터가 저장된 직후

 * 실행 순서:
 * 1. event_stream에서 해당 exec의 이벤트 조회
 * 2. 각 data_source_id별 entity_update_rules 조회
 * 3. event_condition 필터링
 * 4. entity_id_expression으로 대상 엔티티 ID 계산
 * 5. update_type에 따라 업데이트 (SET/INCREMENT/DECREMENT/DELETE)
 * 6. delete_entity_if 조건 체크하여 엔티티 삭제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Prep25EventBasedEntityUpdateProcessor {

    private final EventStreamRepository eventStreamRepository;
    private final EntityUpdateRuleRepository entityUpdateRuleRepository;
    private final EntityAttributeRepository entityAttributeRepository;
    private final ExpressionEvaluator expressionEvaluator;
    private final ObjectMapper objectMapper;

    /**
     * PREP-2.5 실행: 이벤트 기반 엔티티 업데이트
     *
     * @param step2Result PREP-2 실행 결과
     * @return 업데이트된 엔티티 수
     */
    @Transactional
    public int execute(Step2Result step2Result) {
        log.info("========== PREP-2.5 시작 (이벤트 기반 엔티티 업데이트) ==========");
        log.info("ExecDsMpId: {}", step2Result.getExecDsMpId());

        Long execDsMpId = step2Result.getExecDsMpId();
        String dataSourceId = step2Result.getDataSourceId();

        // 1. 해당 exec의 event_stream 조회
        List<EngineEventStreamEntity> events = eventStreamRepository.findByExecDsMpId(execDsMpId);

        if (events == null || events.isEmpty()) {
            // [2026-04-23] WARN → DEBUG 변경: event_stream 대상이 아닌 프로파일(ENTITY_ATTRIBUTES only 등)은
            //              정상적으로 event_stream이 없을 수 있으므로 WARN은 로그 오염 유발
            log.debug("해당 exec에 대한 event_stream이 없음 (정상) - execDsMpId: {}", execDsMpId);
            log.info("========== PREP-2.5 완료 (이벤트 기반 엔티티 업데이트) - 업데이트 없음 ==========");
            return 0;
        }

        log.info("이벤트 {} 건 조회됨", events.size());

        // 2. data_source_id별 이벤트 기반 업데이트 규칙 조회
        List<EntityUpdateRuleEntity> rules = entityUpdateRuleRepository
                .findActiveRulesByDataSourceId(dataSourceId);

        if (rules == null || rules.isEmpty()) {
            log.debug("이벤트 기반 업데이트 규칙 없음 - dataSourceId: {}", dataSourceId);
            log.info("========== PREP-2.5 완료 (이벤트 기반 엔티티 업데이트) - 규칙 없음 ==========");
            return 0;
        }

        log.info("이벤트 기반 업데이트 규칙 {} 개 발견", rules.size());

        // 3. 각 이벤트에 대해 업데이트 규칙 적용
        int updatedCount = 0;
        Map<String, Set<String>> processedEntities = new HashMap<>(); // 중복 업데이트 방지

        for (EngineEventStreamEntity event : events) {
            try {
                // 이벤트 데이터를 Map으로 변환 (표현식 평가용 컨텍스트)
                Map<String, Object> eventData = convertEventDataToMap(event);

                // 각 규칙 적용
                for (EntityUpdateRuleEntity rule : rules) {
                    try {
                        // 3-1. 이벤트 조건 필터링
                        if (!matchesEventCondition(rule, eventData)) {
                            continue;
                        }

                        // 3-2. 엔티티 ID 계산
                        String entityId = calculateEntityId(rule, eventData);
                        if (entityId == null) {
                            log.warn("엔티티 ID 계산 실패 - rule: {}, event: {}",
                                    rule.getEntityUpdateRuleId(), event.getEventStreamId());
                            continue;
                        }

                        String entityType = rule.getEntityType();

                        // 중복 업데이트 방지 체크
                        String entityKey = entityType + ":" + entityId;
                        String fieldKey = rule.getFieldName() != null ? rule.getFieldName() : "DELETE";

                        Set<String> processedFields = processedEntities
                                .computeIfAbsent(entityKey, k -> new HashSet<>());

                        if (processedFields.contains(fieldKey)) {
                            log.debug("이미 처리된 필드 스킵 - entityType={}, entityId={}, field={}",
                                    entityType, entityId, fieldKey);
                            continue;
                        }

                        // 3-3. 업데이트 실행
                        boolean updated = applyEntityUpdate(rule, entityType, entityId, eventData);

                        if (updated) {
                            updatedCount++;
                            processedFields.add(fieldKey);
                            log.info("✅ 이벤트 기반 엔티티 업데이트 성공 - entityType={}, entityId={}, updateType={}",
                                    entityType, entityId, rule.getUpdateType());
                        }

                    } catch (Exception e) {
                        log.error("업데이트 규칙 적용 실패 - ruleId={}, eventId={}",
                                rule.getEntityUpdateRuleId(), event.getEventStreamId(), e);
                    }
                }

            } catch (Exception e) {
                log.error("이벤트 처리 실패 - eventId={}", event.getEventStreamId(), e);
            }
        }

        log.info("========== PREP-2.5 완료 (이벤트 기반 엔티티 업데이트) - 업데이트: {} 건 ==========",
                updatedCount);
        return updatedCount;
    }

    /**
     * 이벤트 데이터를 Map으로 변환
     *
     * @param event 이벤트 엔티티
     * @return 이벤트 데이터 Map
     */
    private Map<String, Object> convertEventDataToMap(EngineEventStreamEntity event) {
        Map<String, Object> eventData = new HashMap<>();

        // event_data (이미 Map으로 저장됨) 가져오기
        if (event.getEventData() != null) {
            eventData.putAll(event.getEventData());
        }

        // 이벤트 메타데이터 추가
        eventData.put("event_stream_id", event.getEventStreamId());
        eventData.put("mapped_storage_id", event.getMappedDataStorageId());
        eventData.put("transaction_id", event.getTransactionId());
        eventData.put("event_dt", event.getEventDt());

        return eventData;
    }

    /**
     * 이벤트 조건 매칭 체크
     *
     * @param rule      업데이트 규칙
     * @param eventData 이벤트 데이터
     * @return 조건 매칭 여부
     */
    private boolean matchesEventCondition(EntityUpdateRuleEntity rule, Map<String, Object> eventData) {
        // event_condition이 없으면 모든 이벤트 허용
        if (rule.getEventCondition() == null || rule.getEventCondition().trim().isEmpty()) {
            return true;
        }

        try {
            // JSON 조건식 파싱
            Map<String, Object> conditionJson = objectMapper.readValue(
                    rule.getEventCondition(),
                    new TypeReference<Map<String, Object>>() {}
            );

            // 조건 평가
            return expressionEvaluator.evaluateJsonCondition(conditionJson, eventData);

        } catch (Exception e) {
            log.error("이벤트 조건 평가 실패 - ruleId: {}, condition: {}",
                    rule.getEntityUpdateRuleId(), rule.getEventCondition(), e);
            return false;
        }
    }

    /**
     * 엔티티 ID 계산 (동적 표현식 평가)
     *
     * @param rule      업데이트 규칙
     * @param eventData 이벤트 데이터
     * @return 계산된 엔티티 ID
     */
    private String calculateEntityId(EntityUpdateRuleEntity rule, Map<String, Object> eventData) {
        String expression = rule.getEntityIdExpression();

        if (expression == null || expression.trim().isEmpty()) {
            log.warn("entity_id_expression이 비어있음 - ruleId: {}", rule.getEntityUpdateRuleId());
            return null;
        }

        try {
            return expressionEvaluator.evaluateAsString(expression, eventData);
        } catch (Exception e) {
            log.error("엔티티 ID 계산 실패 - expression: {}, eventData: {}",
                    expression, eventData, e);
            return null;
        }
    }

    /**
     * 엔티티 업데이트 적용
     *
     * @param rule       업데이트 규칙
     * @param entityType 엔티티 타입
     * @param entityId   엔티티 ID
     * @param eventData  이벤트 데이터
     * @return 업데이트 성공 여부
     */
    private boolean applyEntityUpdate(EntityUpdateRuleEntity rule,
                                       String entityType,
                                       String entityId,
                                       Map<String, Object> eventData) {
        try {
            String updateType = rule.getUpdateType() != null ? rule.getUpdateType() : "SET";

            switch (updateType.toUpperCase()) {
                case "SET":
                    return applySetUpdate(rule, entityType, entityId, eventData);

                case "INCREMENT":
                    return applyIncrementUpdate(rule, entityType, entityId, eventData);

                case "DECREMENT":
                    return applyDecrementUpdate(rule, entityType, entityId, eventData);

                case "DELETE":
                    return applyDeleteUpdate(rule, entityType, entityId, eventData);

                default:
                    log.warn("지원하지 않는 update_type: {}", updateType);
                    return false;
            }

        } catch (Exception e) {
            log.error("엔티티 업데이트 적용 실패 - entityType={}, entityId={}",
                    entityType, entityId, e);
            return false;
        }
    }

    /**
     * SET 타입 업데이트 (값 설정)
     */
    private boolean applySetUpdate(EntityUpdateRuleEntity rule,
                                    String entityType,
                                    String entityId,
                                    Map<String, Object> eventData) {
        try {
            // 1. 엔티티 조회 또는 생성
            EntityAttributeEntity entity = getOrCreateEntity(entityType, entityId);
            Map<String, Object> attributes = entity.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
            }

            // 2. 필드 값 계산
            Object fieldValue = calculateFieldValue(rule, eventData);

            // 3. 값 설정
            attributes.put(rule.getFieldName(), fieldValue);

            // 4. 저장
            entityAttributeRepository.save(entity);

            // 5. 삭제 조건 체크
            checkAndDeleteEntity(rule, entity, eventData);

            return true;

        } catch (Exception e) {
            log.error("SET 업데이트 실패 - entityType={}, entityId={}",
                    entityType, entityId, e);
            return false;
        }
    }

    /**
     * INCREMENT 타입 업데이트 (값 증가)
     */
    private boolean applyIncrementUpdate(EntityUpdateRuleEntity rule,
                                          String entityType,
                                          String entityId,
                                          Map<String, Object> eventData) {
        try {
            EntityAttributeEntity entity = getOrCreateEntity(entityType, entityId);
            Map<String, Object> attributes = entity.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
            }

            // 현재 값 가져오기
            Number currentValue = (Number) attributes.getOrDefault(rule.getFieldName(), 0);

            // 증가할 값 계산
            Number incrementValue = calculateFieldValueAsNumber(rule, eventData);

            // 새 값 계산
            double newValue = currentValue.doubleValue() + incrementValue.doubleValue();
            attributes.put(rule.getFieldName(), newValue);

            entityAttributeRepository.save(entity);
            checkAndDeleteEntity(rule, entity, eventData);

            return true;

        } catch (Exception e) {
            log.error("INCREMENT 업데이트 실패 - entityType={}, entityId={}",
                    entityType, entityId, e);
            return false;
        }
    }

    /**
     * DECREMENT 타입 업데이트 (값 감소)
     */
    private boolean applyDecrementUpdate(EntityUpdateRuleEntity rule,
                                          String entityType,
                                          String entityId,
                                          Map<String, Object> eventData) {
        try {
            EntityAttributeEntity entity = getOrCreateEntity(entityType, entityId);
            Map<String, Object> attributes = entity.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
            }

            // 현재 값 가져오기
            Number currentValue = (Number) attributes.getOrDefault(rule.getFieldName(), 0);

            // 감소할 값 계산
            Number decrementValue = calculateFieldValueAsNumber(rule, eventData);

            // 새 값 계산
            double newValue = currentValue.doubleValue() - decrementValue.doubleValue();
            attributes.put(rule.getFieldName(), newValue);

            entityAttributeRepository.save(entity);
            checkAndDeleteEntity(rule, entity, eventData);

            return true;

        } catch (Exception e) {
            log.error("DECREMENT 업데이트 실패 - entityType={}, entityId={}",
                    entityType, entityId, e);
            return false;
        }
    }

    /**
     * DELETE 타입 업데이트 (엔티티 삭제)
     */
    private boolean applyDeleteUpdate(EntityUpdateRuleEntity rule,
                                       String entityType,
                                       String entityId,
                                       Map<String, Object> eventData) {
        try {
            Optional<EntityAttributeEntity> entityOpt = entityAttributeRepository
                    .findByEntityTypeAndEntityId(entityType, entityId);

            if (entityOpt.isPresent()) {
                entityAttributeRepository.delete(entityOpt.get());
                log.info("엔티티 삭제됨 - entityType={}, entityId={}", entityType, entityId);
                return true;
            } else {
                log.debug("삭제할 엔티티가 없음 - entityType={}, entityId={}", entityType, entityId);
                return false;
            }

        } catch (Exception e) {
            log.error("DELETE 업데이트 실패 - entityType={}, entityId={}",
                    entityType, entityId, e);
            return false;
        }
    }

    /**
     * 엔티티 조회 또는 생성
     */
    private EntityAttributeEntity getOrCreateEntity(String entityType, String entityId) {
        Optional<EntityAttributeEntity> existing = entityAttributeRepository
                .findByEntityTypeAndEntityId(entityType, entityId);

        if (existing.isPresent()) {
            return existing.get();
        } else {
            // 새 엔티티 생성 (Shell)
            return EntityAttributeEntity.createShell(
                    entityType,
                    entityId,
                    "EVENT_BASED_UPDATE",
                    LocalDateTime.now(),
                    new HashMap<>()
            );
        }
    }

    /**
     * 필드 값 계산 (동적 표현식 평가)
     */
    private Object calculateFieldValue(EntityUpdateRuleEntity rule, Map<String, Object> eventData) {
        String expression = rule.getFieldValueExpression();

        if (expression == null || expression.trim().isEmpty()) {
            // 표현식이 없으면 fieldValue 그대로 사용
            return rule.getFieldValue();
        }

        try {
            return expressionEvaluator.evaluateAsString(expression, eventData);
        } catch (Exception e) {
            log.error("필드 값 계산 실패 - expression: {}", expression, e);
            return rule.getFieldValue();
        }
    }

    /**
     * 필드 값 계산 (숫자)
     */
    private Number calculateFieldValueAsNumber(EntityUpdateRuleEntity rule, Map<String, Object> eventData) {
        String expression = rule.getFieldValueExpression();

        if (expression == null || expression.trim().isEmpty()) {
            // 표현식이 없으면 fieldValue를 숫자로 변환
            try {
                return Double.parseDouble(rule.getFieldValue());
            } catch (Exception e) {
                log.warn("fieldValue를 숫자로 변환 실패: {}", rule.getFieldValue());
                return 0;
            }
        }

        try {
            return expressionEvaluator.evaluateAsNumber(expression, eventData);
        } catch (Exception e) {
            log.error("필드 값 계산 실패 (숫자) - expression: {}", expression, e);
            return 0;
        }
    }

    /**
     * 엔티티 삭제 조건 체크
     */
    private void checkAndDeleteEntity(EntityUpdateRuleEntity rule,
                                       EntityAttributeEntity entity,
                                       Map<String, Object> eventData) {
        // delete_entity_if 조건이 없으면 스킵
        if (rule.getDeleteEntityIf() == null || rule.getDeleteEntityIf().trim().isEmpty()) {
            return;
        }

        try {
            // 엔티티 속성을 컨텍스트에 포함하여 조건 평가
            Map<String, Object> context = new HashMap<>(eventData);
            if (entity.getAttributes() != null) {
                context.putAll(entity.getAttributes());
            }

            // 삭제 조건 평가
            Boolean shouldDelete = expressionEvaluator.evaluateAsBoolean(
                    rule.getDeleteEntityIf(),
                    context
            );

            if (shouldDelete != null && shouldDelete) {
                entityAttributeRepository.delete(entity);
                log.info("조건부 엔티티 삭제됨 - entityType={}, entityId={}, condition={}",
                        entity.getEntityType(), entity.getEntityId(), rule.getDeleteEntityIf());
            }

        } catch (Exception e) {
            log.error("엔티티 삭제 조건 평가 실패 - condition: {}", rule.getDeleteEntityIf(), e);
        }
    }
}

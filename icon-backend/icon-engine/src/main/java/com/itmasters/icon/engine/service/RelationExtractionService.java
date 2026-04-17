package com.itmasters.icon.engine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityRelationEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.RelationRuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityRelationRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.RelationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 관계 추출 서비스 (설정 기반)
 *
 * 핵심 원리:

   * -relation_rules에 정의된 규칙에 따라 관계 자동 생성
   * -하드코딩 없이 완전히 설정 기반으로 동작
   * -새 관계 타입 추가 시 코드 수정 불필요

 *
 * 처리 흐름:

   * -DataSource의 활성 규칙 조회
   * -각 규칙의 from_id_field, to_id_field 확인
   * -이벤트 데이터에 둘 다 있으면 관계 생성/업데이트

 *
 * 예시:
 * 
 * 규칙: CUSTOMER(customer_id) --OWNS--> ACCOUNT(account_id)
 * 이벤트: {"customer_id": "C001", "account_id": "A1234"}
 * → CUSTOMER:C001 --OWNS--> ACCOUNT:A1234 관계 생성
 * 
 *
 * @since 2025-02-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RelationExtractionService {

    private final EntityRelationRepository relationRepository;
    private final RelationRuleRepository ruleRepository;
    private final EntityExtractionService entityExtractionService;
    private final ObjectMapper objectMapper;

    /**
     * 이벤트로부터 관계 추출 (설정 기반)
     *
     * 처리 순서:

   * -먼저 To 엔티티들을 entity_attributes에 생성 (EntityExtractionService)
   * -그 다음 관계를 entity_relations에 생성

     *
     * @param dataSourceId DataSource ID
     * @param eventData 이벤트 데이터 (Map 형태)
     * @param eventTime 이벤트 발생 시각
     */
    @Transactional
    public void extractRelationsFromEvent(
            String dataSourceId,
            Map<String, Object> eventData,
            LocalDateTime eventTime) {

        // 0. 먼저 To 엔티티들을 entity_attributes에 생성
        entityExtractionService.extractEntitiesFromEvent(dataSourceId, eventData, eventTime);

        // 1. 이 DataSource의 활성 규칙 조회
        List<RelationRuleEntity> activeRules = ruleRepository.findByDataSourceIdAndIsActive(dataSourceId, true);

        if (activeRules.isEmpty()) {
            log.debug("활성 관계 규칙이 없음 - dataSourceId: {}", dataSourceId);
            return;
        }

        log.debug("활성 관계 규칙 {} 개 조회 - dataSourceId: {}", activeRules.size(), dataSourceId);

        // 2. 각 규칙을 순회하며 조건 만족하면 관계 추출
        int created = 0, updated = 0, skipped = 0;

        for (RelationRuleEntity rule : activeRules) {
            try {
                RelationResult result = extractRelationByRule(rule, eventData, eventTime);
                
                switch (result) {
                    case CREATED -> created++;
                    case UPDATED -> updated++;
                    case SKIPPED -> skipped++;
                }
                
            } catch (Exception e) {
                log.error("관계 추출 실패 - ruleId: {}, dataSourceId: {}", 
                    rule.getRuleId(), dataSourceId, e);
            }
        }

        log.info("관계 추출 완료 - dataSourceId: {}, 생성: {}, 업데이트: {}, 스킵: {}", 
            dataSourceId, created, updated, skipped);
    }

    /**
     * 단일 규칙에 따라 관계 추출
     *
     * @param rule 관계 규칙
     * @param eventData 이벤트 데이터
     * @param eventTime 이벤트 시각
     * @return 처리 결과
     */
    private RelationResult extractRelationByRule(
            RelationRuleEntity rule,
            Map<String, Object> eventData,
            LocalDateTime eventTime) {

        // From 엔티티 ID 확인
        Object fromId = eventData.get(rule.getFromIdField());
        if (fromId == null) {
            log.trace("From 필드 없음 - field: {}, rule: {}", 
                rule.getFromIdField(), rule.getRuleId());
            return RelationResult.SKIPPED;
        }

        // To 엔티티 ID 확인
        Object toIdValue = eventData.get(rule.getToIdField());
        if (toIdValue == null) {
            log.trace("To 필드 없음 - field: {}, rule: {}", 
                rule.getToIdField(), rule.getRuleId());
            return RelationResult.SKIPPED;
        }

        String fromEntityId = String.valueOf(fromId);
        
        // 배열 처리: toIdValue가 List인 경우 각 원소마다 관계 생성
        if (toIdValue instanceof List) {
            List<?> toIdList = (List<?>) toIdValue;
            
            if (toIdList.isEmpty()) {
                log.trace("To 필드가 빈 배열 - field: {}, rule: {}", 
                    rule.getToIdField(), rule.getRuleId());
                return RelationResult.SKIPPED;
            }
            
            int created = 0, updated = 0;
            
            for (Object toId : toIdList) {
                if (toId == null) continue;
                
                RelationResult result = upsertRelation(
                    rule,
                    fromEntityId,
                    String.valueOf(toId),
                    eventTime
                );
                
                if (result == RelationResult.CREATED) created++;
                if (result == RelationResult.UPDATED) updated++;
            }
            
            log.debug("배열 관계 추출 완료 - field: {}, count: {}, created: {}, updated: {}", 
                rule.getToIdField(), toIdList.size(), created, updated);
            
            // 하나라도 생성되었으면 CREATED, 모두 업데이트면 UPDATED
            return created > 0 ? RelationResult.CREATED : 
                   updated > 0 ? RelationResult.UPDATED : RelationResult.SKIPPED;
        }
        
        // 단일 값 처리
        return upsertRelation(
            rule,
            fromEntityId,
            String.valueOf(toIdValue),
            eventTime
        );
    }

    /**
     * 관계 생성 또는 업데이트 (UPSERT)
     *
     * @param rule 관계 규칙
     * @param fromId From 엔티티 ID
     * @param toId To 엔티티 ID
     * @param eventTime 이벤트 시각
     * @return 처리 결과
     */
    private RelationResult upsertRelation(
            RelationRuleEntity rule,
            String fromId,
            String toId,
            LocalDateTime eventTime) {

        String fromType = rule.getFromEntityType();
        String relationType = rule.getRelationType();
        String toType = rule.getToEntityType();

        // 기존 관계 조회
        Optional<EntityRelationEntity> existing = relationRepository
                .findByFromEntityTypeAndFromEntityIdAndRelationTypeAndToEntityTypeAndToEntityId(
                        fromType, fromId, relationType, toType, toId);

        if (existing.isPresent()) {
            // 업데이트
            EntityRelationEntity relation = existing.get();
            updateRelationProperties(relation, eventTime);
            relationRepository.save(relation);

            log.debug("관계 업데이트: {} {} --{}-->  {} {}",
                fromType, fromId, relationType, toType, toId);
            return RelationResult.UPDATED;

        } else {
            // 새로 생성
            ObjectNode props = createInitialProperties(rule, fromId, toId, eventTime);

            EntityRelationEntity newRelation = EntityRelationEntity.of(
                    fromType, fromId, relationType, toType, toId, props);

            relationRepository.save(newRelation);

            log.debug("관계 생성: {} {} --{}--> {} {}",
                fromType, fromId, relationType, toType, toId);
            return RelationResult.CREATED;
        }
    }

    /**
     * 관계 속성 업데이트 (템플릿 기반 - 단순화)
     *
     * @param relation 기존 관계
     * @param eventTime 이벤트 시각
     */
    private void updateRelationProperties(
            EntityRelationEntity relation,
            LocalDateTime eventTime) {

        ObjectNode props = (ObjectNode) relation.getProperties();
        if (props == null) {
            props = objectMapper.createObjectNode();
        }

        // 모든 관계 타입에 대해 last_updated 갱신
        props.put("last_updated", eventTime.toString());

        relation.updateProperties(props);
    }

    /**
     * 초기 관계 속성 생성 (템플릿 기반)
     *
     * @param rule 관계 규칙 (템플릿 포함)
     * @param fromId From 엔티티 ID
     * @param toId To 엔티티 ID
     * @param eventTime 이벤트 시각
     * @return 초기 속성
     */
    private ObjectNode createInitialProperties(
            RelationRuleEntity rule,
            String fromId,
            String toId,
            LocalDateTime eventTime) {

        ObjectNode props = objectMapper.createObjectNode();

        // 템플릿이 있으면 템플릿 사용, 없으면 기본값
        if (rule.getPropertiesTemplate() != null && !rule.getPropertiesTemplate().isNull()) {
            // 템플릿을 복사하고 변수 치환
            props = replaceTemplateVariables(
                (ObjectNode) rule.getPropertiesTemplate(),
                fromId,
                toId,
                eventTime
            );
        } else {
            // 템플릿이 없으면 기본 속성만 설정
            props.put("created", eventTime.toString());
            props.put("last_updated", eventTime.toString());
        }

        return props;
    }

    /**
     * 템플릿 변수 치환
     *
     * @param template 속성 템플릿
     * @param fromId From 엔티티 ID
     * @param toId To 엔티티 ID
     * @param eventTime 이벤트 시각
     * @return 변수가 치환된 속성
     */
    private ObjectNode replaceTemplateVariables(
            ObjectNode template,
            String fromId,
            String toId,
            LocalDateTime eventTime) {

        ObjectNode result = objectMapper.createObjectNode();
        String eventTimeStr = eventTime.toString();

        template.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            var value = entry.getValue();

            if (value.isTextual()) {
                String textValue = value.asText();
                // 변수 치환
                textValue = textValue.replace("${event_time}", eventTimeStr);
                textValue = textValue.replace("${from_entity_id}", fromId);
                textValue = textValue.replace("${to_entity_id}", toId);
                result.put(key, textValue);
            } else if (value.isNumber()) {
                result.set(key, value);
            } else if (value.isBoolean()) {
                result.set(key, value);
            } else {
                // 기타 타입은 그대로 복사
                result.set(key, value);
            }
        });

        return result;
    }

    /**
     * 관계 추출 결과
     */
    private enum RelationResult {
        CREATED,  // 새 관계 생성됨
        UPDATED,  // 기존 관계 업데이트됨
        SKIPPED   // 필드 없어서 스킵됨
    }
}

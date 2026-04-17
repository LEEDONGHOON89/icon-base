package com.itmasters.icon.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityAttributeEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EntitySourceRecordEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.RelationRuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityAttributeRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntitySourceRecordRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.RelationRuleRepository;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineProfileEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 엔티티 추출 서비스 (설정 기반)
 *
 * 핵심 원리:

   * -relation_rules의 to_entity 정보를 사용하여 엔티티 자동 생성
   * -Shell 엔티티 생성: ID만 알고 속성은 나중에 보강
   * -점진적 Enrichment: 여러 DataSource에서 정보 통합

 *
 * 처리 흐름:

   * -DataSource의 활성 규칙 조회
   * -각 규칙의 to_entity_type, to_id_field, to_entity_attributes_template 확인
   * -이벤트 데이터에서 to_id 추출하여 엔티티 생성/업데이트

 *
 * @since 2025-12-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityExtractionService {

    private final EntityAttributeRepository entityAttributeRepository;
    private final EntitySourceRecordRepository entitySourceRecordRepository;
    private final RelationRuleRepository relationRuleRepository;
    private final ObjectMapper objectMapper;

    /**
     * 이벤트로부터 To 엔티티 추출 (설정 기반)
     *
     * @param dataSourceId DataSource ID
     * @param eventData 이벤트 데이터 (Map 형태)
     * @param eventTime 이벤트 발생 시각
     */
    @Transactional
    public void extractEntitiesFromEvent(
            String dataSourceId,
            Map<String, Object> eventData,
            LocalDateTime eventTime) {

        // 1. 이 DataSource의 활성 규칙 조회
        List<RelationRuleEntity> activeRules = relationRuleRepository.findByDataSourceIdAndIsActive(dataSourceId, true);

        if (activeRules.isEmpty()) {
            log.debug("활성 관계 규칙이 없음 - dataSourceId: {}", dataSourceId);
            return;
        }

        log.debug("활성 관계 규칙 {} 개 조회 - dataSourceId: {}", activeRules.size(), dataSourceId);

        // 2. 각 규칙을 순회하며 To 엔티티 추출
        int created = 0, updated = 0, skipped = 0;

        for (RelationRuleEntity rule : activeRules) {
            try {
                EntityResult result = extractEntityByRule(rule, dataSourceId, eventData, eventTime);

                switch (result) {
                    case CREATED -> created++;
                    case UPDATED -> updated++;
                    case SKIPPED -> skipped++;
                }

            } catch (Exception e) {
                log.error("엔티티 추출 실패 - ruleId: {}, dataSourceId: {}",
                    rule.getRuleId(), dataSourceId, e);
            }
        }

        log.info("엔티티 추출 완료 - dataSourceId: {}, 생성: {}, 업데이트: {}, 스킵: {}",
            dataSourceId, created, updated, skipped);
    }

    /**
     * Profile 기반 엔티티 추출 (설정 기반)
     *
     * @param profile 프로파일 설정
     * @param eventData 이벤트 데이터 (Map 형태)
     * @param eventTime 이벤트 발생 시각
     */
    @Transactional
    public void extractEntitiesFromProfile(
            EngineProfileEntity profile,
            Map<String, Object> eventData,
            LocalDateTime eventTime) {

        // Profile이 entity 저장을 해야 하는지 확인
        if (!profile.shouldStoreEntityAttributes()) {
            log.debug("Profile이 엔티티 저장 대상이 아님 - profileId: {}", profile.getProfileId());
            return;
        }

        // Entity 타입과 ID 필드 확인
        if (profile.getEntityType() == null || profile.getEntityIdField() == null) {
            log.warn("Profile에 entityType 또는 entityIdField가 없음 - profileId: {}", profile.getProfileId());
            return;
        }

        String entityType = profile.getEntityType().name();
        String entityIdField = profile.getEntityIdField();

        // Entity ID 추출
        Object entityIdValue = eventData.get(entityIdField);
        if (entityIdValue == null) {
            log.trace("Entity ID 필드 없음 - field: {}, profileId: {}", entityIdField, profile.getProfileId());
            return;
        }

        String entityId = String.valueOf(entityIdValue);

        // 저장할 속성 추출
        Map<String, Object> attributes = extractAttributesFromEvent(profile, eventData);

        // 1. 원본 데이터 이력 저장 (entity_source_records)
        saveSourceRecord(
            entityType,
            entityId,
            profile.getDataSourceId(),
            eventData,
            eventTime
        );

        // 2. 엔티티 생성/업데이트 (entity_attributes)
        upsertEntityFromProfile(
            profile,
            entityType,
            entityId,
            profile.getDataSourceId(),
            eventTime,
            attributes
        );

        log.debug("Profile 기반 엔티티 처리 완료 - type: {}, id: {}, profileId: {}",
            entityType, entityId, profile.getProfileId());
    }

    /**
     * Profile의 storeFields 설정에 따라 이벤트 데이터에서 속성 추출
     *
     * @param profile 프로파일 설정
     * @param eventData 이벤트 데이터
     * @return 추출된 속성
     */
    private Map<String, Object> extractAttributesFromEvent(
            EngineProfileEntity profile,
            Map<String, Object> eventData) {

        Map<String, Object> attributes = new HashMap<>();

        if (profile.shouldStoreAllFields()) {
            // 모든 필드 저장
            attributes.putAll(eventData);
        } else {
            // 지정된 필드만 저장
            List<String> storeFields = profile.getStoreFields();
            for (String field : storeFields) {
                if (eventData.containsKey(field)) {
                    attributes.put(field, eventData.get(field));
                }
            }
        }

        return attributes;
    }

    /**
     * Profile 기반 엔티티 생성 또는 업데이트 (UPSERT)
     *
     * @param profile 프로파일 설정
     * @param entityType 엔티티 타입
     * @param entityId 엔티티 ID
     * @param dataSourceId DataSource ID
     * @param eventTime 이벤트 시각
     * @param attributes 엔티티 속성
     * @return 처리 결과
     */
    private EntityResult upsertEntityFromProfile(
            EngineProfileEntity profile,
            String entityType,
            String entityId,
            String dataSourceId,
            LocalDateTime eventTime,
            Map<String, Object> attributes) {

        // 기존 엔티티 조회
        Optional<EntityAttributeEntity> existing =
            entityAttributeRepository.findByEntityTypeAndEntityId(entityType, entityId);

        if (existing.isPresent()) {
            // 업데이트 (Enrichment)
            EntityAttributeEntity entity = existing.get();
            entity.updateAttributes(attributes);
            entityAttributeRepository.save(entity);

            log.debug("엔티티 enrichment (Profile): {} {}", entityType, entityId);
            return EntityResult.UPDATED;

        } else {
            // 새로 생성 (Shell)
            EntityAttributeEntity newEntity = EntityAttributeEntity.createShell(
                entityType,
                entityId,
                dataSourceId,
                eventTime,
                attributes
            );

            entityAttributeRepository.save(newEntity);

            log.debug("엔티티 생성 (Profile): {} {}", entityType, entityId);
            return EntityResult.CREATED;
        }
    }

    /**
     * 단일 규칙에 따라 To 엔티티 추출
     *
     * @param rule 관계 규칙
     * @param dataSourceId DataSource ID
     * @param eventData 이벤트 데이터
     * @param eventTime 이벤트 시각
     * @return 처리 결과
     */
    private EntityResult extractEntityByRule(
            RelationRuleEntity rule,
            String dataSourceId,
            Map<String, Object> eventData,
            LocalDateTime eventTime) {

        // 디버그: eventData 내용 출력
        log.info("[EntityExtraction] eventData keys: {}", eventData.keySet());
        log.info("[EntityExtraction] Looking for field: {}", rule.getToIdField());

        // To 엔티티 ID 확인
        Object toIdValue = eventData.get(rule.getToIdField());
        if (toIdValue == null) {
            log.warn("[EntityExtraction] To 필드 없음 - field: {}, rule: {}, eventData: {}",
                rule.getToIdField(), rule.getRuleId(), eventData);
            return EntityResult.SKIPPED;
        }

        log.info("[EntityExtraction] Found toIdValue: {} (type: {})", toIdValue, toIdValue.getClass().getName());

        String toEntityType = rule.getToEntityType();

        // 배열 처리: toIdValue가 List인 경우 각 원소마다 엔티티 생성
        if (toIdValue instanceof List) {
            List<?> toIdList = (List<?>) toIdValue;

            if (toIdList.isEmpty()) {
                log.trace("To 필드가 빈 배열 - field: {}, rule: {}",
                    rule.getToIdField(), rule.getRuleId());
                return EntityResult.SKIPPED;
            }

            int created = 0, updated = 0;

            for (Object toId : toIdList) {
                if (toId == null) continue;

                EntityResult result = upsertEntity(
                    rule,
                    toEntityType,
                    String.valueOf(toId),
                    dataSourceId,
                    eventTime
                );

                if (result == EntityResult.CREATED) created++;
                if (result == EntityResult.UPDATED) updated++;
            }

            log.debug("배열 엔티티 추출 완료 - type: {}, field: {}, count: {}, created: {}, updated: {}",
                toEntityType, rule.getToIdField(), toIdList.size(), created, updated);

            // 하나라도 생성되었으면 CREATED, 모두 업데이트면 UPDATED
            return created > 0 ? EntityResult.CREATED :
                   updated > 0 ? EntityResult.UPDATED : EntityResult.SKIPPED;
        }

        // 단일 값 처리
        return upsertEntity(
            rule,
            toEntityType,
            String.valueOf(toIdValue),
            dataSourceId,
            eventTime
        );
    }

    /**
     * 엔티티 생성 또는 업데이트 (UPSERT)
     *
     * @param rule 관계 규칙 (템플릿 포함)
     * @param entityType 엔티티 타입
     * @param entityId 엔티티 ID
     * @param dataSourceId DataSource ID
     * @param eventTime 이벤트 시각
     * @return 처리 결과
     */
    private EntityResult upsertEntity(
            RelationRuleEntity rule,
            String entityType,
            String entityId,
            String dataSourceId,
            LocalDateTime eventTime) {

        // 기존 엔티티 조회
        Optional<EntityAttributeEntity> existing =
            entityAttributeRepository.findByEntityTypeAndEntityId(entityType, entityId);

        if (existing.isPresent()) {
            // 업데이트 (Enrichment)
            EntityAttributeEntity entity = existing.get();

            // 템플릿이 있으면 속성 병합
            if (rule.getToEntityAttributesTemplate() != null && !rule.getToEntityAttributesTemplate().isNull()) {
                Map<String, Object> newAttributes = createAttributesFromTemplate(
                    rule.getToEntityAttributesTemplate(),
                    entityId,
                    dataSourceId,
                    eventTime
                );
                entity.updateAttributes(newAttributes);
            }

            entityAttributeRepository.save(entity);

            log.debug("엔티티 enrichment: {} {}", entityType, entityId);
            return EntityResult.UPDATED;

        } else {
            // 새로 생성 (Shell)
            Map<String, Object> attributes = new HashMap<>();

            // 템플릿이 있으면 템플릿 사용
            if (rule.getToEntityAttributesTemplate() != null && !rule.getToEntityAttributesTemplate().isNull()) {
                attributes = createAttributesFromTemplate(
                    rule.getToEntityAttributesTemplate(),
                    entityId,
                    dataSourceId,
                    eventTime
                );
            }

            EntityAttributeEntity newEntity = EntityAttributeEntity.createShell(
                entityType,
                entityId,
                dataSourceId,
                eventTime,
                attributes
            );

            entityAttributeRepository.save(newEntity);

            log.debug("엔티티 생성 (Shell): {} {}", entityType, entityId);
            return EntityResult.CREATED;
        }
    }

    /**
     * 템플릿으로부터 속성 생성
     *
     * @param template 속성 템플릿
     * @param entityId 엔티티 ID
     * @param dataSourceId DataSource ID
     * @param eventTime 이벤트 시각
     * @return 생성된 속성
     */
    private Map<String, Object> createAttributesFromTemplate(
            com.fasterxml.jackson.databind.JsonNode template,
            String entityId,
            String dataSourceId,
            LocalDateTime eventTime) {

        Map<String, Object> attributes = new HashMap<>();
        String eventTimeStr = eventTime.toString();

        template.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            var value = entry.getValue();

            if (value.isTextual()) {
                String textValue = value.asText();
                // 변수 치환
                textValue = textValue.replace("${event_time}", eventTimeStr);
                textValue = textValue.replace("${data_source_id}", dataSourceId);
                textValue = textValue.replace("${to_entity_id}", entityId);
                attributes.put(key, textValue);
            } else if (value.isNumber()) {
                attributes.put(key, value.numberValue());
            } else if (value.isBoolean()) {
                attributes.put(key, value.booleanValue());
            } else {
                // 기타 타입은 문자열로 변환
                attributes.put(key, value.toString());
            }
        });

        return attributes;
    }

    /**
     * 원본 데이터 이력 저장 (entity_source_records)
     *
     * DataSource의 원본 row 데이터를 그대로 저장합니다.
     * entity_attributes는 최신 상태만 유지하지만,
     * entity_source_records는 모든 이력을 보관합니다.
     *
     * @param entityType 엔티티 타입
     * @param entityId 엔티티 ID
     * @param dataSourceId DataSource ID
     * @param eventData 원본 이벤트 데이터
     * @param eventTime 이벤트 시각
     */
    private void saveSourceRecord(
            String entityType,
            String entityId,
            String dataSourceId,
            Map<String, Object> eventData,
            LocalDateTime eventTime) {

        try {
            // Map을 JsonNode로 변환
            com.fasterxml.jackson.databind.JsonNode sourceData =
                objectMapper.valueToTree(eventData);

            // sourceTxId 추출 (있다면)
            String sourceTxId = extractSourceTxId(eventData);

            // entity_source_records 저장
            EntitySourceRecordEntity sourceRecord = EntitySourceRecordEntity.of(
                entityType,
                entityId,
                dataSourceId,
                sourceTxId,
                sourceData,
                eventTime
            );

            entitySourceRecordRepository.save(sourceRecord);

            log.debug("원본 데이터 이력 저장 완료 - type: {}, id: {}, dataSourceId: {}",
                entityType, entityId, dataSourceId);

        } catch (Exception e) {
            log.error("원본 데이터 이력 저장 실패 - type: {}, id: {}, dataSourceId: {}",
                entityType, entityId, dataSourceId, e);
            // 원본 저장 실패해도 엔티티 추출은 계속 진행
        }
    }

    /**
     * 원본 트랜잭션 ID 추출
     *
     * eventData에서 트랜잭션 ID 필드를 찾습니다.
     * 우선순위: transaction_id > hr_tx_id > cust_tx_id > tx_id
     *
     * @param eventData 이벤트 데이터
     * @return 트랜잭션 ID (없으면 null)
     */
    private String extractSourceTxId(Map<String, Object> eventData) {
        // 트랜잭션 ID 후보 필드들 (우선순위 순서)
        String[] txIdFields = {
            "transaction_id",
            "hr_tx_id",
            "cust_tx_id",
            "corp_tx_id",
            "tx_id",
            "source_tx_id"
        };

        for (String field : txIdFields) {
            Object value = eventData.get(field);
            if (value != null) {
                return String.valueOf(value);
            }
        }

        return null;
    }

    /**
     * 엔티티 추출 결과
     */
    private enum EntityResult {
        CREATED,  // 새 엔티티 생성됨
        UPDATED,  // 기존 엔티티 업데이트됨
        SKIPPED   // 필드 없어서 스킵됨
    }
}

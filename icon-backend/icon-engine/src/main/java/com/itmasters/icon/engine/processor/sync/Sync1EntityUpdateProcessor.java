package com.itmasters.icon.engine.processor.sync;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectScenarioEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityAttributeEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.DetectScenarioRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityAttributeRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityUpdateRuleRepository;
import com.itmasters.icon.entity.EntityUpdateRuleEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * SYNC-1: Entity Update 프로세서

 * 책임:
 * - detect_scenarios 기반 entity_attributes 업데이트
 * - entity_update_rules에 정의된 규칙에 따라 엔티티 속성 갱신

 * 실행 순서:
 * 1. detect_scenarios에서 최근 탐지된 시나리오 조회 (execDsMpId 기준)
 * 2. 각 시나리오에 대해 entity_update_rules 조회
 * 3. 규칙이 있으면 해당 entity의 entity_attributes 업데이트
 * 4. 업데이트 결과 반환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Sync1EntityUpdateProcessor {

    private final DetectScenarioRepository detectScenarioRepository;
    private final EntityUpdateRuleRepository entityUpdateRuleRepository;
    private final EntityAttributeRepository entityAttributeRepository;

    /**
     * SYNC-1 실행: Entity 속성 업데이트 (mappedStorageId 기준)
     *
     * @param mappedStorageId mapped_storage ID
     * @return 업데이트된 엔티티 수
     */
    @Transactional
    public int executeByMappedStorageId(Long mappedStorageId) {
        log.info("========== SYNC-1 시작 (Entity Update - mappedStorageId 기반) ==========");
        log.info("mappedStorageId: {}", mappedStorageId);

        // 1. 해당 mappedStorageId의 detect_scenarios 조회
        List<DetectScenarioEntity> scenarios = detectScenarioRepository.findByMappedStorageId(mappedStorageId);

        if (scenarios == null || scenarios.isEmpty()) {
            log.debug("해당 mappedStorageId에 대한 detect_scenarios가 없음 - mappedStorageId: {}", mappedStorageId);
            log.info("========== SYNC-1 완료 (Entity Update - mappedStorageId 기반) - 업데이트 없음 ==========");
            return 0;
        }

        log.info("탐지된 시나리오 {} 개 조회됨", scenarios.size());

        // 2. 각 시나리오에 대해 업데이트 규칙 조회 및 적용
        int updatedCount = 0;
        Map<String, Set<String>> processedEntities = new HashMap<>(); // entityType+entityId별 중복 방지

        for (DetectScenarioEntity scenario : scenarios) {
            String scenarioId = scenario.getScenarioId();
            String groupKey = scenario.getGroupKey();

            // 2-1. 시나리오에 대한 업데이트 규칙 조회
            List<EntityUpdateRuleEntity> rules = entityUpdateRuleRepository.findActiveRulesByScenarioId(scenarioId);

            if (rules == null || rules.isEmpty()) {
                log.debug("업데이트 규칙 없음 - scenarioId: {}", scenarioId);
                continue;
            }

            log.debug("업데이트 규칙 {} 개 발견 - scenarioId: {}", rules.size(), scenarioId);

            // 2-2. 각 규칙 적용
            for (EntityUpdateRuleEntity rule : rules) {
                try {
                    // groupKey에서 entityId 추출 (groupKey == entityId)
                    String entityId = groupKey;
                    String entityType = rule.getEntityType();

                    // 중복 업데이트 방지
                    String entityKey = entityType + ":" + entityId + ":" + rule.getFieldName();
                    Set<String> processedFields = processedEntities.computeIfAbsent(entityType + ":" + entityId, k -> new HashSet<>());

                    if (processedFields.contains(rule.getFieldName())) {
                        log.debug("이미 처리된 필드 스킵 - entityType={}, entityId={}, fieldName={}",
                                 entityType, entityId, rule.getFieldName());
                        continue;
                    }

                    // Entity Attribute 업데이트
                    boolean updated = updateEntityAttribute(rule, entityType, entityId);

                    if (updated) {
                        updatedCount++;
                        processedFields.add(rule.getFieldName());
                        log.info("✅ Entity 속성 업데이트 성공 - entityType={}, entityId={}, field={}, value={}",
                                entityType, entityId, rule.getFieldName(), rule.getFieldValue());
                    }

                } catch (Exception e) {
                    log.error("Entity 업데이트 실패 - scenarioId={}, ruleId={}, groupKey={}",
                             scenarioId, rule.getEntityUpdateRuleId(), groupKey, e);
                }
            }
        }

        log.info("========== SYNC-1 완료 (Entity Update - mappedStorageId 기반) - 업데이트: {} 건 ==========", updatedCount);
        return updatedCount;
    }

    /**
     * Entity Attribute 업데이트 (UPSERT)
     *
     * @param rule       업데이트 규칙
     * @param entityType 엔티티 타입
     * @param entityId   엔티티 ID
     * @return 업데이트 성공 여부
     */
    private boolean updateEntityAttribute(EntityUpdateRuleEntity rule, String entityType, String entityId) {
        try {
            // 1. 기존 entity_attributes 조회
            Optional<EntityAttributeEntity> existingOpt = entityAttributeRepository
                    .findByEntityTypeAndEntityId(entityType, entityId);

            EntityAttributeEntity entity;
            Map<String, Object> attributes;

            if (existingOpt.isPresent()) {
                // 기존 엔티티가 있으면 attributes 가져오기
                entity = existingOpt.get();
                attributes = entity.getAttributes();
                if (attributes == null) {
                    attributes = new HashMap<>();
                }
            } else {
                // 새 엔티티 생성 (Shell) - 탐지 결과로부터 발견됨
                attributes = new HashMap<>();
                entity = EntityAttributeEntity.createShell(
                    entityType,
                    entityId,
                    "SYNC1_ENTITY_UPDATE", // discoveredFrom: 탐지 기반 엔티티 업데이트에서 발견
                    LocalDateTime.now(),    // discoveredAt
                    attributes
                );
            }

            // 2. 필드 값 변환 및 업데이트
            Object convertedValue = convertFieldValue(rule.getFieldValue(), rule.getFieldType());
            attributes.put(rule.getFieldName(), convertedValue);

            // 3. 저장 (updatedAt은 @PreUpdate에서 자동 설정됨)
            entityAttributeRepository.save(entity);

            log.debug("Entity attribute 업데이트 완료 - entityType={}, entityId={}, field={}, value={}",
                     entityType, entityId, rule.getFieldName(), convertedValue);

            return true;

        } catch (Exception e) {
            log.error("Entity attribute 업데이트 실패 - entityType={}, entityId={}, field={}",
                     entityType, entityId, rule.getFieldName(), e);
            return false;
        }
    }

    /**
     * 필드 값 타입 변환
     *
     * @param fieldValue 필드 값 (문자열)
     * @param fieldType  필드 타입
     * @return 변환된 값
     */
    private Object convertFieldValue(String fieldValue, String fieldType) {
        if (fieldValue == null) {
            return null;
        }

        if (fieldType == null) {
            return fieldValue; // 타입이 없으면 문자열 그대로 반환
        }

        switch (fieldType.toUpperCase()) {
            case "BOOLEAN":
                return Boolean.parseBoolean(fieldValue);

            case "NUMBER":
            case "INTEGER":
                try {
                    return Integer.parseInt(fieldValue);
                } catch (NumberFormatException e) {
                    log.warn("정수 변환 실패, Double로 시도 - value: {}", fieldValue);
                    return Double.parseDouble(fieldValue);
                }

            case "DOUBLE":
            case "DECIMAL":
                return Double.parseDouble(fieldValue);

            case "DATE":
            case "DATETIME":
                return LocalDateTime.parse(fieldValue);

            case "STRING":
            default:
                return fieldValue;
        }
    }
}

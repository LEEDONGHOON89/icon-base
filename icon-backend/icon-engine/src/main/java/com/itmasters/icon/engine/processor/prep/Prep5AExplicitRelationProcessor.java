package com.itmasters.icon.engine.processor.prep;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityRelationEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityRelationFieldEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityRelationFieldRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityRelationRepository;
import com.itmasters.icon.engine.dto.MappedDataRow;
import com.itmasters.icon.engine.dto.Step1Result;
import com.itmasters.icon.engine.service.ExecDsMpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * PREP-5-A: Explicit Relation Processor (명시적 관계 즉시 생성)
 *
 * 책임:
 * - entity_relation_fields 설정에 따라 데이터 도착 시 즉시 관계 생성 (Palantir 방식)
 * - relation_type이 설정된 필드만 처리 (명시적 관계)
 * - 임계값 없음 (1건만 와도 즉시 생성)
 *
 * 입력: Step1Result (매핑된 데이터)
 * 출력: 생성된 관계 수
 *
 * 예시:
 * - entity_relation_fields에 "sender_account → TRANSFERS_TO → receiver_account" 설정
 * - 이체 데이터 도착: { sender_account: "A001", receiver_account: "A002" }
 * - 즉시 생성: entity_relations (ACCOUNT:A001 --TRANSFERS_TO--> ACCOUNT:A002)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Prep5AExplicitRelationProcessor {

    private final EntityRelationFieldRepository entityRelationFieldRepository;
    private final EntityRelationRepository entityRelationRepository;
    private final ExecDsMpService execDsMpService;
    private final ObjectMapper objectMapper;

    /**
     * PREP-5-A 실행: 명시적 관계 즉시 생성
     *
     * @param step1Result Step1 실행 결과
     * @return 생성된 관계 수
     */
    public int execute(Step1Result step1Result) {
        List<MappedDataRow> mappedDataRows = execDsMpService.ensureMappedData(step1Result.getExecDsMpId());
        Step1Result enrichedStep1 = step1Result.withMappedData(mappedDataRows);

        log.info("========== PREP-5-A 시작 (명시적 관계 즉시 생성) ==========");
        log.info("ExecDsMpId: {}, DataSource: {}, 데이터 건수: {}",
                enrichedStep1.getExecDsMpId(), enrichedStep1.getDataSourceId(), enrichedStep1.getTotalRows());

        if (!enrichedStep1.hasData()) {
            log.warn("처리할 매핑된 데이터가 없음 - dataSourceId: {}", enrichedStep1.getDataSourceId());
            return 0;
        }

        try {
            // 1. 명시적 관계 설정 조회 (relation_type이 설정된 필드만)
            List<EntityRelationFieldEntity> explicitRelationFields =
                entityRelationFieldRepository.findByDataSourceIdAndIsEnabledOrderByPriorityDesc(
                    enrichedStep1.getDataSourceId(),
                    true
                ).stream()
                .filter(EntityRelationFieldEntity::hasExplicitRelation)  // relation_type이 있는 것만
                .toList();

            if (explicitRelationFields.isEmpty()) {
                log.info("명시적 관계 설정 없음 - dataSourceId: {}", enrichedStep1.getDataSourceId());
                return 0;
            }

            log.info("명시적 관계 설정: {} 개", explicitRelationFields.size());
            explicitRelationFields.forEach(field ->
                log.debug("  - {} ({}) --{}--> {} ({})",
                    field.getFieldName(), field.getEntityType(),
                    field.getRelationType(),
                    field.getTargetField(), field.getTargetEntityType())
            );

            // 2. 각 데이터 행에 대해 관계 생성
            int createdCount = createExplicitRelations(
                mappedDataRows,
                explicitRelationFields
            );

            log.info("생성된 관계: {} 개", createdCount);
            log.info("========== PREP-5-A 완료 (명시적 관계 즉시 생성) ==========");

            return createdCount;

        } catch (Exception e) {
            log.error("명시적 관계 생성 실패 - dataSourceId: {}", enrichedStep1.getDataSourceId(), e);
            return 0;
        }
    }

    /**
     * 명시적 관계 생성
     */
    @Transactional
    protected int createExplicitRelations(
            List<MappedDataRow> mappedDataRows,
            List<EntityRelationFieldEntity> explicitRelationFields) {

        int createdCount = 0;

        for (MappedDataRow row : mappedDataRows) {
            Map<String, Object> data = row.getRawData();

            for (EntityRelationFieldEntity config : explicitRelationFields) {
                String fromField = config.getFieldName();
                String targetField = config.getTargetField();

                // Case 1: targetField가 null인 경우 (배열 필드 처리 - owned_accounts 등)
                if (targetField == null || targetField.isEmpty()) {
                    createdCount += processArrayField(data, config);
                }
                // Case 2: targetField가 있는 경우 (기존 단일 값 처리)
                else {
                    createdCount += processSingleField(data, config, fromField, targetField);
                }
            }
        }

        return createdCount;
    }

    /**
     * 배열 필드 처리 (owned_accounts 등)
     * - fromField가 배열이면 각 요소를 to_entity_id로 사용
     * - from_entity_id는 entity_id 필드에서 가져옴
     */
    private int processArrayField(Map<String, Object> data, EntityRelationFieldEntity config) {
        int createdCount = 0;
        String fromField = config.getFieldName();

        // fromField 값 추출 (배열)
        Object fromValue = data.get(fromField);
        if (fromValue == null) {
            return 0;
        }

        // entity_id에서 from_entity_id 가져오기
        Object entityIdValue = data.get("entity_id");
        if (entityIdValue == null) {
            log.warn("entity_id 필드가 없음 - fromField: {}, data: {}", fromField, data);
            return 0;
        }
        String fromEntityId = String.valueOf(entityIdValue);

        // 배열 처리
        if (fromValue instanceof List) {
            List<?> toEntityIds = (List<?>) fromValue;

            for (Object toValue : toEntityIds) {
                if (toValue != null) {
                    String toEntityId = String.valueOf(toValue);

                    // 속성 생성
                    JsonNode properties = createProperties(data);

                    try {
                        // EntityRelationEntity 생성
                        EntityRelationEntity relation = EntityRelationEntity.of(
                            config.getEntityType(),        // from_entity_type (CORPORATE, EMPLOYEE 등)
                            fromEntityId,                  // from_entity_id (CORP_030, EMP030 등)
                            config.getRelationType(),      // relation_type (OWNS)
                            config.getTargetEntityType(),  // to_entity_type (ACCOUNT)
                            toEntityId,                    // to_entity_id (100-111-222333 등)
                            properties                     // properties (JSONB)
                        );

                        entityRelationRepository.save(relation);
                        createdCount++;

                        log.debug("배열 필드 관계 생성: {} ({}) --{}--> {} ({})",
                            config.getEntityType(), fromEntityId,
                            config.getRelationType(),
                            config.getTargetEntityType(), toEntityId);

                    } catch (DataIntegrityViolationException e) {
                        // UNIQUE 제약조건 위반 (이미 존재하는 관계)
                        log.trace("관계 이미 존재 (무시): {} ({}) --{}--> {} ({})",
                            config.getEntityType(), fromEntityId,
                            config.getRelationType(),
                            config.getTargetEntityType(), toEntityId);
                    }
                }
            }
        } else {
            log.warn("fromField가 배열이 아님 - fromField: {}, value: {}", fromField, fromValue);
        }

        return createdCount;
    }

    /**
     * 단일 필드 처리 (기존 로직)
     * - fromField, targetField 모두 단일 값
     */
    private int processSingleField(Map<String, Object> data, EntityRelationFieldEntity config,
                                    String fromField, String targetField) {
        int createdCount = 0;

        // from/to 필드 값 추출
        Object fromValue = data.get(fromField);
        Object toValue = data.get(targetField);

        // 둘 다 있으면 관계 생성
        if (fromValue != null && toValue != null) {
            String fromEntityId = String.valueOf(fromValue);
            String toEntityId = String.valueOf(toValue);

            // 속성 생성 (거래 일시, 채널 등 추가 정보)
            JsonNode properties = createProperties(data);

            try {
                // EntityRelationEntity 생성
                EntityRelationEntity relation = EntityRelationEntity.of(
                    config.getEntityType(),        // from_entity_type
                    fromEntityId,                  // from_entity_id
                    config.getRelationType(),      // relation_type
                    config.getTargetEntityType(),  // to_entity_type
                    toEntityId,                    // to_entity_id
                    properties                     // properties (JSONB)
                );

                entityRelationRepository.save(relation);
                createdCount++;

                log.debug("단일 필드 관계 생성: {} ({}) --{}--> {} ({})",
                    config.getEntityType(), fromEntityId,
                    config.getRelationType(),
                    config.getTargetEntityType(), toEntityId);

            } catch (DataIntegrityViolationException e) {
                // UNIQUE 제약조건 위반 (이미 존재하는 관계)
                log.trace("관계 이미 존재 (무시): {} ({}) --{}--> {} ({})",
                    config.getEntityType(), fromEntityId,
                    config.getRelationType(),
                    config.getTargetEntityType(), toEntityId);
            }
        }

        return createdCount;
    }

    /**
     * 관계 속성 생성 (거래 일시, 금액, 채널 등)
     */
    private JsonNode createProperties(Map<String, Object> data) {
        ObjectNode properties = objectMapper.createObjectNode();

        // 거래 일시
        if (data.containsKey("trx_dt")) {
            properties.put("trx_dt", String.valueOf(data.get("trx_dt")));
        }

        // 거래 금액
        if (data.containsKey("trx_amount")) {
            properties.put("trx_amount", String.valueOf(data.get("trx_amount")));
        }

        // 채널
        if (data.containsKey("channel")) {
            properties.put("channel", String.valueOf(data.get("channel")));
        }

        // 거래 타입
        if (data.containsKey("trx_type")) {
            properties.put("trx_type", String.valueOf(data.get("trx_type")));
        }

        return properties;
    }
}

package com.itmasters.icon.api.derivedfield.application.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.api.derivedfield.application.strategy.context.ComputationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ENTITY_RELATION_LOOKUP Strategy
 *
 * entity_relations 테이블에서 관계 존재 여부를 확인
 *
 * 설정 예시:
 * {
 *   "from_entity_type": "CUSTOMER",
 *   "from_id_field": "customer_id",
 *   "relation_type": "OWNS",
 *   "to_entity_type": "ACCOUNT",
 *   "to_id_field": "receiver_account",
 *   "not_exists": true
 * }
 *
 * 로직:
 * - CUSTOMER-OWNS-ACCOUNT 관계가 없으면 → is_third_party = true
 * - CUSTOMER-OWNS-ACCOUNT 관계가 있으면 → is_third_party = false
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityRelationLookupStrategy implements FieldComputationStrategy {

    private final ObjectMapper objectMapper;
    // TODO: EntityRelationRepository 추가 필요
    // private final EntityRelationRepository entityRelationRepository;

    @Override
    public Object compute(ComputationContext context) {
        try {
            String configJson = context.getComputationConfigJson();
            JsonNode config = objectMapper.readTree(configJson);

            // 설정 파싱
            String fromType = config.get("from_entity_type").asText();
            String fromIdField = config.get("from_id_field").asText();
            String relationType = config.get("relation_type").asText();
            String toType = config.get("to_entity_type").asText();
            String toIdField = config.get("to_id_field").asText();
            boolean notExists = config.get("not_exists").asBoolean();

            // 이벤트 데이터에서 ID 추출
            String fromId = context.getStringValue(fromIdField);
            String toId = context.getStringValue(toIdField);

            // TODO: entity_relations 테이블 조회
            // boolean relationExists = entityRelationRepository.existsByFromAndTo(
            //     fromType, fromId,
            //     relationType,
            //     toType, toId
            // );

            // 임시 구현: 항상 관계가 없다고 가정 (향후 실제 조회로 대체)
            boolean relationExists = false;

            log.debug("ENTITY_RELATION_LOOKUP: {}-[{}]->{} = {}",
                     fromType + "(" + fromId + ")",
                     relationType,
                     toType + "(" + toId + ")",
                     relationExists);

            // not_exists가 true면: 관계 없으면 true 반환
            return notExists ? !relationExists : relationExists;

        } catch (Exception e) {
            log.error("Failed to compute ENTITY_RELATION_LOOKUP: {}", e.getMessage(), e);
            throw new IllegalStateException("ENTITY_RELATION_LOOKUP computation failed", e);
        }
    }

    @Override
    public String getSupportedComputationType() {
        return "ENTITY_RELATION_LOOKUP";
    }
}

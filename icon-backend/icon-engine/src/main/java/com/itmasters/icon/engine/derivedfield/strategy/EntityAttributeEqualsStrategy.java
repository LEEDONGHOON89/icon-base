package com.itmasters.icon.engine.derivedfield.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityAttributeRepository;
import com.itmasters.icon.engine.derivedfield.strategy.context.ComputationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * ENTITY_ATTRIBUTE_EQUALS Strategy (범용)
 *
 * entity_attributes에서 특정 엔티티의 특정 속성 값이 기대값과 같은지 체크
 *
 * 설정 예시:
 * {
 *   "entity_type": "EMPLOYEE",              // 엔티티 타입
 *   "entity_id_field": "USER_ID",           // 엔티티 ID가 들어있는 이벤트 필드명
 *   "attribute_field": "status",            // 체크할 속성 필드명
 *   "expected_value": "RESIGN_PENDING"      // 기대값
 * }
 *
 * 활용 예시:
 * 1. 퇴직 예정자 체크:
 *    - entity_type="EMPLOYEE", attribute_field="status", expected_value="RESIGN_PENDING"
 * 2. VIP 고객 체크:
 *    - entity_type="CUSTOMER", attribute_field="grade", expected_value="VIP"
 * 3. 특정 부서 직원 체크:
 *    - entity_type="EMPLOYEE", attribute_field="dept_name", expected_value="영업1팀"
 *
 * 반환값:
 * - true: 속성 값이 기대값과 일치
 * - false: 속성 값이 기대값과 다름
 * - null: 판단 불가 (엔티티 없음, 속성 없음 등)
 */
@Slf4j
@Component("engineEntityAttributeEqualsStrategy")
@RequiredArgsConstructor
public class EntityAttributeEqualsStrategy implements FieldComputationStrategy {

    private final ObjectMapper objectMapper;

    @Override
    public Object compute(ComputationContext context) {
        try {
            String configJson = context.getComputationConfigJson();
            JsonNode config = objectMapper.readTree(configJson);

            // 설정 값 추출
            String entityType = config.get("entity_type").asText();
            String entityIdField = config.get("entity_id_field").asText();
            String attributeField = config.get("attribute_field").asText();
            String expectedValue = config.get("expected_value").asText();

            // 엔티티 ID 추출
            String entityId = context.getStringValue(entityIdField);
            if (entityId == null || entityId.trim().isEmpty()) {
                log.warn("ENTITY_ATTRIBUTE_EQUALS: entity_id_field '{}' is null or empty", entityIdField);
                return null;
            }

            // entity_attributes에서 엔티티 정보 조회
            EntityAttributeRepository entityAttributeRepository =
                context.getRepositoryHolder().getEntityAttributeRepository();

            Optional<Map<String, Object>> attributesOpt =
                entityAttributeRepository.findAttributesByEntity(entityType, entityId);

            if (attributesOpt.isEmpty()) {
                log.debug("ENTITY_ATTRIBUTE_EQUALS: 엔티티 속성 없음 ({}:{})", entityType, entityId);
                return null;
            }

            Map<String, Object> attributes = attributesOpt.get();
            Object actualValue = attributes.get(attributeField);

            if (actualValue == null) {
                log.debug("ENTITY_ATTRIBUTE_EQUALS: 속성 '{}' 없음 ({}:{})", 
                    attributeField, entityType, entityId);
                return null;
            }

            // 값 비교
            boolean isEquals = expectedValue.equals(actualValue.toString());

            log.info("ENTITY_ATTRIBUTE_EQUALS - entityType={}, entityId={}, " +
                    "attributeField={}, expectedValue={}, actualValue={}, isEquals={}",
                    entityType, entityId, attributeField, expectedValue, actualValue, isEquals);

            return isEquals;

        } catch (Exception e) {
            log.error("Failed to compute ENTITY_ATTRIBUTE_EQUALS: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String getSupportedComputationType() {
        return "ENTITY_ATTRIBUTE_EQUALS";
    }
}

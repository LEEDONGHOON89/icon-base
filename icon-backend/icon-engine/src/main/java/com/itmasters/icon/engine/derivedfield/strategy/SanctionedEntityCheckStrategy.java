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
 * SANCTIONED_ENTITY_CHECK Strategy
 *
 * entity_attributes에서 SANCTIONED_ENTITY를 조회하여
 * 거래 상대방이 제재 대상인지 확인합니다.
 *
 * 설정 예시:
 * {
 *   "source_field": "receiver_name",     // 확인할 이름 필드
 *   "entity_type": "SANCTIONED_ENTITY"   // 고정값
 * }
 *
 * 반환값:
 * - true: 제재 대상 (entity_attributes에 존재하고 is_active = true)
 * - false: 일반 (제재 대상 아님)
 * - null: 판단 불가 (데이터 부족)
 */
@Slf4j
@Component("engineSanctionedEntityCheckStrategy")
@RequiredArgsConstructor
public class SanctionedEntityCheckStrategy implements FieldComputationStrategy {

    private final ObjectMapper objectMapper;

    @Override
    public Object compute(ComputationContext context) {
        try {
            String configJson = context.getComputationConfigJson();
            JsonNode config = objectMapper.readTree(configJson);

            // 설정 값 추출
            String sourceField = config.get("source_field").asText();
            String entityType = config.has("entity_type") 
                ? config.get("entity_type").asText() 
                : "SANCTIONED_ENTITY";

            // receiver_name 값 추출
            String receiverName = context.getStringValue(sourceField);
            if (receiverName == null || receiverName.trim().isEmpty()) {
                log.debug("SANCTIONED_ENTITY_CHECK: source_field '{}' is null or empty", sourceField);
                return false; // 이름이 없으면 제재 대상 아님
            }

            // entity_attributes에서 SANCTIONED_ENTITY 조회
            EntityAttributeRepository entityAttributeRepository =
                context.getRepositoryHolder().getEntityAttributeRepository();

            Optional<Map<String, Object>> attributesOpt =
                entityAttributeRepository.findAttributesByEntity(entityType, receiverName);

            if (attributesOpt.isEmpty()) {
                log.debug("SANCTIONED_ENTITY_CHECK: 제재 대상 없음 ({}:{})", entityType, receiverName);
                return false; // 제재 리스트에 없으면 일반
            }

            Map<String, Object> attributes = attributesOpt.get();
            Object isActiveObj = attributes.get("is_active");

            if (isActiveObj == null) {
                log.warn("SANCTIONED_ENTITY_CHECK: is_active 필드 없음 ({}:{})", entityType, receiverName);
                return false; // is_active 없으면 비활성으로 간주
            }

            // is_active = true인 경우에만 제재 대상
            boolean isActive = parseBoolean(isActiveObj);
            
            if (isActive) {
                log.warn("⚠️ 제재 대상 거래 감지! receiver_name='{}', list_type={}, sanctions_type={}", 
                    receiverName,
                    attributes.get("list_type"),
                    attributes.get("sanctions_type"));
            }

            log.info("SANCTIONED_ENTITY_CHECK - receiverName={}, isActive={}, isSanctioned={}",
                    receiverName, isActive, isActive);

            return isActive;

        } catch (Exception e) {
            log.error("Failed to compute SANCTIONED_ENTITY_CHECK: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 다양한 타입의 boolean 값을 파싱
     */
    private boolean parseBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return false;
    }

    @Override
    public String getSupportedComputationType() {
        return "SANCTIONED_ENTITY_CHECK";
    }
}

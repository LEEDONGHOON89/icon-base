package com.itmasters.icon.engine.derivedfield.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityRelationRepository;
import com.itmasters.icon.engine.derivedfield.strategy.context.ComputationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * RELATION_EXISTS Strategy
 *
 * entity_relations 테이블에서 특정 관계가 존재하는지 확인합니다.
 * beneficiary_owner_id 같은 필드가 없어도 관계만 있으면 판단 가능합니다.
 *
 * 설정 파라미터:
 * - account_field (필수): 계좌번호가 담긴 필드명 (예: "account_number", "sender_account")
 * - from_entity_type (필수): FROM 엔티티 타입 (예: "EMPLOYEE", "CORPORATE")
 * - relation_type (필수): 관계 타입 (예: "OWNS")
 * - to_entity_type (필수): TO 엔티티 타입 (예: "ACCOUNT")
 *
 * 동작 방식:
 * 1. account_field에서 계좌번호 추출 (예: "220-333-444555")
 * 2. entity_relations에서 조회:
 *    FROM: from_entity_type, TO: to_entity_type(계좌번호), RELATION: relation_type
 * 3. 관계가 존재하면 true, 없으면 false
 *
 * 사용 예:
 * - is_employee_target: EMPLOYEE가 account_number를 OWNS하는지 확인
 * - is_corporate_source: CORPORATE가 sender_account를 OWNS하는지 확인
 */
@Slf4j
@Component("engineRelationExistsStrategy")
@RequiredArgsConstructor
public class RelationExistsStrategy implements FieldComputationStrategy {

    private final ObjectMapper objectMapper;

    @Override
    public Object compute(ComputationContext context) {
        try {
            String configJson = context.getComputationConfigJson();
            JsonNode config = objectMapper.readTree(configJson);

            // 필수 설정값 추출
            String accountField = config.get("account_field").asText();
            String fromEntityType = config.get("from_entity_type").asText();
            String relationType = config.get("relation_type").asText();
            String toEntityType = config.get("to_entity_type").asText();

            // 계좌번호 추출
            String accountNumber = context.getStringValue(accountField);

            // 계좌번호가 없으면 false
            if (accountNumber == null || accountNumber.isEmpty()) {
                log.debug("RELATION_EXISTS - account_field '{}' is null or empty -> false", accountField);
                return false;
            }

            // EntityRelationRepository 가져오기
            EntityRelationRepository relationRepository =
                context.getRepositoryHolder().getEntityRelationRepository();

            // 관계 존재 여부 확인
            boolean exists = relationRepository.existsByFromEntityTypeAndRelationTypeAndToEntityTypeAndToEntityId(
                fromEntityType,
                relationType,
                toEntityType,
                accountNumber
            );

            log.debug("RELATION_EXISTS - {} -{}-> {}:{} = {}",
                fromEntityType, relationType, toEntityType, accountNumber, exists);

            return exists;

        } catch (Exception e) {
            log.error("Failed to compute RELATION_EXISTS: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getSupportedComputationType() {
        return "RELATION_EXISTS";
    }
}

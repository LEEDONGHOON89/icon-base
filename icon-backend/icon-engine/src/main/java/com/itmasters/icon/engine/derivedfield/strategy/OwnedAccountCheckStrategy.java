package com.itmasters.icon.engine.derivedfield.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityAttributeRepository;
import com.itmasters.icon.engine.derivedfield.strategy.context.ComputationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OWNED_ACCOUNT_CHECK Strategy
 *
 * entity_attributes에서 지정된 계좌 목록을 조회하여
 * source_field가 해당 계좌 목록에 포함되는지 확인
 *
 * 설정 예시:
 * {
 *   "source_field": "receiver_account",     // 확인할 계좌번호 필드
 *   "entity_type": "CUSTOMER",              // 엔티티 타입
 *   "entity_id_field": "customer_id",       // 엔티티 ID 필드
 *   "account_field": "owned_accounts"       // 계좌 목록 필드명 (선택, 기본값: "owned_accounts")
 * }
 *
 * account_field 활용 예시:
 * - "owned_accounts": 본인 계좌 체크
 * - "family_accounts": 가족 계좌 체크
 * - "business_accounts": 사업자 계좌 체크
 *
 * 반환값:
 * - true: 타인 계좌 (계좌 목록에 없음)
 * - false: 본인/지정된 계좌 (계좌 목록에 있음)
 * - null: 판단 불가 (데이터 부족)
 */
@Slf4j
@Component("engineOwnedAccountCheckStrategy")
@RequiredArgsConstructor
public class OwnedAccountCheckStrategy implements FieldComputationStrategy {

    private final ObjectMapper objectMapper;

    @Override
    public Object compute(ComputationContext context) {
        try {
            String configJson = context.getComputationConfigJson();
            JsonNode config = objectMapper.readTree(configJson);

            // 설정 값 추출
            String sourceField = config.get("source_field").asText();
            String entityType = config.get("entity_type").asText();
            String entityIdField = config.get("entity_id_field").asText();

            // receiver_account 값 추출
            String receiverAccount = context.getStringValue(sourceField);
            if (receiverAccount == null || receiverAccount.trim().isEmpty()) {
                log.warn("OWNED_ACCOUNT_CHECK: source_field '{}' is null or empty", sourceField);
                return null;
            }

            // customer_id 값 추출
            String entityId = context.getStringValue(entityIdField);
            if (entityId == null || entityId.trim().isEmpty()) {
                log.warn("OWNED_ACCOUNT_CHECK: entity_id_field '{}' is null or empty", entityIdField);
                return null;
            }

            // account_field 파라미터 추출 (기본값: "owned_accounts")
            String accountFieldName = config.has("account_field")
                ? config.get("account_field").asText()
                : "owned_accounts";

            // entity_attributes에서 계좌 목록 조회
            EntityAttributeRepository entityAttributeRepository =
                context.getRepositoryHolder().getEntityAttributeRepository();

            Optional<Map<String, Object>> attributesOpt =
                entityAttributeRepository.findAttributesByEntity(entityType, entityId);

            if (attributesOpt.isEmpty()) {
                log.debug("OWNED_ACCOUNT_CHECK: 엔티티 속성 없음 ({}:{})", entityType, entityId);
                return null;
            }

            Map<String, Object> attributes = attributesOpt.get();
            Object ownedAccountsObj = attributes.get(accountFieldName);

            if (ownedAccountsObj == null) {
                log.debug("OWNED_ACCOUNT_CHECK: {} 없음 ({}:{})", accountFieldName, entityType, entityId);
                // 계좌 목록이 없으면 본인 계좌가 아님
                return false;
            }

            // 계좌 목록은 List<String>으로 저장됨
            if (ownedAccountsObj instanceof List) {
                List<?> accounts = (List<?>) ownedAccountsObj;
                boolean isOwnAccount = accounts.stream()
                    .map(Object::toString)
                    .anyMatch(account -> account.equals(receiverAccount));

                // true: 본인 계좌 (목록에 있음), false: 타인 계좌 (목록에 없음)
                log.info("OWNED_ACCOUNT_CHECK - entityId={}, receiverAccount={}, accountField={}, accounts={}, isOwnAccount={}",
                        entityId, receiverAccount, accountFieldName, accounts, isOwnAccount);

                return isOwnAccount;
            }

            log.warn("OWNED_ACCOUNT_CHECK: {} 타입 오류 ({})", accountFieldName, ownedAccountsObj.getClass());
            return null;

        } catch (Exception e) {
            log.error("Failed to compute OWNED_ACCOUNT_CHECK: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String getSupportedComputationType() {
        return "OWNED_ACCOUNT_CHECK";
    }
}

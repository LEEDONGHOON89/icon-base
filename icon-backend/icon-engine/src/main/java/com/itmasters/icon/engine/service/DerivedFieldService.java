package com.itmasters.icon.engine.service;

import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityAttributeRepository;
import com.itmasters.icon.engine.derivedfield.strategy.FieldComputationStrategy;
import com.itmasters.icon.engine.derivedfield.strategy.context.ComputationContext;
import com.itmasters.icon.engine.derivedfield.strategy.context.RepositoryHolder;
import com.itmasters.icon.entity.DerivedRuleEntity;
import com.itmasters.icon.engine.repository.DerivedRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 파생 필드 계산 서비스 (icon-engine용)
 *
 * mapped_data에 파생 필드를 추가합니다.
 * 처리 시점: Profile 매핑 완료 후, event_stream 저장 전
 *
 * Strategy Pattern을 사용하여 computation_type별 계산 로직을 분리합니다.
 */
@Slf4j
@Service("engineDerivedFieldService")
public class DerivedFieldService {

    private final DerivedRuleRepository derivedRuleRepository;
    private final EntityAttributeRepository entityAttributeRepository;
    private final Map<String, FieldComputationStrategy> strategies;
    private final RepositoryHolder repositoryHolder;

    /**
     * 생성자 - Spring이 모든 FieldComputationStrategy 구현체를 자동으로 주입합니다.
     *
     * @param derivedRuleRepository 규칙 조회용 Repository
     * @param entityAttributeRepository 엔티티 속성 조회용 Repository
     * @param strategyList 모든 Strategy 구현체 (Spring이 자동 수집)
     */
    public DerivedFieldService(
            DerivedRuleRepository derivedRuleRepository,
            EntityAttributeRepository entityAttributeRepository,
            com.itmasters.icon.engine.adapter.out.persistence.repository.EntityRelationRepository entityRelationRepository,
            List<FieldComputationStrategy> strategyList) {

        this.derivedRuleRepository = derivedRuleRepository;
        this.entityAttributeRepository = entityAttributeRepository;
        this.repositoryHolder = new RepositoryHolder(entityAttributeRepository, entityRelationRepository);

        // Strategy들을 computation_type을 키로 하는 Map으로 변환
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                FieldComputationStrategy::getSupportedComputationType,
                strategy -> strategy
            ));

        log.info("✅ DerivedFieldService 초기화 완료 - 등록된 전략: {}", strategies.keySet());
    }

    /**
     * 파생 필드 계산
     *
     * @param dataSourceId DataSource ID
     * @param rows mapped_data 행들
     */
    @Transactional(readOnly = true)
    public void computeFields(String dataSourceId, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            log.debug("No rows to process for derived fields");
            return;
        }

        // 1. derived_rules 조회 (캐싱됨)
        List<DerivedRuleEntity> rules = getActiveRules(dataSourceId);

        if (rules.isEmpty()) {
            log.debug("No active derived rules for DataSource: {}", dataSourceId);
            return;
        }

        log.info("Processing {} rows with {} derived rules", rows.size(), rules.size());

        // DEBUG: 첫 번째 행의 모든 키 출력
        if (!rows.isEmpty()) {
            Map<String, Object> firstRow = rows.get(0);
            log.info("🔍 첫 번째 행의 키: {}", firstRow.keySet());
            log.info("🔍 receiver_account 값: {}", firstRow.get("receiver_account"));
        }

        // 2. 각 행에 대해 파생 필드 계산
        int computedCount = 0;
        for (Map<String, Object> row : rows) {
            for (DerivedRuleEntity rule : rules) {
                try {
                    computeField(row, rule);
                    computedCount++;
                } catch (Exception e) {
                    log.error("Failed to compute derived field: {} for rule: {}",
                            rule.getTargetField(), rule.getRuleId(), e);
                }
            }
        }

        log.info("파생 필드 계산 완료: {} 건 처리됨", computedCount);
    }

    /**
     * 단일 행에 대한 파생 필드 계산 (Strategy Pattern)
     *
     * computation_type에 따라 적절한 Strategy를 선택하여 실행합니다.
     * 새로운 computation_type은 Strategy 구현체만 추가하면 자동으로 지원됩니다.
     */
    private void computeField(Map<String, Object> row, DerivedRuleEntity rule) {
        String computationType = rule.getComputationType();

        // 1. 해당 computation_type의 Strategy 조회
        FieldComputationStrategy strategy = strategies.get(computationType);

        if (strategy == null) {
            log.warn("⚠️ 지원하지 않는 computation_type: {} (rule_id: {})",
                computationType, rule.getRuleId());
            return;
        }

        // 2. Strategy 실행
        try {
            ComputationContext context = new ComputationContext(row, rule, repositoryHolder);
            Object result = strategy.compute(context);

            // 3. 결과를 row에 추가
            row.put(rule.getTargetField(), result);

            log.debug("✅ 파생 필드 계산 완료: {} = {} (computation_type: {}, rule_id: {})",
                rule.getTargetField(), result, computationType, rule.getRuleId());

        } catch (Exception e) {
            log.error("❌ 파생 필드 계산 실패: {} (computation_type: {}, rule_id: {})",
                rule.getTargetField(), computationType, rule.getRuleId(), e);
        }
    }

    /**
     * FIELD_COMPARISON 로직 구현
     */
    private Object computeFieldComparison(Map<String, Object> row, Map<String, Object> config) {
        String sourceField = (String) config.get("source_field");
        String logic = (String) config.get("logic");
        List<?> values = (List<?>) config.get("values");

        Object sourceValue = row.get(sourceField);
        if (sourceValue == null) {
            return null;
        }

        String sourceStr = sourceValue.toString();

        // NOT_STARTS_WITH 로직
        if ("NOT_STARTS_WITH".equals(logic)) {
            boolean result = true;
            for (Object value : values) {
                if (sourceStr.startsWith(value.toString())) {
                    result = false;
                    break;
                }
            }

            // AND 조건 처리
            if (result && config.containsKey("and")) {
                Map<String, Object> andConfig = (Map<String, Object>) config.get("and");
                String andField = (String) andConfig.get("source_field");
                String andLogic = (String) andConfig.get("logic");
                List<?> andValues = (List<?>) andConfig.get("values");

                Object andSourceValue = row.get(andField);
                if (andSourceValue != null) {
                    String andSourceStr = andSourceValue.toString();

                    // NOT_IN 로직
                    if ("NOT_IN".equals(andLogic)) {
                        for (Object andValue : andValues) {
                            if (andSourceStr.equals(andValue.toString())) {
                                result = false;
                                break;
                            }
                        }
                    }
                }
            }

            return result;
        }

        return null;
    }

    /**
     * OWNED_ACCOUNT_CHECK 로직 구현
     *
     * entity_attributes에서 owned_accounts를 조회하여
     * receiver_account가 본인 계좌인지 확인
     *
     * @param row 데이터 행
     * @param config 설정
     *   - source_field: 확인할 계좌번호 필드 (예: "receiver_account")
     *   - entity_type: 엔티티 타입 (예: "CUSTOMER")
     *   - entity_id_field: 엔티티 ID 필드 (예: "customer_id")
     * @return true (타인 계좌), false (본인 계좌), null (확인 불가)
     */
    private Object computeOwnedAccountCheck(Map<String, Object> row, Map<String, Object> config) {
        String sourceField = (String) config.get("source_field");
        String entityType = (String) config.get("entity_type");
        String entityIdField = (String) config.get("entity_id_field");

        // 필수 필드 확인
        if (sourceField == null || entityType == null || entityIdField == null) {
            log.warn("OWNED_ACCOUNT_CHECK: 필수 설정 누락 (source_field, entity_type, entity_id_field)");
            return null;
        }

        // receiver_account 값 추출
        Object receiverAccountValue = row.get(sourceField);
        if (receiverAccountValue == null) {
            return null; // receiver_account가 없으면 판단 불가
        }
        String receiverAccount = receiverAccountValue.toString();

        // customer_id 값 추출
        Object entityIdValue = row.get(entityIdField);
        if (entityIdValue == null) {
            log.warn("OWNED_ACCOUNT_CHECK: entity_id_field '{}' 값이 없음", entityIdField);
            return null;
        }
        String entityId = entityIdValue.toString();

        // entity_attributes에서 owned_accounts 조회
        try {
            Optional<Map<String, Object>> attributesOpt =
                entityAttributeRepository.findAttributesByEntity(entityType, entityId);

            if (attributesOpt.isEmpty()) {
                log.debug("OWNED_ACCOUNT_CHECK: 엔티티 속성 없음 ({}:{})", entityType, entityId);
                return null; // 속성이 없으면 판단 불가
            }

            Map<String, Object> attributes = attributesOpt.get();
            Object ownedAccountsObj = attributes.get("owned_accounts");

            if (ownedAccountsObj == null) {
                log.debug("OWNED_ACCOUNT_CHECK: owned_accounts 없음 ({}:{})", entityType, entityId);
                return true; // owned_accounts가 없으면 타인 계좌로 간주
            }

            // owned_accounts는 List<String>으로 저장됨
            if (ownedAccountsObj instanceof List) {
                List<?> ownedAccounts = (List<?>) ownedAccountsObj;
                boolean isOwnAccount = ownedAccounts.stream()
                    .map(Object::toString)
                    .anyMatch(account -> account.equals(receiverAccount));

                // true: 타인 계좌, false: 본인 계좌
                return !isOwnAccount;
            }

            log.warn("OWNED_ACCOUNT_CHECK: owned_accounts 타입 오류 ({})", ownedAccountsObj.getClass());
            return null;

        } catch (Exception e) {
            log.error("OWNED_ACCOUNT_CHECK 실행 중 오류 발생", e);
            return null;
        }
    }

    /**
     * DORMANT_ACCOUNT_CHECK 로직 구현
     *
     * entity_attributes에서 ACCOUNT의 last_transaction_date를 조회하여
     * 휴면 계좌 여부를 판단
     *
     * @param row 데이터 행
     * @param config 설정
     *   - receiver_field: 수취 계좌번호 필드 (예: "RECEIVER")
     *   - dormant_days: 휴면 기준 일수 (예: 365)
     * @return true (휴면 계좌), false (활동 계좌), null (확인 불가)
     */
    private Object computeDormantAccountCheck(Map<String, Object> row, Map<String, Object> config) {
        String receiverField = (String) config.get("receiver_field");
        Object dormantDaysObj = config.get("dormant_days");

        // 기본값: 365일
        int dormantDays = 365;
        if (dormantDaysObj instanceof Number) {
            dormantDays = ((Number) dormantDaysObj).intValue();
        }

        // 필수 필드 확인
        if (receiverField == null) {
            log.warn("DORMANT_ACCOUNT_CHECK: receiver_field 설정 누락");
            return null;
        }

        // RECEIVER 계좌번호 추출
        Object receiverValue = row.get(receiverField);
        if (receiverValue == null) {
            log.debug("DORMANT_ACCOUNT_CHECK: {} 필드 값이 없음", receiverField);
            return null;
        }
        String receiverAccount = receiverValue.toString();

        // ACCOUNT entity 조회
        try {
            Optional<Map<String, Object>> attributesOpt =
                entityAttributeRepository.findAttributesByEntity("ACCOUNT", receiverAccount);

            if (attributesOpt.isEmpty()) {
                log.debug("DORMANT_ACCOUNT_CHECK: ACCOUNT 엔티티 속성 없음 ({})", receiverAccount);
                return null; // 계좌 정보 없으면 판단 불가
            }

            Map<String, Object> attributes = attributesOpt.get();
            Object lastTxDateObj = attributes.get("last_transaction_date");

            if (lastTxDateObj == null) {
                log.debug("DORMANT_ACCOUNT_CHECK: last_transaction_date 없음 ({})", receiverAccount);
                // last_transaction_date가 없으면 신규 계좌이거나 데이터 없음
                // 보수적으로 휴면 아님으로 판단
                return false;
            }

            // 날짜 파싱 (다양한 형식 지원)
            LocalDate lastTxDate = parseDate(lastTxDateObj.toString());
            if (lastTxDate == null) {
                log.warn("DORMANT_ACCOUNT_CHECK: 날짜 파싱 실패 ({})", lastTxDateObj);
                return null;
            }

            // 현재 날짜와 비교
            LocalDate now = LocalDate.now();
            long daysSinceLastTx = ChronoUnit.DAYS.between(lastTxDate, now);

            boolean isDormant = daysSinceLastTx > dormantDays;

            log.info("DORMANT_ACCOUNT_CHECK: 계좌={}, 마지막거래={}, 경과일수={}, 기준일수={}, 휴면여부={}",
                receiverAccount, lastTxDate, daysSinceLastTx, dormantDays, isDormant);

            return isDormant;

        } catch (Exception e) {
            log.error("DORMANT_ACCOUNT_CHECK 실행 중 오류 발생: receiverAccount={}", receiverAccount, e);
            return null;
        }
    }

    /**
     * 날짜 문자열을 LocalDate로 파싱
     * 다양한 형식 지원: "2024-10-15", "2024-10-15 11:20:00", "20241015"
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        try {
            // ISO 형식: "2024-10-15" 또는 "2024-10-15 11:20:00"
            if (dateStr.contains("-")) {
                String datePart = dateStr.split(" ")[0]; // 시간 부분 제거
                return LocalDate.parse(datePart, DateTimeFormatter.ISO_LOCAL_DATE);
            }

            // "20241015" 형식
            if (dateStr.length() == 8 && dateStr.matches("\\d{8}")) {
                return LocalDate.parse(dateStr, DateTimeFormatter.BASIC_ISO_DATE);
            }

            // 기본 파싱 시도
            return LocalDate.parse(dateStr);

        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}", dateStr, e);
            return null;
        }
    }

    /**
     * DataSource별 활성화된 규칙 조회 (캐시)
     */
    @Cacheable(value = "derivedRules", key = "#dataSourceId")
    public List<DerivedRuleEntity> getActiveRules(String dataSourceId) {
        return derivedRuleRepository.findByDataSourceIdAndIsActiveTrueOrderByPriorityAsc(dataSourceId);
    }
}

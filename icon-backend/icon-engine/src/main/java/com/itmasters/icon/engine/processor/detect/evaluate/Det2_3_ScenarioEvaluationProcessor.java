package com.itmasters.icon.engine.processor.detect.evaluate;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectRuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectScenarioEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectActionEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineScenarioEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.RiskLevelEntity;
import com.itmasters.icon.common.domain.DomainEntityType;
import com.itmasters.icon.engine.adapter.out.persistence.repository.DetectRuleRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.DetectScenarioRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.DetectActionRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineScenarioRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityAttributeRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.MappedDataStorageRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.ScenarioRuleRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.RiskLevelRepository;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineScenarioRuleEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DET-2-3: 시나리오 평가 프로세서 (Scenario Evaluation)

 * 책임:
 * - detect_scenarios 테이블 적재
 * - detect_aggregates 기반 시나리오 매칭
 * - 복합 시나리오 지원: scenario_aggregates 기반 매칭
 * - 중복 억제: scenarios.dedup_minutes
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Det2_3_ScenarioEvaluationProcessor {

    private final DetectRuleRepository detectAggregateQueryRepository;
    private final EngineScenarioRepository engineScenarioRepository;
    private final DetectScenarioRepository detectScenarioRepository;
    private final DetectActionRepository detectActionRepository;
    private final EntityAttributeRepository entityAttributeRepository;
    private final ScenarioRuleRepository scenarioRuleRepository;
    private final MappedDataStorageRepository mappedDataStorageRepository;
    private final RiskLevelRepository riskLevelRepository;
    private final ObjectMapper objectMapper;

    /**
     * mappedStorageId 기반 시나리오 평가
     * - exec_ds_mp_id 대신 개별 mapped_storage_id 기준으로 처리
     * - MainEngineService에서 각 mappedStorageId별 루프 호출용
     *
     * @param mappedStorageId mapped_storages 테이블의 ID
     * @return 저장된 시나리오 건수
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int executeByMappedStorageId(Long mappedStorageId) {
        log.info("========== DET-2-3 시작 (시나리오 평가 - mappedStorageId 기반) ==========");
        log.info("mappedStorageId: {}", mappedStorageId);

        // 1. 현재 mappedStorageId의 detect_rules로부터 group_key 수집
        List<DetectRuleEntity> currentAggregates = detectAggregateQueryRepository.findByMappedStorageId(mappedStorageId);

        if (currentAggregates == null || currentAggregates.isEmpty()) {
            log.debug("해당 mappedStorageId에 대한 detect_rules가 없음 - mappedStorageId: {}", mappedStorageId);
            return 0;
        }

        Set<String> groupKeys = currentAggregates.stream()
                .map(DetectRuleEntity::getGroupKey)
                .collect(Collectors.toSet());

        log.info("DET-2-3 대상 group_key {} 개: {}", groupKeys.size(), groupKeys);

        // 2. 해당 group_key들의 모든 활성 aggregate 조회
        List<DetectRuleEntity> allActiveAggregates =
            detectAggregateQueryRepository.findActiveAggregatesByGroupKeys(groupKeys);

        log.info("활성 aggregate {} 개 조회됨", allActiveAggregates.size());

        // 3. group_key별로 aggregate 그룹화
        Map<String, List<DetectRuleEntity>> aggregatesByGroupKey = allActiveAggregates.stream()
                .collect(Collectors.groupingBy(DetectRuleEntity::getGroupKey));

        // 4. 모든 활성 시나리오 조회
        List<EngineScenarioEntity> scenarios = engineScenarioRepository.findActiveScenarios();
        log.info("활성 시나리오 {} 개 조회됨", scenarios.size());

        // 5. 각 group_key + 시나리오 조합 평가
        int saved = 0;
        for (String groupKey : groupKeys) {
            List<DetectRuleEntity> groupAggregates = aggregatesByGroupKey.get(groupKey);
            if (groupAggregates == null || groupAggregates.isEmpty()) {
                continue;
            }

            for (EngineScenarioEntity scn : scenarios) {
                try {
                    saved += evaluateScenarioByMappedStorageId(scn, groupKey, groupAggregates, mappedStorageId);
                } catch (Exception e) {
                    log.error("시나리오 평가 실패 - scenarioId={}, groupKey={}, mappedStorageId={}",
                            scn.getScenarioId(), groupKey, mappedStorageId, e);
                }
            }
        }

        log.info("========== DET-2-3 완료 (시나리오 평가 - mappedStorageId 기반) - 저장: {} 건 ==========", saved);
        return saved;
    }


    /**
     * 단일 시나리오 평가 (mappedStorageId 기반)
     */
    private int evaluateScenarioByMappedStorageId(EngineScenarioEntity scn, String groupKey,
                                                   List<DetectRuleEntity> groupAggregates, Long mappedStorageId) {
        String scenarioId = scn.getScenarioId();

        // 1. 시나리오 집계 목록 조회
        List<EngineScenarioRuleEntity> scenarioAggregates =
            scenarioRuleRepository.findByScenarioIdOrderByOrderNo(scenarioId);

        if (scenarioAggregates == null || scenarioAggregates.isEmpty()) {
            log.debug("시나리오 집계 없음 - scenarioId: {}", scenarioId);
            return 0;
        }

        // 2. 필요한 aggregate ID 목록 추출
        List<String> requiredAggregateIds = scenarioAggregates.stream()
                .map(EngineScenarioRuleEntity::getRuleId)
                .collect(Collectors.toList());

        if (requiredAggregateIds.isEmpty()) {
            log.debug("필요한 aggregate가 없는 시나리오 - scenarioId: {}", scenarioId);
            return 0;
        }

        // 3. 필요한 aggregate가 모두 존재하는지 확인
        Set<String> availableAggregateIds = groupAggregates.stream()
                .map(DetectRuleEntity::getRuleId)
                .collect(Collectors.toSet());

        boolean allPresent = requiredAggregateIds.stream()
                .allMatch(availableAggregateIds::contains);

        if (!allPresent) {
            log.debug("필요한 aggregate 미충족 - scenarioId={}, required={}, available={}",
                     scenarioId, requiredAggregateIds, availableAggregateIds);
            return 0;
        }

        // 4. 매칭된 aggregate 조회
        List<DetectRuleEntity> matchedAggregates = groupAggregates.stream()
                .filter(agg -> requiredAggregateIds.contains(agg.getRuleId()))
                .collect(Collectors.toList());

        // 5. 현재 mappedStorageId에서 새로 생성된 aggregate가 있는지 확인
        boolean hasNewAggregate = matchedAggregates.stream()
                .anyMatch(agg -> mappedStorageId.equals(agg.getMappedStorageId()));

        if (!hasNewAggregate) {
            log.debug("현재 mappedStorageId에서 새로 생성된 aggregate가 없음 - 스킵 - scenarioId={}, groupKey={}, mappedStorageId={}",
                     scenarioId, groupKey, mappedStorageId);
            return 0;
        }

        // 6. entity_filter_json 체크
        if (scn.getEntityFilterJson() != null && !scn.getEntityFilterJson().isBlank()) {
            boolean entityFilterPassed = evaluateEntityFilter(scn.getEntityFilterJson(), groupKey);
            if (!entityFilterPassed) {
                log.debug("Entity filter 미통과 - scenarioId={}, groupKey={}", scenarioId, groupKey);
                return 0;
            }
        }

        // 7. 중복 체크 (dedup_minutes 기반)
        LocalDateTime detectedAt = LocalDateTime.now();
        int dedupMinutes = scn.getDedupMinutes() != null ? scn.getDedupMinutes() : 1440;
        LocalDateTime since = detectedAt.minusMinutes(dedupMinutes);

        if (detectScenarioRepository.existsRecentByKeyAndScenario(groupKey, scenarioId, since)) {
            log.debug("시나리오 중복 억제 - scenarioId={}, groupKey={}, dedupMinutes={}, since={}",
                     scenarioId, groupKey, dedupMinutes, since);
            return 0;
        }


        String transactionId = matchedAggregates.stream()
                .filter(agg -> mappedStorageId.equals(agg.getMappedStorageId()))
                .filter(agg -> agg.getTransactionId() != null)
                .map(DetectRuleEntity::getTransactionId)
                .findFirst()
                .orElse(null);

        LocalDateTime anchorEventDt = matchedAggregates.stream()
                .filter(agg -> mappedStorageId.equals(agg.getMappedStorageId()))
                .filter(agg -> agg.getEventDt() != null)
                .map(DetectRuleEntity::getEventDt)
                .findFirst()
                .orElse(detectedAt);

        DetectScenarioEntity row = DetectScenarioEntity.builder()
                .scenarioId(scenarioId)
                .groupKey(groupKey)
                .detectedDt(detectedAt)
                .eventDt(anchorEventDt)
                .windowStart(null)
                .windowEnd(detectedAt)
                .mappedStorageId(mappedStorageId)
                .transactionId(transactionId)
                .build();
        DetectScenarioEntity savedScenario = detectScenarioRepository.save(row);

        log.info("✅ 시나리오 탐지 (mappedStorageId 기반) - scenarioId={}, groupKey={}, mappedStorageId={}, aggregates={}",
                 scenarioId, groupKey, mappedStorageId, requiredAggregateIds);

        // 9. risk_level 기반 자동 조치 생성 (BLOCK, HOLD인 경우)
        if (scn.getRiskLevelId() != null) {
            try {
                Optional<RiskLevelEntity> riskLevelOpt = riskLevelRepository.findByRiskLevelId(scn.getRiskLevelId());
                if (riskLevelOpt.isPresent()) {
                    RiskLevelEntity riskLevel = riskLevelOpt.get();
                    String actionType = riskLevel.getActionType();

                    if ("BLOCK".equals(actionType) || "HOLD".equals(actionType)) {
                        DetectActionEntity action = DetectActionEntity.builder()
                                .detectScenarioId(savedScenario.getDetectScenarioId())
                                .scenarioId(scenarioId)
                                .groupKey(groupKey)
                                .actionType(DetectActionEntity.ActionType.valueOf(actionType))
                                .actionStatus(DetectActionEntity.ActionStatus.PENDING)
                                .riskLevel(riskLevel.getRiskLevelId())
                                .requestedBy("SYSTEM")
                                .requestedAt(LocalDateTime.now())
                                .build();
                        detectActionRepository.save(action);

                        log.info("✅ 조치 자동 생성 - scenarioId={}, groupKey={}, actionType={}, riskLevel={}",
                                 scenarioId, groupKey, actionType, riskLevel.getRiskLevelId());
                    }
                }
            } catch (Exception e) {
                log.error("조치 생성 실패 - scenarioId={}, groupKey={}, riskLevelId={}",
                         scenarioId, groupKey, scn.getRiskLevelId(), e);
                // 조치 생성 실패해도 시나리오는 저장되었으므로 계속 진행
            }
        }

        return 1;
    }


    private String toScenarioId(String ruleId) {
        if (ruleId == null) return null;
        String suffix = ruleId.startsWith("AGG_") ? ruleId.substring(4) : ruleId;
        return "S_" + suffix;
    }

    /**
     * entity_filter_json 평가
     *
     * @param entityFilterJson JSON 배열 형식의 필터 조건 (예: [{"fieldName":"customer_age","operator":"GREATER_THAN_OR_EQUALS","value":65}])
     * @param groupKey 그룹 키 (예: CUS_TEST_001)
     * @return 필터 통과 여부
     */
    private boolean evaluateEntityFilter(String entityFilterJson, String groupKey) {
        try {
            log.info("===== Entity Filter 평가 시작 - groupKey={} =====", groupKey);

            // 1. groupKey와 연결된 모든 entity_type의 attributes 조회
            Map<String, Object> allAttributes = new HashMap<>();

            // CUSTOMER 엔티티 조회
            Optional<Map<String, Object>> customerAttrs =
                entityAttributeRepository.findAttributesByEntity(DomainEntityType.CUSTOMER.name(), groupKey);
            if (customerAttrs.isPresent()) {
                log.info("{} 엔티티 발견 - attributes: {}", DomainEntityType.CUSTOMER, customerAttrs.get());
                allAttributes.putAll(customerAttrs.get());
            } else {
                log.info("{} 엔티티 없음", DomainEntityType.CUSTOMER);
            }

            // ACCOUNT 엔티티 조회 (customer_id로 연결된 계좌)
            // groupKey가 고객 ID이므로, customer_id = groupKey인 ACCOUNT를 찾음
            Optional<Map<String, Object>> accountAttrs =
                entityAttributeRepository.findAttributesByEntityTypeAndCustomerId(DomainEntityType.ACCOUNT.name(), groupKey);
            if (accountAttrs.isPresent()) {
                log.info("{} 엔티티 발견 - attributes: {}", DomainEntityType.ACCOUNT, accountAttrs.get());
                allAttributes.putAll(accountAttrs.get());
            } else {
                log.info("{} 엔티티 없음", DomainEntityType.ACCOUNT);
            }

            // DEVICE 엔티티도 필요하면 추가 가능

            log.info("통합 attributes: {}", allAttributes);

            if (allAttributes.isEmpty()) {
                log.warn("Entity attributes 없음 - groupKey={}", groupKey);
                return false;
            }

            Map<String, Object> attributes = allAttributes;

            // 3. JSON 파싱 (배열 형식)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> filters = objectMapper.readValue(entityFilterJson, List.class);

            // 4. 모든 필터 조건 평가 (AND 조건)
            for (Map<String, Object> filter : filters) {
                String fieldName = (String) filter.get("fieldName");
                String operator = (String) filter.get("operator");
                Object value = filter.get("value");

                // 필드가 attributes에 없으면 실패
                if (!attributes.containsKey(fieldName)) {
                    log.debug("Entity attribute 필드 없음 - fieldName={}, groupKey={}", fieldName, groupKey);
                    return false;
                }

                Object actualValue = attributes.get(fieldName);

                // 조건 평가
                boolean passed = evaluateCondition(fieldName, operator, value, actualValue);
                if (!passed) {
                    log.debug("Entity filter 조건 실패 - field={}, operator={}, expected={}, actual={}",
                             fieldName, operator, value, actualValue);
                    return false;
                }
            }

            log.debug("Entity filter 모든 조건 통과 - groupKey={}", groupKey);
            return true;

        } catch (Exception e) {
            log.error("Entity filter 평가 실패 - groupKey={}, error={}", groupKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 단일 조건 평가
     */
    private boolean evaluateCondition(String fieldName, String operator, Object expectedValue, Object actualValue) {
        if (actualValue == null) {
            return false;
        }

        switch (operator) {
            case "EQUALS":
            case "EQ":
                return actualValue.equals(expectedValue);

            case "GREATER_THAN_OR_EQUALS":
            case "GTE":
            case "GE":
                return compareNumeric(actualValue, expectedValue) >= 0;

            case "LESS_THAN_OR_EQUALS":
            case "LTE":
            case "LE":
                return compareNumeric(actualValue, expectedValue) <= 0;

            default:
                log.warn("지원하지 않는 operator: {}", operator);
                return false;
        }
    }

    /**
     * 숫자 비교
     */
    private int compareNumeric(Object actual, Object expected) {
        java.math.BigDecimal actualNum = toBigDecimal(actual);
        java.math.BigDecimal expectedNum = toBigDecimal(expected);

        if (actualNum == null || expectedNum == null) {
            throw new IllegalArgumentException("숫자 변환 실패: actual=" + actual + ", expected=" + expected);
        }

        return actualNum.compareTo(expectedNum);
    }

    /**
     * BigDecimal 변환
     */
    private java.math.BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;

        try {
            if (value instanceof Number) {
                return new java.math.BigDecimal(value.toString());
            } else if (value instanceof String) {
                return new java.math.BigDecimal((String) value);
            }
        } catch (NumberFormatException e) {
            log.warn("숫자 변환 실패: value={}", value, e);
        }

        return null;
    }

    /**
     * groupKey에서 entityType 추출
     */
    private String extractEntityType(String groupKey) {
        if (groupKey == null) {
            return DomainEntityType.CUSTOMER.name();
        }

        String upper = groupKey.toUpperCase();

        if (upper.startsWith("CUS")) {
            return DomainEntityType.CUSTOMER.name();
        } else if (upper.startsWith("ACC")) {
            return DomainEntityType.ACCOUNT.name();
        } else if (upper.startsWith("DEV")) {
            return DomainEntityType.DEVICE.name();
        }

        return DomainEntityType.CUSTOMER.name(); // 기본값
    }
}


package com.itmasters.icon.api.derivedfield.application.service;

import com.itmasters.icon.api.derivedfield.adapter.persistence.DerivedRuleRepository;
import com.itmasters.icon.entity.DerivedRuleEntity;
import com.itmasters.icon.api.derivedfield.application.strategy.FieldComputationStrategy;
import com.itmasters.icon.api.derivedfield.application.strategy.context.ComputationContext;
import com.itmasters.icon.api.derivedfield.application.strategy.context.RepositoryHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 파생 필드 계산 서비스
 *
 * mapped_data에 파생 필드를 추가합니다.
 * 처리 시점: Profile 매핑 완료 후, event_stream 저장 전
 */
@Slf4j
@Service("apiDerivedFieldService")
@RequiredArgsConstructor
public class DerivedFieldService {

    private final DerivedRuleRepository derivedRuleRepository;
    private final Map<String, FieldComputationStrategy> strategies;
    private final RepositoryHolder repositoryHolder;

    /**
     * 파생 필드 계산 (배치 처리)
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

        log.info("Computing derived fields for {} rows with {} rules", rows.size(), rules.size());

        // 2. 각 row에 규칙 적용
        for (Map<String, Object> row : rows) {
            for (DerivedRuleEntity rule : rules) {
                computeAndSetField(row, rule);
            }
        }

        log.debug("Derived fields computation completed for DataSource: {}", dataSourceId);
    }

    /**
     * 단일 필드 계산 및 설정
     */
    private void computeAndSetField(Map<String, Object> row, DerivedRuleEntity rule) {
        try {
            // 1. computation_type에 맞는 Strategy 선택
            String computationType = rule.getComputationType();
            FieldComputationStrategy strategy = strategies.get(computationType);

            if (strategy == null) {
                log.warn("No strategy found for computation_type: {}", computationType);
                return;
            }

            // 2. ComputationContext 생성
            ComputationContext context = new ComputationContext(row, rule, repositoryHolder);

            // 3. 파생 필드 계산
            Object computedValue = strategy.compute(context);

            // 4. row에 추가
            row.put(rule.getTargetField(), computedValue);

            log.debug("Computed field '{}' = {} for rule_id: {}",
                     rule.getTargetField(), computedValue, rule.getRuleId());

        } catch (Exception e) {
            log.error("Failed to compute field '{}' for rule_id: {}",
                     rule.getTargetField(), rule.getRuleId(), e);
            // 에러가 나도 다른 필드 계산은 계속 진행
        }
    }

    /**
     * DataSource별 활성 규칙 조회 (캐싱)
     *
     * @param dataSourceId DataSource ID
     * @return 활성화된 파생 필드 규칙 목록
     */
    @Cacheable(value = "derivedRules", key = "#dataSourceId")
    public List<DerivedRuleEntity> getActiveRules(String dataSourceId) {
        return derivedRuleRepository.findActiveRulesByDataSourceId(dataSourceId);
    }

    /**
     * 캐시 무효화 (규칙 변경 시 호출)
     */
    @org.springframework.cache.annotation.CacheEvict(value = "derivedRules", key = "#dataSourceId")
    public void evictCache(String dataSourceId) {
        log.info("Evicted derived rules cache for DataSource: {}", dataSourceId);
    }
}

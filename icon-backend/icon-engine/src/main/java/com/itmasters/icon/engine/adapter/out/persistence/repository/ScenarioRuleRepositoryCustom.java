package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineScenarioRuleEntity;

import java.util.List;

/**
 * scenario_rules 커스텀 Repository 인터페이스
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
public interface ScenarioRuleRepositoryCustom {

    /**
     * 시나리오 ID로 룰 매핑 조회 (order_no 순서대로)
     */
    List<EngineScenarioRuleEntity> findByScenarioIdOrderByOrderNo(String scenarioId);

    /**
     * 특정 룰이 포함된 시나리오 룰 조회
     */
    List<EngineScenarioRuleEntity> findByRuleId(String ruleId);

    /**
     * 시나리오 ID 목록에 해당하는 모든 룰 조회
     */
    List<EngineScenarioRuleEntity> findByScenarioIdIn(List<String> scenarioIds);
}

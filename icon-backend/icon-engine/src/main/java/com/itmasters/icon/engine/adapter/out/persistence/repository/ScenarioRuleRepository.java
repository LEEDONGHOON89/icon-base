package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineScenarioRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * scenario_rules 테이블 리포지토리
 * QueryDSL 기반 복잡한 쿼리는 ScenarioRuleRepositoryCustom에서 구현
 */
@Repository
public interface ScenarioRuleRepository extends JpaRepository<EngineScenarioRuleEntity, Long>, ScenarioRuleRepositoryCustom {

    /**
     * 시나리오 ID로 룰 매핑 조회 (order_no 순서대로) - QueryDSL로 구현
     */
    // @Query 제거됨 - ScenarioRuleRepositoryCustom 인터페이스에서 선언, ScenarioRuleRepositoryImpl에서 구현
    // List<EngineScenarioRuleEntity> findByScenarioIdOrderByOrderNo(String scenarioId);

    /**
     * 시나리오 ID와 집계 ID로 조회
     */
    EngineScenarioRuleEntity findByScenarioIdAndRuleId(String scenarioId, String ruleId);
}

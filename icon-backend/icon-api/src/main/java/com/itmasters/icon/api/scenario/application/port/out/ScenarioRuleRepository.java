package com.itmasters.icon.api.scenario.application.port.out;

import com.itmasters.icon.api.scenario.domain.ScenarioRule;

import java.util.List;
/**
 * 시나리오-규칙 매핑 Repository 인터페이스
 */
public interface ScenarioRuleRepository {
    
    /**
     * 시나리오-규칙 매핑 저장
     */
    ScenarioRule save(ScenarioRule scenarioRule);
    
    /**
     * 시나리오별 규칙 매핑 조회 (순서별 정렬)
     */
    List<ScenarioRule> findByScenarioIdOrderByOrderNo(String scenarioId);
    
    /**
     * 규칙별 시나리오 매핑 조회
     */
    List<ScenarioRule> findByRuleId(String ruleId);
    
    /**
     * 시나리오별 규칙 매핑 삭제
     */
    void deleteByScenarioId(String scenarioId);
    
    /**
     * 규칙별 시나리오 매핑 삭제
     */
    void deleteByRuleId(String ruleId);
    
    /**
     * 특정 시나리오-규칙 매핑 삭제
     */
    void deleteByScenarioIdAndRuleId(String scenarioId, String ruleId);
    
    /**
     * 시나리오-규칙 매핑 존재 여부 확인
     */
    boolean existsByScenarioIdAndRuleId(String scenarioId, String ruleId);
    
    /**
     * 시나리오별 규칙 매핑 일괄 저장
     */
    List<ScenarioRule> saveAll(List<ScenarioRule> scenarioRules);
}
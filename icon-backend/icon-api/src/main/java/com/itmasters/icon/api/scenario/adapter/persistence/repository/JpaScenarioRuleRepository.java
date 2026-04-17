package com.itmasters.icon.api.scenario.adapter.persistence.repository;

import com.itmasters.icon.api.scenario.adapter.persistence.entity.ScenarioRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 시나리오-규칙 매핑 JPA Repository
 */
@Repository
public interface JpaScenarioRuleRepository extends JpaRepository<ScenarioRuleEntity, Long> {
    
    // @Query 메서드들은 ScenarioRuleRepositoryImpl에서 QueryDSL로 구현됨
}
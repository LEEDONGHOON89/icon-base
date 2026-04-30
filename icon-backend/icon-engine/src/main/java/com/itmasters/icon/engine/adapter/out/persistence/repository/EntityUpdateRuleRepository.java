package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.entity.EntityUpdateRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Entity Update Rule Repository

 * 시나리오 탐지 시 entity_attributes를 업데이트하는 규칙을 조회합니다.
 * QueryDSL 기반 복잡한 쿼리는 EntityUpdateRuleRepositoryCustom에서 구현
 */
@Repository
public interface EntityUpdateRuleRepository extends JpaRepository<EntityUpdateRuleEntity, String>, EntityUpdateRuleRepositoryCustom {

    /**
     * 특정 시나리오 ID에 대한 활성 업데이트 규칙 조회 - QueryDSL로 구현
     */
    // @Query 제거됨 - EntityUpdateRuleRepositoryCustom 인터페이스에서 선언, EntityUpdateRuleRepositoryImpl에서 구현
    // List<EntityUpdateRuleEntity> findActiveRulesByScenarioId(String scenarioId);

    /**
     * 모든 활성 업데이트 규칙 조회 - QueryDSL로 구현
     */
    // @Query 제거됨 - EntityUpdateRuleRepositoryCustom 인터페이스에서 선언, EntityUpdateRuleRepositoryImpl에서 구현
    // List<EntityUpdateRuleEntity> findAllActiveRules();
}

package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Rule Repository
 *
 * QueryDSL 기반 복잡한 쿼리는 JpaRuleRepositoryCustom에서 구현
 */
public interface JpaRuleRepository extends JpaRepository<RuleEntity, String>, JpaRuleRepositoryCustom {

    /**
     * 활성 상태인 Rule 목록 조회
     *
     * @return 활성 Rule 목록
     */
    List<RuleEntity> findByIsActiveTrue();

    /**
     * 활성 상태이면서 지정된 ruleId가 4개의 센서 필드 중 하나라도 포함된 Rule 조회 - QueryDSL로 구현
     */
    // @Query 제거됨 - JpaRuleRepositoryCustom 인터페이스에서 선언, JpaRuleRepositoryImpl에서 구현
    // List<RuleEntity> findActiveByRuleIds(Collection<String> ruleIds);
}

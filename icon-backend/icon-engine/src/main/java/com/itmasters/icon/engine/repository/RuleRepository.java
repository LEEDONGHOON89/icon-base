package com.itmasters.icon.engine.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity;

import java.util.List;

/**
 * 집계 정의(RuleEntity)에 접근하기 위한 Repository 인터페이스
 */
public interface RuleRepository {

    /**
     * 활성화된 모든 집계 정의를 조회합니다.
     * @return 활성 상태인 RuleEntity 목록
     */
    List<RuleEntity> findAllActive();

    /**
     * 지정한 룰 ID들과 연관된 활성 집계 정의를 조회합니다.
     */
    List<RuleEntity> findActiveByRuleIds(java.util.Collection<String> ruleIds);
}

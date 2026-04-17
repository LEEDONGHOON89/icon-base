package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity;

import java.util.Collection;
import java.util.List;

/**
 * Rule Repository 커스텀 인터페이스
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
public interface JpaRuleRepositoryCustom {

    /**
     * 활성 상태이면서 지정된 ruleId가 4개의 센서 필드 중 하나라도 포함된 Rule 조회
     *
     * @param ruleIds 검색할 rule ID 목록
     * @return 조건에 맞는 Rule 목록
     */
    List<RuleEntity> findActiveByRuleIds(Collection<String> ruleIds);
}

package com.itmasters.icon.api.derivedfield.adapter.persistence;

import com.itmasters.icon.entity.DerivedRuleEntity;

import java.util.List;

/**
 * 파생 필드 규칙 커스텀 Repository 인터페이스
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
public interface DerivedRuleRepositoryCustom {

    /**
     * DataSource별 활성 규칙 조회 (우선순위 순)
     *
     * @param dataSourceId DataSource ID
     * @return 활성화된 파생 필드 규칙 목록 (우선순위 오름차순)
     */
    List<DerivedRuleEntity> findActiveRulesByDataSourceId(String dataSourceId);
}

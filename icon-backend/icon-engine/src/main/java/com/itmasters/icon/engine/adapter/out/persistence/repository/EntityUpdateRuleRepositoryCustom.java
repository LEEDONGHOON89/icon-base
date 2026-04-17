package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.entity.EntityUpdateRuleEntity;

import java.util.List;

/**
 * Entity Update Rule 커스텀 Repository 인터페이스
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
public interface EntityUpdateRuleRepositoryCustom {

    /**
     * 특정 시나리오 ID에 대한 활성 업데이트 규칙 조회
     *
     * @param scenarioId 시나리오 ID
     * @return 업데이트 규칙 목록
     */
    List<EntityUpdateRuleEntity> findActiveRulesByScenarioId(String scenarioId);

    /**
     * 모든 활성 업데이트 규칙 조회
     *
     * @return 활성 업데이트 규칙 목록
     */
    List<EntityUpdateRuleEntity> findAllActiveRules();

    /**
     * 특정 데이터소스 ID에 대한 활성 이벤트 기반 업데이트 규칙 조회
     *
     * @param dataSourceId 데이터소스 ID
     * @return 이벤트 기반 업데이트 규칙 목록
     */
    List<EntityUpdateRuleEntity> findActiveRulesByDataSourceId(String dataSourceId);

    /**
     * 모든 활성 이벤트 기반 업데이트 규칙 조회
     *
     * @return 활성 이벤트 기반 업데이트 규칙 목록
     */
    List<EntityUpdateRuleEntity> findAllActiveEventRules();
}

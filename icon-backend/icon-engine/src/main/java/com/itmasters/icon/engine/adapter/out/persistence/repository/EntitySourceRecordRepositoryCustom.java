package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EntitySourceRecordEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 엔티티 원본 데이터 이력 커스텀 Repository 인터페이스
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
public interface EntitySourceRecordRepositoryCustom {

    /**
     * 특정 엔티티의 특정 시점 이전 데이터 조회 (시점 복원용)
     */
    List<EntitySourceRecordEntity> findByEntityTypeAndEntityIdAsOf(
            String entityType,
            String entityId,
            LocalDateTime asOfTime
    );

    /**
     * 특정 DataSource의 최근 데이터 조회
     */
    List<EntitySourceRecordEntity> findRecentByDataSourceId(String dataSourceId, int limit);
}

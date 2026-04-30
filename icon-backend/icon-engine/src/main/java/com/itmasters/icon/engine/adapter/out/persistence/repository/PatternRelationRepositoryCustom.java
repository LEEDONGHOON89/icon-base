package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.PatternRelationEntity;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pattern Relations 커스텀 Repository 인터페이스
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
public interface PatternRelationRepositoryCustom {

    /**
     * 최소 신뢰도 이상 패턴 조회
     */
    List<PatternRelationEntity> findHighConfidencePatterns(
            String approvalStatus,
            BigDecimal minConfidence
    );

    /**
     * 최근 발견된 패턴 조회 (N개)
     */
    List<PatternRelationEntity> findRecentPendingPatterns(int limit);

    /**
     * 통계: DataSource별 패턴 상태별 개수
     */
    List<Object[]> countByApprovalStatus(String dataSourceId);
}

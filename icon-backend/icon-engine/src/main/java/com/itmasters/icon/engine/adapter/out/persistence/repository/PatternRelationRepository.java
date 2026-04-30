package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.PatternRelationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Pattern Relations Repository
 *
 * 자동 발견된 관계 패턴 관리
 *
 * 주요 쿼리 패턴:

   * -검토 대기 중인 패턴 조회
   * -신뢰도 기준 필터링
   * -승인된 패턴 조회 (entity_relation_rules 승격용)

 *
 * QueryDSL 기반 복잡한 쿼리는 PatternRelationRepositoryCustom에서 구현
 *
 * @since 2025-02-10
 */
@Repository
public interface PatternRelationRepository extends JpaRepository<PatternRelationEntity, Long>, PatternRelationRepositoryCustom {

    /**
     * 검토 대기 중인 패턴 조회 (신뢰도 높은 순)
     */
    List<PatternRelationEntity> findByApprovalStatusOrderByConfidenceScoreDesc(String approvalStatus);

    /**
     * DataSource별 검토 대기 패턴 조회
     */
    List<PatternRelationEntity> findByDataSourceIdAndApprovalStatusOrderByConfidenceScoreDesc(
            String dataSourceId,
            String approvalStatus
    );

    /**
     * 최소 신뢰도 이상 패턴 조회 - QueryDSL로 구현
     */
    // @Query 제거됨 - PatternRelationRepositoryCustom 인터페이스에서 선언, PatternRelationRepositoryImpl에서 구현
    // List<PatternRelationEntity> findHighConfidencePatterns(String approvalStatus, BigDecimal minConfidence);

    /**
     * 특정 패턴 존재 확인 (중복 방지용)
     */
    Optional<PatternRelationEntity> findByDataSourceIdAndFromEntityTypeAndFromIdFieldAndRelationTypeAndToEntityTypeAndToIdField(
            String dataSourceId,
            String fromEntityType,
            String fromIdField,
            String relationType,
            String toEntityType,
            String toIdField
    );

    /**
     * 승인된 패턴 목록 조회 (entity_relation_rules 승격용)
     */
    List<PatternRelationEntity> findByApprovalStatus(String approvalStatus);

    /**
     * DataSource별 모든 패턴 조회
     */
    List<PatternRelationEntity> findByDataSourceIdOrderByConfidenceScoreDesc(String dataSourceId);

    /**
     * 탐지 방법별 패턴 조회
     */
    List<PatternRelationEntity> findByDetectionMethodAndApprovalStatus(
            String detectionMethod,
            String approvalStatus
    );

    /**
     * 최근 발견된 패턴 조회 (N개) - QueryDSL로 구현
     */
    // @Query 제거됨 - PatternRelationRepositoryCustom 인터페이스에서 선언, PatternRelationRepositoryImpl에서 구현
    // List<PatternRelationEntity> findRecentPendingPatterns(int limit);

    /**
     * 통계: DataSource별 패턴 상태별 개수 - QueryDSL로 구현
     */
    // @Query 제거됨 - PatternRelationRepositoryCustom 인터페이스에서 선언, PatternRelationRepositoryImpl에서 구현
    // List<Object[]> countByApprovalStatus(String dataSourceId);
}

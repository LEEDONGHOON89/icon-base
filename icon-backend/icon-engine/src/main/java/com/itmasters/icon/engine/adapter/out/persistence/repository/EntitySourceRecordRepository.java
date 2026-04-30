package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EntitySourceRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 엔티티 원본 데이터 이력 Repository
 *
 * 주요 기능:
 * - 엔티티별 이력 조회
 * - 특정 시점 데이터 조회 (시점 복원)
 * - DataSource별 조회
 *
 * QueryDSL 기반 복잡한 쿼리는 EntitySourceRecordRepositoryCustom에서 구현
 *
 * @since 2025-12-22
 */
@Repository
public interface EntitySourceRecordRepository extends JpaRepository<EntitySourceRecordEntity, Long>, EntitySourceRecordRepositoryCustom {

    /**
     * 특정 엔티티의 모든 이력 조회 (최신순)
     *
     * @param entityType 엔티티 타입
     * @param entityId 엔티티 ID
     * @return 이력 목록
     */
    List<EntitySourceRecordEntity> findByEntityTypeAndEntityIdOrderByReceivedAtDesc(
            String entityType,
            String entityId
    );

    /**
     * 특정 엔티티의 특정 시점 이전 데이터 조회 (시점 복원용) - QueryDSL로 구현
     */
    // @Query 제거됨 - EntitySourceRecordRepositoryCustom 인터페이스에서 선언, EntitySourceRecordRepositoryImpl에서 구현
    // List<EntitySourceRecordEntity> findByEntityTypeAndEntityIdAsOf(String entityType, String entityId, LocalDateTime asOfTime);

    /**
     * 특정 DataSource의 최근 데이터 조회 - QueryDSL로 구현
     */
    // @Query 제거됨 - EntitySourceRecordRepositoryCustom 인터페이스에서 선언, EntitySourceRecordRepositoryImpl에서 구현
    // List<EntitySourceRecordEntity> findRecentByDataSourceId(String dataSourceId, int limit);

    /**
     * 특정 트랜잭션 ID로 조회
     *
     * @param sourceTxId 원본 트랜잭션 ID
     * @return 해당 트랜잭션 레코드
     */
    List<EntitySourceRecordEntity> findBySourceTxId(String sourceTxId);

    /**
     * 특정 배치의 모든 데이터 조회
     *
     * @param processingBatchId 배치 ID
     * @return 배치 데이터 목록
     */
    List<EntitySourceRecordEntity> findByProcessingBatchIdOrderByRecordId(String processingBatchId);

    /**
     * 특정 기간 동안의 데이터 조회
     *
     * @param startTime 시작 시각
     * @param endTime 종료 시각
     * @return 기간 내 데이터 목록
     */
    List<EntitySourceRecordEntity> findByReceivedAtBetweenOrderByReceivedAtDesc(
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * 특정 엔티티 타입의 전체 개수 조회
     *
     * @param entityType 엔티티 타입
     * @return 개수
     */
    long countByEntityType(String entityType);

    /**
     * 특정 DataSource의 전체 개수 조회
     *
     * @param dataSourceId DataSource ID
     * @return 개수
     */
    long countByDataSourceId(String dataSourceId);
}

package com.itmasters.icon.api.audit.adapter.out.persistence;

import com.itmasters.icon.api.audit.domain.ConfigAuditTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Detection Config Audit Repository
 *
 * QueryDSL 기반 복잡한 쿼리는 DetectionConfigAuditRepositoryCustom에서 구현
 */
@Repository
public interface DetectionConfigAuditRepository extends JpaRepository<DetectionConfigAuditEntity, Long>, DetectionConfigAuditRepositoryCustom {

    /**
     * 특정 대상의 변경 이력 조회
     */
    List<DetectionConfigAuditEntity> findByTargetTypeAndTargetIdOrderByChangedAtDesc(
            ConfigAuditTargetType targetType, String targetId);

    /**
     * 특정 대상의 변경 이력 조회 (페이징)
     */
    Page<DetectionConfigAuditEntity> findByTargetTypeAndTargetIdOrderByChangedAtDesc(
            ConfigAuditTargetType targetType, String targetId, Pageable pageable);

    /**
     * 특정 사용자의 변경 이력 조회
     */
    Page<DetectionConfigAuditEntity> findByChangedByOrderByChangedAtDesc(
            String changedBy, Pageable pageable);

    /**
     * 기간별 변경 이력 조회 - QueryDSL로 구현
     */
    // @Query 제거됨 - DetectionConfigAuditRepositoryCustom 인터페이스에서 선언, DetectionConfigAuditRepositoryImpl에서 구현
    // Page<DetectionConfigAuditEntity> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * 타입별 + 기간별 변경 이력 조회 - QueryDSL로 구현
     */
    // @Query 제거됨 - DetectionConfigAuditRepositoryCustom 인터페이스에서 선언, DetectionConfigAuditRepositoryImpl에서 구현
    // Page<DetectionConfigAuditEntity> findByTargetTypeAndDateRange(ConfigAuditTargetType targetType, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * 최근 변경 이력 조회
     */
    List<DetectionConfigAuditEntity> findTop50ByOrderByChangedAtDesc();
}

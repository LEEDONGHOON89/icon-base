package com.itmasters.icon.api.audit.adapter.out.persistence;

import com.itmasters.icon.api.audit.domain.ConfigAuditTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Detection Config Audit 커스텀 Repository 인터페이스
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
public interface DetectionConfigAuditRepositoryCustom {

    /**
     * 기간별 변경 이력 조회
     *
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @param pageable 페이징 정보
     * @return 변경 이력 목록 (페이징)
     */
    Page<DetectionConfigAuditEntity> findByDateRange(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);

    /**
     * 타입별 + 기간별 변경 이력 조회
     *
     * @param targetType 대상 타입
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @param pageable 페이징 정보
     * @return 변경 이력 목록 (페이징)
     */
    Page<DetectionConfigAuditEntity> findByTargetTypeAndDateRange(
            ConfigAuditTargetType targetType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);
}

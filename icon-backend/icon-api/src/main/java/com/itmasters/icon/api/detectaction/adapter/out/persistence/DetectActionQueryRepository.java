package com.itmasters.icon.api.detectaction.adapter.out.persistence;

import com.itmasters.icon.api.detectaction.dto.DetectActionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * 탐지 조치 QueryDSL Repository
 */
public interface DetectActionQueryRepository {

    /**
     * 탐지 조치 목록 조회 (페이징)
     */
    Page<DetectActionDto> findDetectActions(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String riskLevel,
            String actionStatus,
            Pageable pageable
    );
}

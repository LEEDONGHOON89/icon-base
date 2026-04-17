package com.itmasters.icon.api.detectaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 탐지 조치 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectActionDto {

    private Long detectActionId;
    private Long detectScenarioId;
    private String scenarioId;
    private String scenarioName;
    private String groupKey;
    private String actionType;
    private String actionStatus;
    private String riskLevel;
    private String actionMemo;
    private String actionReason;
    private String requestedBy;
    private LocalDateTime requestedAt;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String completedBy;
    private LocalDateTime completedAt;
    private LocalDateTime regDt;

    /**
     * 조치 상태 조회 요청 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRequest {
        private java.time.LocalDate startDate;
        private java.time.LocalDate endDate;
        private String riskLevel;  // BLOCK, REVIEW
        private String actionStatus;  // PENDING, APPROVED, REJECTED, COMPLETED
        private Integer page;
        private Integer size;
    }

    /**
     * 조치 업데이트 요청 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String actionMemo;      // 조치 메모
        private String actionReason;    // 조치 사유
        private String actionStatus;    // 조치 상태 (PENDING, APPROVED, REJECTED, COMPLETED)
    }
}

package com.itmasters.icon.api.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 엔티티 행적 조회 DTO
 *
 * 특정 entity_id가 포함된 모든 탐지 이력과 활동 로그를 조회합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityHistoryDto {

    private String entityId;
    private String entityType;  // 엔티티 타입 (예: CUSTOMER, PRODUCT 등)
    private EntityHistoryStats stats;
    private List<DetectionRecord> detections;
    private List<ActivityLog> activities;

    /**
     * 통계 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityHistoryStats {
        private int totalDetections;
        private int scenarioCount;
        private int aggregateCount;
        private int ruleCount;
        private int totalActivities;
        private LocalDateTime firstActivityAt;
        private LocalDateTime lastActivityAt;
    }

    /**
     * 탐지 이력
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectionRecord {
        private String type;  // SCENARIO, AGGREGATE, RULE
        private String detectionId;
        private String detectionName;
        private String groupKey;
        private String riskLevel;
        private LocalDateTime detectedAt;
        private Map<String, Object> details;
    }

    /**
     * 활동 로그 (이벤트 스트림)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityLog {
        private Long eventStreamId;
        private String groupKey;
        private String dataSourceId;
        private String transactionId;
        private LocalDateTime eventDt;
        private Map<String, Object> eventData;
    }
}

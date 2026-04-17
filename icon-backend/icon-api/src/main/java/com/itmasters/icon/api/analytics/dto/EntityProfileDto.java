package com.itmasters.icon.api.analytics.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * 엔티티 프로필 조회 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityProfileDto {

    /**
     * 엔티티 ID (group_key)
     */
    private String entityId;

    /**
     * 엔티티 타입 (CUSTOMER, ACCOUNT, DEVICE 등)
     */
    private String entityType;

    /**
     * 엔티티 속성 (entity_attributes 테이블)
     */
    private Map<String, Object> attributes;

    /**
     * 탐지 이력 - 룰
     */
    private List<DetectedRuleDto> detectedRules;

    /**
     * 탐지 이력 - 룰 (Aggregate)
     */
    private List<DetectRuleDto> detectedAggregates;

    /**
     * 탐지 이력 - 시나리오
     */
    private List<DetectScenarioDto> detectedScenarios;

    /**
     * 탐지 통계
     */
    private DetectionStats stats;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetectionStats {
        private Long totalRules;
        private Long totalAggregates;
        private Long totalScenarios;
    }
}

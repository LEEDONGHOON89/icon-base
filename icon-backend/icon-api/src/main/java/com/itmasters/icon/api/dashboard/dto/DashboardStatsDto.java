package com.itmasters.icon.api.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 대시보드 통계 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {

    /**
     * 금일 전체거래 (mapped_storages 테이블 row count)
     */
    private Long totalTransactions;

    /**
     * 탐지거래 (detect_scenarios 테이블 count)
     */
    private Long detectedTransactions;

    /**
     * 탐지율 (탐지거래 / 금일 전체거래 * 100)
     */
    private BigDecimal detectionRate;

    /**
     * 위험수준별 탐지 건수
     */
    private RiskLevelStats riskLevelStats;

    /**
     * 위험수준별 통계 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskLevelStats {
        private Long block;                // 차단 탐지 수 (BLOCK)
        private ActionStatusStats blockActions;  // 차단 조치 상태별 수
        private Long review;               // 심사 탐지 수 (REVIEW)
        private ActionStatusStats reviewActions; // 심사 조치 상태별 수
        private Long intensive;            // 집중모니터링 (INTENSIVE)
        private Long monitor;              // 모니터링 (MONITOR)

        public static RiskLevelStats from(Map<String, Long> countMap, Map<String, ActionStatusStats> actionStatsMap) {
            return RiskLevelStats.builder()
                    .block(countMap.getOrDefault("BLOCK", 0L))
                    .blockActions(actionStatsMap.getOrDefault("BLOCK", ActionStatusStats.empty()))
                    .review(countMap.getOrDefault("REVIEW", 0L))
                    .reviewActions(actionStatsMap.getOrDefault("REVIEW", ActionStatusStats.empty()))
                    .intensive(countMap.getOrDefault("INTENSIVE", 0L))
                    .monitor(countMap.getOrDefault("MONITOR", 0L))
                    .build();
        }
    }

    /**
     * 조치 상태별 통계 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionStatusStats {
        private Long pending;      // 미처리 (PENDING)
        private Long approved;     // 승인 (APPROVED)
        private Long rejected;     // 거절 (REJECTED)
        private Long completed;    // 완료 (COMPLETED)

        public static ActionStatusStats empty() {
            return ActionStatusStats.builder()
                    .pending(0L)
                    .approved(0L)
                    .rejected(0L)
                    .completed(0L)
                    .build();
        }

        public Long getTotal() {
            return pending + approved + rejected + completed;
        }
    }

    /**
     * 탐지영역별 탐지 통계
     */
    private List<DetectionAreaStats> detectionAreaStats;

    /**
     * 탐지영역별 통계 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectionAreaStats {
        private String detectionAreaId;
        private String detectionAreaName;
        private String colorCode;
        private Long detectionCount;
        private List<ScenarioSummary> topScenarios;
    }

    /**
     * 시나리오 요약 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScenarioSummary {
        private String scenarioId;
        private String scenarioName;
        private String riskLevelId;
        private Long count;
    }

    /**
     * 시간대별 탐지 통계
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyStats {
        private Integer hour;           // 0-23
        private Long transactionCount;  // 거래 건수
        private Long detectionCount;    // 탐지 건수
    }

    /**
     * 통계 계산 메서드
     */
    public static DashboardStatsDto calculate(
            Long totalTransactions,
            Long detectedTransactions,
            Map<String, Long> riskLevelCounts,
            Map<String, ActionStatusStats> actionStatsMap,
            List<DetectionAreaStats> detectionAreaStats) {
        BigDecimal rate = BigDecimal.ZERO;

        if (totalTransactions != null && totalTransactions > 0 && detectedTransactions != null) {
            rate = BigDecimal.valueOf(detectedTransactions)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalTransactions), 4, RoundingMode.HALF_UP);
        }

        return DashboardStatsDto.builder()
                .totalTransactions(totalTransactions != null ? totalTransactions : 0L)
                .detectedTransactions(detectedTransactions != null ? detectedTransactions : 0L)
                .detectionRate(rate)
                .riskLevelStats(RiskLevelStats.from(riskLevelCounts, actionStatsMap))
                .detectionAreaStats(detectionAreaStats)
                .build();
    }
}

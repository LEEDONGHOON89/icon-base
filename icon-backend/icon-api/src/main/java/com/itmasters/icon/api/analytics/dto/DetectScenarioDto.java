package com.itmasters.icon.api.analytics.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectScenarioDto {
    private String scenarioId;
    private String scenarioName;
    private String groupKey;
    private LocalDateTime detectedAt;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private Integer aggregateCount;   // 연결된 집계 수
    private Integer passedCount;      // 이번 실행(해당 앵커)에서 통과한 집계 수
    private Boolean allPassed;        // aggregateCount == passedCount
    private String detectionAreaId;   // 탐지영역 ID
    private String detectionAreaName; // 탐지영역명
    private String transactionId;     // 트랜잭션 ID
}

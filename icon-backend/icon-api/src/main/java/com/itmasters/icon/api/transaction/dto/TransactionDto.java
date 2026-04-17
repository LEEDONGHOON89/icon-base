package com.itmasters.icon.api.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 트랜잭션 추적 DTO
 */
public class TransactionDto {

    /**
     * 트랜잭션 추적 정보 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackingInfo {
        private String transactionId;
        private String dataSourceId;
        private String dataSourceName;
        private LocalDateTime firstSeenAt;
        private LocalDateTime lastSeenAt;
        
        private List<MappedStorageInfo> mappedStorages;
        private List<EventStreamInfo> eventStreams;
        private List<DetectRuleInfo> detectRules;
        private List<DetectAggregateInfo> detectAggregates;
        private List<DetectScenarioInfo> detectScenarios;
    }

    /**
     * MappedStorage 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MappedStorageInfo {
        private Long mappedDataStorageId;
        private Long landingRecordId;
        private Integer rowIndex;
        private Map<String, Object> rowData;
        private LocalDateTime regDt;
    }

    /**
     * EventStream 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventStreamInfo {
        private Long eventStreamId;
        private String groupKey;
        private Map<String, Object> eventData;
        private Long mappedDataStorageId;
        private LocalDateTime eventDt;
    }

    /**
     * DetectRule 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectRuleInfo {
        private Long detectRuleId;
        private String ruleId;
        private String ruleName;
        private String groupKey;
        private Map<String, Object> matchedFields;
        private LocalDateTime detectedAt;
    }

    /**
     * DetectAggregate 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectAggregateInfo {
        private Long detectAggregateId;
        private String aggregateId;
        private String groupKey;
        private String operator;
        private Boolean pass;
        private java.math.BigDecimal matchedCount;
        private Long mappedStorageId;
        private LocalDateTime detectedAt;
    }

    /**
     * DetectScenario 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectScenarioInfo {
        private Long detectScenarioId;
        private String scenarioId;
        private String groupKey;
        private LocalDateTime detectedAt;
    }

    /**
     * 최근 트랜잭션 목록 항목
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionListItem {
        private String transactionId;
        private String dataSourceId;
        private String dataSourceName;
        private LocalDateTime firstSeenAt;
        private LocalDateTime lastSeenAt;
        private Integer totalRecords;
        private Integer scenariosDetected;
    }
}

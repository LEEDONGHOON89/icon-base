package com.itmasters.icon.api.analytics.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * detect_rules 조회용 엔티티.
 * 실제 테이블 구조에 맞게 수정 (matched_fields 제거)
 */
@Entity
@Table(name = "detect_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiDetectRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detect_rule_id")
    private Long detectRuleId;

    @Column(name = "rule_id", length = 100)
    private String ruleId;

    @Column(name = "group_key", length = 200)
    private String groupKey;

    @Column(name = "original_group_key", length = 200)
    private String originalGroupKey;

    @Column(name = "operator", length = 30)
    private String operator;

    @Column(name = "window_minutes")
    private Integer windowMinutes;

    @Column(name = "dedup_minutes")
    private Integer dedupMinutes;

    @Column(name = "start_dt")
    private LocalDateTime startDt;

    @Column(name = "end_dt")
    private LocalDateTime endDt;

    @Column(name = "detected_dt")
    private LocalDateTime detectedDt;

    @Column(name = "event_dt")
    private LocalDateTime eventDt;

    @Column(name = "mapped_storage_id")
    private Long mappedStorageId;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "matched_count")
    private BigDecimal matchedCount;

    @Column(name = "threshold_count")
    private BigDecimal thresholdCount;

    @Column(name = "pass")
    private Boolean pass;
}

package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DetectRules result entity (룰 pass 결과)
 * - 테이블명: detect_rules
 */
@Entity
@Table(name = "detect_rules",
        indexes = {
                @Index(name = "idx_detect_rule_exec", columnList = "exec_ds_mp_id"),
                @Index(name = "idx_detect_rule_gk_time", columnList = "group_key,end_dt DESC"),
                @Index(name = "idx_detect_rule_rule_time", columnList = "rule_id,end_dt DESC")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detect_rule_id")
    private Long detectRuleId;

    @Column(name = "group_key", nullable = false, length = 200)
    private String groupKey;

    // Original group_key from event_stream (before group_by_fields transformation)
    // Used when analytics needs to find events by the original grouping
    @Column(name = "original_group_key", length = 200)
    private String originalGroupKey;

    @Column(name = "rule_id", nullable = false, length = 50)
    private String ruleId; // rules.rule_id

    @Column(name = "operator", nullable = false, length = 30)
    private String operator;


    @Column(name = "window_minutes", nullable = false)
    private Integer windowMinutes;

    @Column(name = "dedup_minutes")
    private Integer dedupMinutes;

    @Column(name = "start_dt", nullable = false)
    private LocalDateTime startDt;

    @Column(name = "end_dt", nullable = false)
    private LocalDateTime endDt;

    @Column(name = "detected_dt", nullable = false)
    private LocalDateTime detectedDt;

    @Column(name = "event_dt")
    private LocalDateTime eventDt;

    // Anchor event's original identifier for cross-source traceability
    // Stores event_stream.mapped_storage_id of the anchor event
    @Column(name = "mapped_storage_id")
    private Long mappedStorageId;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "matched_count")
    private java.math.BigDecimal matchedCount;

    @Column(name = "threshold_count")
    private java.math.BigDecimal thresholdCount;

    @Column(name = "pass", nullable = false)
    private Boolean pass;
}

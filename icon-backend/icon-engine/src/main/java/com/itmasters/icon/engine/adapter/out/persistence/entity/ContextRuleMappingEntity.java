package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 컨텍스트-룰 매핑 엔티티
 * 특정 컨텍스트에서 매칭된 룰들의 정보
 */
@Entity
@Table(name = "context_rule_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContextRuleMappingEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "context_rule_mapping_id")
    private Long contextRuleMappingId;
    
    @Column(name = "detection_context_id", nullable = false)
    private Long detectionContextId;
    
    @Column(name = "rule_id", nullable = false, length = 13)
    private String ruleId;
    
    @Column(name = "rule_name", length = 255)
    private String ruleName;
    
    @Column(name = "rule_category", length = 100)
    private String ruleCategory;
    
    @Column(name = "detection_timestamp", nullable = false)
    private LocalDateTime detectionTimestamp;
    
    @Column(name = "sequence_number")
    private Integer sequenceNumber;
    
    @Column(name = "time_since_last_rule_seconds")
    private Integer timeSinceLastRuleSeconds;
    
    @Column(name = "rule_score")
    private Double ruleScore;
    
    @Column(name = "severity", length = 20)
    private String severity;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_conditions", columnDefinition = "jsonb")
    private String matchedConditions;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_metadata", columnDefinition = "jsonb")
    private String ruleMetadata;
    
    @Column(name = "is_trigger")
    private Boolean isTrigger;
    
    @Column(name = "alert_sent")
    private Boolean alertSent;
    
    @Column(name = "reg_dt")
    private LocalDateTime regDt;
    
    @PrePersist
    protected void onCreate() {
        if (regDt == null) {
            regDt = LocalDateTime.now();
        }
        if (detectionTimestamp == null) {
            detectionTimestamp = LocalDateTime.now();
        }
        if (isTrigger == null) {
            isTrigger = false;
        }
        if (alertSent == null) {
            alertSent = false;
        }
    }
}
package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 시나리오 탐지 이벤트 엔티티
 * 각 룰 매칭이 발생할 때 생성되는 이벤트
 */
@Entity
@Table(name = "detection_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectionEventEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detection_event_id")
    private Long eventId;
    
    @Column(name = "detection_context_id", nullable = false)
    private Long contextId;
    
    @Column(name = "rule_id", length = 13)
    private String ruleId;
    
    @Column(name = "rule_name", length = 255)
    private String ruleName;
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;  // RULE_MATCH, THRESHOLD_EXCEEDED, etc.
    
    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", columnDefinition = "jsonb")
    private String eventData;  // JSON string
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_conditions", columnDefinition = "jsonb")
    private String matchedConditions;  // JSON string
    
    @Column(name = "anomaly_score")
    private Double anomalyScore;
    
    @Column(name = "severity", length = 20)
    private String severity;
    
    @Column(name = "category", length = 100)
    private String category;
    
    @Column(name = "description", columnDefinition = "text")
    private String description;
    
    @Column(name = "exec_ds_mp_id")
    private Long execDsMpId;
    
    @Column(name = "reg_dt")
    private LocalDateTime regDt;
    
    @PrePersist
    protected void onCreate() {
        if (regDt == null) {
            regDt = LocalDateTime.now();
        }
        if (eventTimestamp == null) {
            eventTimestamp = LocalDateTime.now();
        }
    }
}
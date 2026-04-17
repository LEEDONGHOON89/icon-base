package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 시나리오 탐지 컨텍스트 엔티티
 * 동일한 상관 관계 키를 가진 이벤트들의 그룹
 */
@Entity
@Table(name = "detection_context")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectionContextEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detection_context_id")
    private Long detectionContextId;
    
    @Column(name = "detect_key", nullable = false, length = 200)
    private String correlationKey;
    
    @Column(name = "key_type", nullable = false, length = 50)
    private String keyType;
    
    @Column(name = "profile_id", nullable = false, length = 13)
    private String profileId;
    
    @Column(name = "status", length = 20)
    private String status;  // ACTIVE, CLOSED, EXPIRED
    
    @Column(name = "event_count")
    private Integer eventCount;
    
    @Column(name = "rule_match_count")
    private Integer ruleMatchCount;
    
    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;
    
    @Column(name = "window_end")
    private LocalDateTime windowEnd;
    
    @Column(name = "window_duration_minutes")
    private Integer windowDurationMinutes;
    
    @Column(name = "risk_level", length = 20)
    private String riskLevel;  // LOW, MEDIUM, HIGH, CRITICAL
    
    @Column(name = "anomaly_score")
    private Double anomalyScore;
    
    @Column(name = "scenario_type", length = 100)
    private String scenarioType;
    
    @Column(name = "scenario_stage", length = 50)
    private String scenarioStage;
    
    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_data", columnDefinition = "jsonb")
    private Map<String, Object> contextData;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (windowStart == null) {
            windowStart = LocalDateTime.now();
        }
        if (eventCount == null) {
            eventCount = 0;
        }
        if (ruleMatchCount == null) {
            ruleMatchCount = 0;
        }
        if (status == null) {
            status = "ACTIVE";
        }
        if (windowDurationMinutes == null) {
            windowDurationMinutes = 60;
        }
        if (keyType == null) {
            keyType = "CUSTOMER";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
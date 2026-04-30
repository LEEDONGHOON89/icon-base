package com.itmasters.icon.api.analytics.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 조치 탐지 Entity (API 모듈용 읽기 전용)
 * detect_actions 테이블 매핑
 */
@Entity
@Table(name = "detect_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiDetectActionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detect_action_id")
    private Long detectActionId;

    @Column(name = "detect_scenario_id", nullable = false)
    private Long detectScenarioId;

    @Column(name = "scenario_id", nullable = false, length = 50)
    private String scenarioId;

    @Column(name = "group_key", nullable = false, length = 100)
    private String groupKey;

    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType;

    @Column(name = "action_status", nullable = false, length = 20)
    private String actionStatus;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "action_memo", columnDefinition = "TEXT")
    private String actionMemo;

    @Column(name = "action_reason", columnDefinition = "TEXT")
    private String actionReason;

    @Column(name = "requested_by", nullable = false, length = 50)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "completed_by", length = 50)
    private String completedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "reg_dt", nullable = false)
    private LocalDateTime regDt;
}

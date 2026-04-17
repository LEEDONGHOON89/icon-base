package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 탐지된 시나리오에 대한 조치(Action) 엔티티

 * detect_scenarios 테이블의 탐지 결과에 대해
 * BLOCK, HOLD 등의 조치를 요청하고 승인/완료 추적
 * </p>
 */
@Entity
@Table(name = "detect_actions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DetectActionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detect_action_id")
    private Long detectActionId;

    /**
     * 탐지 시나리오 ID (FK to detect_scenarios)
     */
    @Column(name = "detect_scenario_id", nullable = false)
    private Long detectScenarioId;

    /**
     * 시나리오 정의 ID
     */
    @Column(name = "scenario_id", nullable = false, length = 50)
    private String scenarioId;

    /**
     * 그룹 키 (엔티티 식별자: 사용자, 계좌 등)
     */
    @Column(name = "group_key", nullable = false, length = 100)
    private String groupKey;

    /**
     * 조치 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private ActionType actionType;

    /**
     * 조치 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_status", nullable = false, length = 20)
    @Builder.Default
    private ActionStatus actionStatus = ActionStatus.PENDING;

    /**
     * 리스크 레벨 (옵션)
     */
    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    /**
     * 조치 메모
     */
    @Column(name = "action_memo", columnDefinition = "TEXT")
    private String actionMemo;

    /**
     * 조치 사유
     */
    @Column(name = "action_reason", columnDefinition = "TEXT")
    private String actionReason;

    /**
     * 조치 요청자
     */
    @Column(name = "requested_by", nullable = false, length = 50)
    private String requestedBy;

    /**
     * 조치 요청 시간
     */
    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();

    /**
     * 조치 승인자
     */
    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    /**
     * 조치 승인 시간
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * 조치 완료자
     */
    @Column(name = "completed_by", length = 50)
    private String completedBy;

    /**
     * 조치 완료 시간
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 추가 메타데이터 (JSONB)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /**
     * 등록 일시 (로그성 데이터이므로 regDt만 사용)
     */
    @CreationTimestamp
    @Column(name = "reg_dt", nullable = false, updatable = false)
    private LocalDateTime regDt;

    /**
     * 조치 유형
     */
    public enum ActionType {
        /** 차단 */
        BLOCK,
        /** 보류 */
        HOLD,
        /** 허용 */
        ALLOW,
        /** 모니터링 */
        MONITOR
    }

    /**
     * 조치 상태
     */
    public enum ActionStatus {
        /** 요청 대기 */
        PENDING,
        /** 승인됨 */
        APPROVED,
        /** 거부됨 */
        REJECTED,
        /** 완료됨 */
        COMPLETED
    }

    /**
     * 조치 승인
     */
    public void approve(String approvedBy) {
        this.actionStatus = ActionStatus.APPROVED;
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * 조치 거부
     */
    public void reject(String rejectedBy, String reason) {
        this.actionStatus = ActionStatus.REJECTED;
        this.approvedBy = rejectedBy;
        this.approvedAt = LocalDateTime.now();
        if (reason != null) {
            this.actionReason = reason;
        }
    }

    /**
     * 조치 완료
     */
    public void complete(String completedBy) {
        this.actionStatus = ActionStatus.COMPLETED;
        this.completedBy = completedBy;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 메모 추가
     */
    public void addMemo(String memo) {
        if (this.actionMemo == null) {
            this.actionMemo = memo;
        } else {
            this.actionMemo += "\n" + memo;
        }
    }
}

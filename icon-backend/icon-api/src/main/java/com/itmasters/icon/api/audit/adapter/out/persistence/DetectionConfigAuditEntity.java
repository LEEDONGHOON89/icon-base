package com.itmasters.icon.api.audit.adapter.out.persistence;

import com.itmasters.icon.api.audit.domain.ConfigAuditAction;
import com.itmasters.icon.api.audit.domain.ConfigAuditTargetType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 탐지 설정 변경 감사 로그 Entity
 */
@Entity
@Table(name = "detection_config_audit")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DetectionConfigAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ConfigAuditTargetType targetType;

    @Column(name = "target_id", nullable = false, length = 50)
    private String targetId;

    @Column(name = "target_name", length = 255)
    private String targetName;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 10)
    private ConfigAuditAction action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changed_fields", columnDefinition = "jsonb")
    private String changedFields;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_snapshot", columnDefinition = "jsonb")
    private String beforeSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_snapshot", columnDefinition = "jsonb")
    private String afterSnapshot;

    @Column(name = "change_reason")
    private String changeReason;

    @Column(name = "changed_by", nullable = false, length = 50)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * 감사 로그 생성 팩토리 메서드
     */
    public static DetectionConfigAuditEntity of(
            ConfigAuditTargetType targetType,
            String targetId,
            String targetName,
            ConfigAuditAction action,
            String changedFields,
            String beforeSnapshot,
            String afterSnapshot,
            String changedBy,
            String ipAddress
    ) {
        DetectionConfigAuditEntity entity = new DetectionConfigAuditEntity();
        entity.targetType = targetType;
        entity.targetId = targetId;
        entity.targetName = targetName;
        entity.action = action;
        entity.changedFields = changedFields;
        entity.beforeSnapshot = beforeSnapshot;
        entity.afterSnapshot = afterSnapshot;
        entity.changedBy = changedBy;
        entity.changedAt = LocalDateTime.now();
        entity.ipAddress = ipAddress;
        return entity;
    }
}

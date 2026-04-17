package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 리스크 레벨 Entity (읽기 전용)
 */
@Entity
@Table(name = "risk_levels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RiskLevelEntity {

    @Id
    @Column(name = "risk_level_id", length = 30)
    private String riskLevelId;

    @Column(name = "level_code", nullable = false)
    private Integer levelCode;

    @Column(name = "level_name", nullable = false, length = 50)
    private String levelName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "color_code", length = 20)
    private String colorCode;

    @Column(name = "action_type", length = 30)
    private String actionType;

    @Column(name = "notification_required")
    private Boolean notificationRequired;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

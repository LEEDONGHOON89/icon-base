package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 시나리오 Entity (Engine용)
 * API 모듈의 ScenarioEntity와 동일한 구조
 */
@Entity
@Table(name = "scenarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EngineScenarioEntity {
    
    @Id
    @Column(name = "scenario_id", length = 13)
    private String scenarioId;

    @Column(name = "scenario_name", nullable = false, length = 255)
    private String scenarioName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 호환성 유지를 위한 메서드 - created_at 반환
     */
    public LocalDateTime getRegDt() {
        return createdAt;
    }

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    // Step4 signals-only 메타
    @Column(name = "dedup_minutes")
    private Integer dedupMinutes;            // 중복 억제 창(분) - 동일 시나리오 중복 탐지 방지

    @Column(name = "entity_filter_json", columnDefinition = "TEXT")
    private String entityFilterJson;         // 엔티티 필터 조건 (JSON 배열)

    @Column(name = "risk_level_id", length = 30)
    private String riskLevelId;              // 리스크 레벨 ID (FK to risk_levels)
}

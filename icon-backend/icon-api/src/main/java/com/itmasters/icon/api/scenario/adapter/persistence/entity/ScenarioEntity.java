package com.itmasters.icon.api.scenario.adapter.persistence.entity;

import com.itmasters.icon.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 시나리오 Entity
 */
@Entity
@Table(name = "scenarios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScenarioEntity extends Auditable {
    @Id
    @Column(name = "scenario_id", length = 13)
    private String scenarioId;

    @Column(name = "scenario_name", nullable = false, length = 255, unique = true)
    private String scenarioName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entity_filter_json", columnDefinition = "jsonb")
    private String entityFilterJson;

    // Phase 2: Entity 기반 탐지를 위한 primary_entity_type 필드
    @Column(name = "primary_entity_type", length = 50)
    private String primaryEntityType;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * 호환성 유지를 위한 메서드 - created_at 반환
     * (기존 reg_dt 컬럼은 created_at으로 통합됨)
     */
    public LocalDateTime getRegDt() {
        return getCreatedAt();
    }

    @Column(name = "risk_level_id", length = 30)
    private String riskLevelId;

    @Column(name = "detection_area_id", length = 30)
    private String detectionAreaId;

    // 엔진 설정 필드
    // [2026-04-24] DB DEFAULT 0 NOT NULL 과 일치하도록 Java 기본값 설정 (null 삽입 방지)
    @Column(name = "dedup_minutes", nullable = false)
    private Integer dedupMinutes = 0;   // 중복 제거 창 (분) - 0 = 중복 억제 없음

    // Phase 2: Primary Entity Type 업데이트
    public void updatePrimaryEntityType(String primaryEntityType) {
        this.primaryEntityType = primaryEntityType;
    }

    // 위험수준 업데이트
    public void updateRiskLevel(String riskLevelId) {
        this.riskLevelId = riskLevelId;
    }

    // 탐지영역 업데이트
    public void updateDetectionArea(String detectionAreaId) {
        this.detectionAreaId = detectionAreaId;
    }
}

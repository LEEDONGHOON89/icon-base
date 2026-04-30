package com.itmasters.icon.api.scenario.domain;

import com.itmasters.icon.entity.Auditable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 시나리오 도메인 - 여러 원자규칙을 조합한 복잡한 비즈니스 로직
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Scenario extends Auditable {
    private String scenarioId; // 시나리오 고유 ID - TSID
    private String scenarioName; // 시나리오명
    private String description; // 설명
    private String entityFilterJson; // 엔티티 필터 JSON
    private boolean isActive; // 활성화 여부

    /**
     * 호환성 유지를 위한 메서드 - created_at 반환
     * (기존 reg_dt는 Auditable.createdAt으로 통합됨)
     */
    public LocalDateTime getRegDt() {
        return getCreatedAt();
    }
    private String riskLevelId; // 위험 레벨 ID
    private String detectionAreaId; // 탐지 영역 ID
    private String primaryEntityType; // 주요 엔티티 타입

    // 엔진 설정 필드
    private Integer dedupMinutes;   // 중복 제거 창 (분) - 동일 탐지 중복 방지

    /**
     * 시나리오 생성 Factory 메서드
     */
    public static Scenario of(String scenarioName, String description, String entityFilterJson) {
        return of(scenarioName, description, entityFilterJson, null, null, null, null);
    }

    /**
     * 시나리오 생성 Factory 메서드 (기본 필드)
     */
    public static Scenario of(String scenarioName, String description, String entityFilterJson,
                              String riskLevelId, String detectionAreaId, String primaryEntityType) {
        return of(scenarioName, description, entityFilterJson, riskLevelId, detectionAreaId, primaryEntityType, null);
    }

    /**
     * 시나리오 생성 Factory 메서드 (엔진 설정 필드 포함)
     */
    public static Scenario of(String scenarioName, String description, String entityFilterJson,
                              String riskLevelId, String detectionAreaId, String primaryEntityType,
                              Integer dedupMinutes) {
        Scenario scenario = new Scenario();
        scenario.scenarioName = scenarioName;
        scenario.description = description;
        // PostgreSQL JSON 타입은 빈 문자열을 허용하지 않으므로 null로 변환
        scenario.entityFilterJson = (entityFilterJson == null || entityFilterJson.trim().isEmpty()) ? null : entityFilterJson;
        scenario.riskLevelId = riskLevelId;
        scenario.detectionAreaId = detectionAreaId;
        // [2026-04-24] primaryEntityType NOT NULL 제약 — null/blank 이면 DB DEFAULT 'CUSTOMER' 적용
        scenario.primaryEntityType = (primaryEntityType != null && !primaryEntityType.isBlank()) ? primaryEntityType : "CUSTOMER";
        scenario.dedupMinutes = dedupMinutes;
        scenario.isActive = true;
        return scenario;
    }

    /**
     * ID 할당 (Repository에서 사용)
     */
    public void assignId(String id) {
        if (this.scenarioId != null) {
            throw new IllegalStateException("ID가 이미 할당되었습니다");
        }
        this.scenarioId = id;
    }

    /**
     * 시나리오 수정 (null인 필드는 기존 값 유지)
     */
    public void update(String scenarioName, String description, String entityFilterJson, Boolean isActive) {
        update(scenarioName, description, entityFilterJson, isActive, null, null, null, null);
    }

    /**
     * 시나리오 수정 (기본 필드, null인 필드는 기존 값 유지)
     */
    public void update(String scenarioName, String description, String entityFilterJson, Boolean isActive,
                       String riskLevelId, String detectionAreaId, String primaryEntityType) {
        update(scenarioName, description, entityFilterJson, isActive, riskLevelId, detectionAreaId, primaryEntityType, null);
    }

    /**
     * 시나리오 수정 (엔진 설정 필드 포함, null인 필드는 기존 값 유지)
     */
    public void update(String scenarioName, String description, String entityFilterJson, Boolean isActive,
                       String riskLevelId, String detectionAreaId, String primaryEntityType,
                       Integer dedupMinutes) {
        if (scenarioName != null) {
            this.scenarioName = scenarioName;
        }
        if (description != null) {
            this.description = description;
        }
        // entityFilterJson은 명시적으로 전달된 경우에만 업데이트 (빈 문자열은 null로 변환)
        if (entityFilterJson != null) {
            this.entityFilterJson = entityFilterJson.trim().isEmpty() ? null : entityFilterJson;
        }
        if (isActive != null) {
            this.isActive = isActive;
        }
        if (riskLevelId != null) {
            this.riskLevelId = riskLevelId;
        }
        if (detectionAreaId != null) {
            this.detectionAreaId = detectionAreaId;
        }
        if (primaryEntityType != null) {
            this.primaryEntityType = primaryEntityType;
        }
        if (dedupMinutes != null) {
            this.dedupMinutes = dedupMinutes;
        }
    }

    /**
     * 시나리오 활성화/비활성화
     */
    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 시나리오 검증
     */
    public void validate() {
        if (scenarioName == null || scenarioName.trim().isEmpty()) {
            throw new IllegalArgumentException("시나리오명은 필수입니다.");
        }
    }

    /**
     * ID 설정 (Repository에서 조회 시 사용)
     */
    public void setScenarioId(String scenarioId) {
        this.scenarioId = scenarioId;
    }

    /**
     * 활성화 상태 설정 (Repository에서 조회 시 사용)
     */
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * 위험 레벨 ID 설정 (Repository에서 조회 시 사용 - ModelMapper)
     */
    public void setRiskLevelId(String riskLevelId) {
        this.riskLevelId = riskLevelId;
    }

    /**
     * 탐지 영역 ID 설정 (Repository에서 조회 시 사용 - ModelMapper)
     */
    public void setDetectionAreaId(String detectionAreaId) {
        this.detectionAreaId = detectionAreaId;
    }

    /**
     * 주요 엔티티 타입 설정 (Repository에서 조회 시 사용 - ModelMapper)
     */
    public void setPrimaryEntityType(String primaryEntityType) {
        this.primaryEntityType = primaryEntityType;
    }

    /**
     * 중복 제거 창 설정 (분) - ModelMapper용
     */
    public void setDedupMinutes(Integer dedupMinutes) {
        this.dedupMinutes = dedupMinutes;
    }
}

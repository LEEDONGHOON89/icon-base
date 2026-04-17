package com.itmasters.icon.entity;

// Auditable은 같은 패키지에 있음
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * 파생 필드 규칙 엔티티
 *
 * mapped_data에서 파생 필드를 계산하는 규칙을 정의합니다.
 * 원본 데이터에 없는 필드(예: is_third_party)를 계산식으로 생성합니다.
 *
 * 처리 시점: Profile 매핑 완료 후, event_stream 저장 전
 */
@Entity
@Table(name = "derived_rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DerivedRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long ruleId;

    /**
     * 적용 대상 DataSource
     */
    @Column(name = "data_source_id", nullable = false, length = 50)
    private String dataSourceId;

    /**
     * 생성할 파생 필드명
     * 예: "is_third_party", "risk_score"
     */
    @Column(name = "target_field", nullable = false, length = 100)
    private String targetField;

    /**
     * 필드 타입
     * "boolean", "string", "number"
     */
    @Column(name = "field_type", nullable = false, length = 20)
    private String fieldType;

    /**
     * 계산 방식
     * - FIELD_COMPARISON: 현재 이벤트 필드 비교
     * - ENTITY_RELATION_LOOKUP: entity_relations 테이블 조회
     * - EVENT_STREAM_LOOKUP: event_stream 과거 이력 조회
     * - ENTITY_ATTRIBUTE_LOOKUP: entity_attributes 테이블 조회
     * - FORMULA: 복잡한 수식 계산
     */
    @Column(name = "computation_type", nullable = false, length = 50)
    private String computationType;

    /**
     * 계산 설정 (JSONB)
     * computation_type에 따라 다른 형식의 JSON 저장
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "computation_config", nullable = false, columnDefinition = "jsonb")
    private String computationConfig;

    /**
     * 실행 우선순위 (낮을수록 먼저 실행)
     * 파생 필드 간 의존성이 있을 때 사용
     */
    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    /**
     * 규칙 활성화 여부
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 규칙 설명
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 등록자 ID
     */
    @Column(name = "reg_user_id", length = 100)
    private String regUserId;

    /**
     * 등록 일시
     */
    @Column(name = "reg_dt")
    private java.time.LocalDateTime regDt;

    /**
     * 수정자 ID
     */
    @Column(name = "upd_user_id", length = 100)
    private String updUserId;

    /**
     * 수정 일시
     */
    @Column(name = "upd_dt")
    private java.time.LocalDateTime updDt;

    // === 정적 팩토리 메서드 ===

    /**
     * 새 파생 필드 규칙 생성
     */
    public static DerivedRuleEntity create(
            String dataSourceId,
            String targetField,
            String fieldType,
            String computationType,
            String computationConfig,
            Integer priority,
            String description) {

        DerivedRuleEntity entity = new DerivedRuleEntity();
        entity.dataSourceId = dataSourceId;
        entity.targetField = targetField;
        entity.fieldType = fieldType;
        entity.computationType = computationType;
        entity.computationConfig = computationConfig;
        entity.priority = priority != null ? priority : 0;
        entity.description = description;
        entity.isActive = true;

        return entity;
    }

    // === 비즈니스 메서드 ===

    /**
     * 규칙 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 규칙 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 설명 업데이트
     */
    public void updateDescription(String description) {
        this.description = description;
    }

    /**
     * 계산 설정 업데이트
     */
    public void updateComputationConfig(String computationConfig) {
        if (computationConfig == null || computationConfig.trim().isEmpty()) {
            throw new IllegalArgumentException("Computation config cannot be empty");
        }
        this.computationConfig = computationConfig;
    }

    /**
     * 우선순위 업데이트
     */
    public void updatePriority(Integer priority) {
        if (priority == null || priority < 0) {
            throw new IllegalArgumentException("Priority must be non-negative");
        }
        this.priority = priority;
    }

    /**
     * icon-engine 호환성: computationConfig를 Map으로 파싱하여 반환
     *
     * @return JSON 문자열을 파싱한 Map (파싱 실패 시 빈 Map 반환)
     */
    public Map<String, Object> getComputationConfigAsMap() {
        if (computationConfig == null || computationConfig.trim().isEmpty()) {
            return new java.util.HashMap<>();
        }

        try {
            // JSON 파싱을 위한 ObjectMapper 사용
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(computationConfig, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            // 파싱 실패 시 빈 Map 반환
            return new java.util.HashMap<>();
        }
    }
}

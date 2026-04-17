package com.itmasters.icon.api.rule.adapter.persistence.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.entity.Auditable;
import com.itmasters.icon.api.common.domain.RuleCategory;
import com.itmasters.icon.api.rule.dto.SensorDto;
import com.itmasters.icon.common.domain.RuleDomain;
import com.itmasters.icon.common.domain.rule.RuleOperator;
import com.itmasters.icon.common.domain.rule.condition.RuleCondition;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 센서 엔티티 - 이벤트 감지 조건
 * RuleCondition을 JSON 문자열로 저장
 * 테이블명: sensors (기존 rules)
 */
@Entity
@Table(name = "sensors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SensorEntity extends Auditable {
    @Id
    @Column(name = "sensor_id", nullable = false)
    private String sensorId;

    @Column(name = "sensor_name", nullable = false, length = 255)
    private String sensorName;

    // v4.0 schema: no 'category' column in sensors table
    @Transient
    private RuleCategory category;

    // v4: 도메인은 where_json/필드에서 유추. 별도 컬럼 미사용
    @Transient
    private RuleDomain domain;

    // v4: 연산자는 where_json 내부에 포함. 별도 컬럼 미사용
    @Transient
    private RuleOperator operator;


    // v4 schema: condition is stored in where_json; legacy column condition_data is absent
    @Transient
    private String conditionData;  // JSON 문자열 (비영속)

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "version", nullable = false)
    @Version  // JPA Optimistic Lock
    private Integer version = 1;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "where_json", columnDefinition = "jsonb")
    private String whereJson; // 조건 JSON (신규)

    // === Anchor/Where setters (for new schema) ===
    public void updateWhereJson(String whereJson) {
        this.whereJson = whereJson;
    }

    // === 정적 팩토리 메서드 ===

    /**
     * 새 센서 생성 - 필수 필드만 (기존 방식 - 하위 호환성)
     */
    public static SensorEntity create(String sensorName, RuleCategory category, RuleCondition condition, ObjectMapper objectMapper) {
        SensorEntity entity = new SensorEntity();
        entity.sensorName = sensorName;
        entity.category = category;
        entity.setConditionFromObject(condition, objectMapper);
        entity.version = null;  // JPA가 새 엔티티로 인식하도록 null로 설정
        entity.isActive = true;
        return entity;
    }

    /**
     * 새 센서 생성 - 새로운 도메인/연산자 방식
     */
    public static SensorEntity createWithDomain(String sensorName, RuleDomain domain, RuleOperator operator, RuleCondition condition, ObjectMapper objectMapper) {
        SensorEntity entity = new SensorEntity();
        entity.sensorName = sensorName;
        entity.domain = domain;
        entity.operator = operator;
        entity.setConditionFromObject(condition, objectMapper);
        entity.version = null;
        entity.isActive = true;
        return entity;
    }

    /**
     * ID 할당 (Repository에서 사용)
     */
    public void assignId(String id) {
        if (this.sensorId != null) {
            throw new IllegalStateException("ID가 이미 할당되었습니다");
        }
        this.sensorId = id;
    }


    /**
     * 센서 조건 업데이트
     */
    public void updateCondition(RuleCondition newCondition, ObjectMapper objectMapper) {
        setConditionFromObject(newCondition, objectMapper);
    }

    /**
     * 현재 조건과 새 조건이 다른지 확인
     */
    public boolean isChangeCondition(RuleCondition newCondition, ObjectMapper objectMapper) {
        RuleCondition currentCondition = getConditionAsObject(objectMapper);
        if (currentCondition == null && newCondition == null) {
            return false;
        }
        if (currentCondition == null || newCondition == null) {
            return true;
        }
        return !currentCondition.equals(newCondition);
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateName(String sensorName) {
        if (sensorName == null || sensorName.trim().isEmpty()) {
            throw new IllegalArgumentException("Sensor name cannot be empty");
        }
        this.sensorName = sensorName;
    }

    public void updateCategory(RuleCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        this.category = category;
    }

    public void updateDomain(RuleDomain domain) {
        this.domain = domain;
    }

    public void updateOperator(RuleOperator operator) {
        this.operator = operator;
    }

    public void updateDomainAndOperator(RuleDomain domain, RuleOperator operator) {
        this.domain = domain;
        this.operator = operator;
    }

    public void updateConditionData(String conditionData) {
        this.conditionData = conditionData;
    }

    // === JSON 변환 헬퍼 메서드 ===

    /**
     * conditionData를 RuleCondition 객체로 변환
     */
    public RuleCondition getConditionAsObject(ObjectMapper objectMapper) {
        if (conditionData == null || conditionData.trim().isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(conditionData, RuleCondition.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse condition data", e);
        }
    }

    /**
     * RuleCondition 객체를 conditionData JSON 문자열로 설정
     */
    public void setConditionFromObject(RuleCondition condition, ObjectMapper objectMapper) {
        if (condition == null) {
            this.conditionData = null;
            return;
        }

        try {
            this.conditionData = objectMapper.writeValueAsString(condition);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize condition", e);
        }
    }

    // === DTO 변환 메서드 ===

    /**
     * SensorDto.Info로 변환
     */
    public SensorDto.Info toInfo(ObjectMapper objectMapper) {
        return SensorDto.Info.from(this, objectMapper);
    }

    /**
     * SensorDto.Simple로 변환
     */
    public SensorDto.Simple toSimple() {
        return SensorDto.Simple.from(this);
    }
}

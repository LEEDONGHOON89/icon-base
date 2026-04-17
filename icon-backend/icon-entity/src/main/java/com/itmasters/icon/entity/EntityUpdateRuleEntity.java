package com.itmasters.icon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity Update Rule 엔티티

 * 시나리오 탐지 또는 이벤트 발생 시 entity_attributes를 업데이트하는 규칙을 정의합니다.

 * 트리거 타입:
 * - SCENARIO: 시나리오 탐지 시 엔티티 업데이트
 * - EVENT: 이벤트 발생 시 엔티티 업데이트

 * 예시 1 (SCENARIO 트리거):
 * - trigger_type: "SCENARIO"
 * - scenario_id: "S_CUS013_휴면계좌_탐지"
 * - entity_type: "CUSTOMER"
 * - field_name: "IS_DORMANT_ACCOUNT"
 * - field_value: "true"
 * → S_CUS013 시나리오가 탐지되면 해당 고객의 IS_DORMANT_ACCOUNT를 true로 설정

 * 예시 2 (EVENT 트리거):
 * - trigger_type: "EVENT"
 * - data_source_id: "DS_SECURITY_TRADE"
 * - event_condition: "{\"trade_type\": \"SELL\"}"
 * - entity_type: "HOLDING"
 * - entity_id_expression: "CONCAT(customer_id, '_', security_id)"
 * - update_type: "DECREMENT"
 * - field_name: "quantity"
 * - field_value_expression: "trade_quantity"
 * → 매도 거래 발생 시 보유 수량 감소
 */
@Entity
@Table(name = "entity_update_rules")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityUpdateRuleEntity {

    @Id
    @Column(name = "entity_update_rule_id", length = 50)
    private String entityUpdateRuleId;

    /**
     * 트리거 타입
     * SCENARIO: 시나리오 탐지 시
     * EVENT: 이벤트 발생 시
     */
    @Column(name = "trigger_type", length = 20, nullable = false)
    @Builder.Default
    private String triggerType = "SCENARIO";

    /**
     * 시나리오 ID (trigger_type='SCENARIO'일 때)
     */
    @Column(name = "scenario_id", length = 50)
    private String scenarioId;

    /**
     * 데이터소스 ID (trigger_type='EVENT'일 때)
     */
    @Column(name = "data_source_id", length = 50)
    private String dataSourceId;

    /**
     * 이벤트 조건 (trigger_type='EVENT'일 때)
     * JSON 형식, 예: {"trade_type": "SELL"}
     */
    @Column(name = "event_condition", columnDefinition = "TEXT")
    private String eventCondition;

    /**
     * 엔티티 타입 (어떤 엔티티 타입의)
     * 예: CUSTOMER, ACCOUNT, DEVICE, HOLDING, SECURITY
     */
    @Column(name = "entity_type", length = 50, nullable = false)
    private String entityType;

    /**
     * 엔티티 ID 표현식 (동적 계산)
     * 예: "customer_id", "CONCAT(customer_id, '_', security_id)"
     * null이면 groupKey 사용 (SCENARIO 트리거의 기본 동작)
     */
    @Column(name = "entity_id_expression", length = 200)
    private String entityIdExpression;

    /**
     * 엔티티 필터 (다중 엔티티 업데이트 시)
     * JSON 형식, 예: {"security_id": "$security_id"}
     */
    @Column(name = "entity_filter", columnDefinition = "TEXT")
    private String entityFilter;

    /**
     * 업데이트 타입
     * SET: 값 설정
     * INCREMENT: 값 증가
     * DECREMENT: 값 감소
     * DELETE: 엔티티 삭제
     */
    @Column(name = "update_type", length = 20)
    @Builder.Default
    private String updateType = "SET";

    /**
     * 필드 이름 (어떤 필드를)
     * 예: IS_DORMANT_ACCOUNT, quantity, current_rating
     */
    @Column(name = "field_name", length = 100)
    private String fieldName;

    /**
     * 필드 값 (정적 값, update_type='SET'일 때)
     * 예: "true", "HIGH", "2024-01-01"
     */
    @Column(name = "field_value", length = 500)
    private String fieldValue;

    /**
     * 필드 값 표현식 (동적 계산)
     * 예: "trade_quantity", "quantity - trade_quantity"
     */
    @Column(name = "field_value_expression", length = 500)
    private String fieldValueExpression;

    /**
     * 필드 타입
     * 예: STRING, BOOLEAN, NUMBER, DATE
     */
    @Column(name = "field_type", length = 20)
    private String fieldType;

    /**
     * 엔티티 삭제 조건
     * 예: "quantity <= 0"
     */
    @Column(name = "delete_entity_if", length = 200)
    private String deleteEntityIf;

    /**
     * 설명
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 활성 여부
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 등록 일시
     */
    @Column(name = "reg_dt", nullable = false, updatable = false)
    private LocalDateTime regDt;

    /**
     * 수정 일시
     */
    @Column(name = "upd_dt", nullable = false)
    private LocalDateTime updDt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (regDt == null) {
            regDt = now;
        }
        updDt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updDt = LocalDateTime.now();
    }
}

package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.itmasters.icon.common.domain.aggregate.AggregateOperator;

/**
 * Rules definition entity (룰 정의)
 * - 대상 EVENT 센서(predicate)를 창(window)/임계(threshold)/앵커(anchor) 등으로 집계하는 정의
 * - 테이블명: rules
 */
@Entity
@Table(name = "rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleEntity {

    @Id
    @Column(name = "rule_id", length = 50)
    private String ruleId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "operator", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private AggregateOperator operator; // COUNT_WITHIN | SUM_WITHIN | DISTINCT_COUNT_WITHIN | SEQUENCE_WITHIN

    @Column(name = "predicate_sensor_id", length = 50)
    private String predicateSensorId; // 대상 EVENT 센서

    @Column(name = "prev_sensor_id", length = 50)
    private String prevSensorId; // 시퀀스 prev(A)

    @Column(name = "next_sensor_id", length = 50)
    private String nextSensorId; // 시퀀스 next(B)

    @Column(name = "anchor_sensor_id", length = 50)
    private String anchorSensorId; // 앵커 이벤트 센서(옵션)

    @Column(name = "window_minutes")
    private Integer windowMinutes;

    @Column(name = "threshold_count")
    private BigDecimal thresholdCount;

    @Column(name = "threshold_amount")
    private BigDecimal thresholdAmount;

    @Column(name = "dedup_minutes")
    private Integer dedupMinutes;

    /**
     * 직접 필터 조건 (predicate_sensor_id 없이 where 조건 직접 지정)
     * Sensor의 where_json과 동일한 형식 사용
     * 예: {"approval_datetime": null} 또는 [{"fieldName": "is_active", "operator": "EQUALS", "value": true}]
     * predicate_sensor_id가 null이고 where_json이 있으면 이 조건으로 필터링
     */
    @Column(name = "where_json", columnDefinition = "jsonb")
    private String whereJson;

    /**
     * 수치 집계 대상 필드명(예: transaction_amount)
     * - SUM/AVG/MAX/MIN 등에서 값을 추출할 때 사용
     */
    @Column(name = "aggregation_field", length = 100)
    private String aggregationField;

    /**
     * 그룹핑 기준 필드 목록(text[])
     * - COUNT/SUM/AVG/MIN/MAX 등 집계에서 동일 그룹(동일 값 조합) 단위로 윈도우 내 이벤트를 묶어 계산
     * - 예) 동일 수취계좌 3회 → receiver_account
     * - 여러 필드 지정 시 (필드1, 필드2) 튜플로 그룹핑
     * - 기존 group_by_field(단일)에서 승격됨
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.ARRAY)
    @Column(name = "group_by_fields", columnDefinition = "text[]")
    private String[] groupByFields;

    /**
     * 집계 평가 모드
     * - SINGLE_ROW: 단일 행 평가 (현재 이벤트만 체크, window 조회 없음)
     * - WINDOW: 시간 윈도우 집계 (기본값, 시간 범위 내 여러 이벤트 집계)
     */
    @Column(name = "evaluation_mode", length = 20)
    private String evaluationMode = "WINDOW";

    @Transient
    public String getGroupByField() {
        if (groupByFields != null && groupByFields.length > 0) return groupByFields[0];
        return null;
    }

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

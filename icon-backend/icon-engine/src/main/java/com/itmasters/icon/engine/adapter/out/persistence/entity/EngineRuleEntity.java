package com.itmasters.icon.engine.adapter.out.persistence.entity;

import com.itmasters.icon.common.domain.RuleDomain;
import com.itmasters.icon.common.domain.rule.RuleOperator;
import jakarta.persistence.*;
import lombok.*;

/**
 * Engine 모듈용 Rule 엔티티
 * API 모듈의 RuleEntity와 동일한 테이블 매핑
 */
@Entity
@Table(name = "rules")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EngineRuleEntity {
    
    @Id
    @Column(name = "rule_id", nullable = false)
    private String ruleId;
    
    @Column(name = "name", nullable = false)
    private String ruleName;
    
    // v4: domain is derived from where_json; not stored as column
    @Transient
    private RuleDomain domain;
    
    // v4: operator is inside where_json; not stored as column
    @Transient
    private RuleOperator operator;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "where_json", columnDefinition = "jsonb")
    private String whereJson; // JSON for conditions (required)

    @Column(name = "predicate_sensor_id")
    private String predicateSensorId;

    @Column(name = "anchor_sensor_id")
    private String anchorSensorId;

    @Column(name = "prev_sensor_id")
    private String prevSensorId;

    @Column(name = "next_sensor_id")
    private String nextSensorId;

    @Column(name = "operator")
    private String operatorName;
}

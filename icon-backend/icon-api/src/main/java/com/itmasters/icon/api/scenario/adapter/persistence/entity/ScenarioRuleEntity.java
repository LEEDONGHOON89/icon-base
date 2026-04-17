package com.itmasters.icon.api.scenario.adapter.persistence.entity;

import com.itmasters.icon.entity.Auditable;
import com.itmasters.icon.common.domain.scenario.ScenarioOperator;
import jakarta.persistence.*;
import lombok.*;

/**
 * 시나리오-집계 매핑 Entity (UI 상 "집계")
 */
@Entity
@Table(name = "scenario_rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScenarioRuleEntity extends Auditable {
    @Id
    @Column(name = "scenario_aggregate_id", nullable = false, length = 50)
    private String scenarioRuleId; // 필드명은 scenarioRuleId지만 DB 컬럼은 scenario_aggregate_id

    @Column(name = "scenario_id", nullable = false, length = 50)
    private String scenarioId;

    @Column(name = "rule_id", nullable = false, length = 50)
    private String ruleId; // aggregateId를 저장 (호환 목적)

    @Column(name = "order_no")
    private Integer orderNo;

    @Column(name = "operator", length = 10)
    @Enumerated(EnumType.STRING)
    private ScenarioOperator operator;

}

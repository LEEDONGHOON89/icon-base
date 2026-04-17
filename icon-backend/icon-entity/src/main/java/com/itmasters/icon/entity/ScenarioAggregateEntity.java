package com.itmasters.icon.entity;

import com.itmasters.icon.common.domain.scenario.ScenarioOperator;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scenario_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioAggregateEntity {

    @Id
    @Column(name = "scenario_aggregate_id", length = 50)
    private String scenarioAggregateId; // DB 컬럼명과 일치

    @Column(name = "scenario_id", nullable = false, length = 50)
    private String scenarioId;

    @Column(name = "rule_id", nullable = false, length = 50)
    private String aggregateId; // 필드명은 aggregateId로 유지하되 DB 컬럼은 rule_id

    @Column(name = "order_no")
    private Integer orderNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", length = 10)
    private ScenarioOperator operator;


    /**
     * icon-engine 호환성: operator를 String으로 반환
     */
    public String getOperatorAsString() {
        return operator != null ? operator.name() : null;
    }
}


package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 시나리오-룰 매핑 Entity (Engine용)
 * API 모듈의 ScenarioRuleEntity와 동일한 구조
 */
@Entity
@Table(name = "scenario_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EngineScenarioRuleEntity {
    
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

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 시나리오 내 규칙 간 논리 연산자
     */
    public enum ScenarioOperator {
        AND("AND", "그리고"),
        OR("OR", "또는"),
        ANCHOR("ANCHOR", "기준");

        private final String code;
        private final String displayName;
        
        ScenarioOperator(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}

package com.itmasters.icon.api.rule.domain;

import com.itmasters.icon.api.common.domain.type.LogicalOperatorType;
import com.itmasters.icon.api.rule.adapter.persistence.entity.SensorEntity;
import com.itmasters.icon.api.scenario.domain.Scenario;
import lombok.*;

/**
 * 시나리오와 규칙의 매핑 도메인
 * 시나리오 내에서 규칙들의 조합 방법을 정의
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ScenarioRule {
    
    private Long id;
    private String scenarioId;
    private String ruleId;
    private String nestedScenarioId; // 중첩 시나리오 지원
    private LogicalOperatorType logicalOperator;
    private Integer orderNo;
    
    // 연관 도메인 (조회용)
    private SensorEntity rule;
    private Scenario nestedScenario;
    
    public static ScenarioRule createWithRule(
            String scenarioId,
            String ruleId,
            LogicalOperatorType logicalOperator,
            Integer orderNo) {
        
        var scenarioRule = new ScenarioRule();
        scenarioRule.scenarioId = scenarioId;
        scenarioRule.ruleId = ruleId;
        scenarioRule.logicalOperator = logicalOperator;
        scenarioRule.orderNo = orderNo;
        
        return scenarioRule;
    }
    
    public static ScenarioRule createWithNestedScenario(
            String scenarioId,
            String nestedScenarioId,
            LogicalOperatorType logicalOperator,
            Integer orderNo) {
        
        var scenarioRule = new ScenarioRule();
        scenarioRule.scenarioId = scenarioId;
        scenarioRule.nestedScenarioId = nestedScenarioId;
        scenarioRule.logicalOperator = logicalOperator;
        scenarioRule.orderNo = orderNo;
        
        return scenarioRule;
    }
    
    public boolean isRule() {
        return ruleId != null;
    }
    
    public boolean isNestedScenario() {
        return nestedScenarioId != null;
    }
    
    /**
     * 매핑 유효성 검증
     */
    public boolean isValid() {
        // 규칙 또는 중첩 시나리오 중 하나만 설정되어야 함
        boolean hasOneReference = (ruleId != null) ^ (nestedScenarioId != null);
        
        return scenarioId != null
                && hasOneReference
                && logicalOperator != null
                && orderNo != null && orderNo >= 0;
    }
}
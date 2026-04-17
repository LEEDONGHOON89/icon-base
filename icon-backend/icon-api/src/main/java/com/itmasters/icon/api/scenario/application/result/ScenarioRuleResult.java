package com.itmasters.icon.api.scenario.application.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.api.rule.adapter.persistence.entity.SensorEntity;
import com.itmasters.icon.common.domain.scenario.ScenarioOperator;
import com.itmasters.icon.api.scenario.domain.ScenarioRule;
import com.itmasters.icon.common.domain.rule.condition.RuleCondition;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시나리오-규칙 매핑 결과 DTO
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ScenarioRuleResult {
    private String ruleId;
    private String ruleName;
    private String category;
    private String fieldName;
    private String operator;
    private String operatorSymbol;
    private Object value;
    private boolean isActive;
    private Integer orderNo;
    private ScenarioOperator scenarioOperator; // 논리 연산자 (AND/OR)


    /**
     * SensorEntity와 ScenarioRule 정보를 합쳐서 생성
     */
    public static ScenarioRuleResult from(SensorEntity rule, ScenarioRule scenarioRule, ObjectMapper objectMapper) {
        RuleCondition condition = rule.getConditionAsObject(objectMapper);
        return new ScenarioRuleResult(
                rule.getSensorId(),
                rule.getSensorName(),
                rule.getCategory().name(),
                condition != null ? condition.getFieldName() : null,
                condition != null ? condition.getOperator().name() : null,
                condition != null ? condition.getOperator().getSymbol() : null,
                condition != null ? condition.getValue() : null,
                rule.getIsActive(),
                scenarioRule.getOrderNo(),
                scenarioRule.getOperator()
        );
    }
}

package com.itmasters.icon.engine.dto;

import com.itmasters.icon.common.domain.RuleDomain;
import com.itmasters.icon.common.domain.rule.RuleOperator;
import com.itmasters.icon.common.domain.rule.condition.RuleCondition;
import lombok.Builder;
import lombok.Getter;

/**
 * Rule 정의를 담는 엔진용 DTO
 * API 모듈로부터 이 형태로 데이터를 전달받음
 */
@Getter
@Builder
public class RuleDto {
    private String ruleId;
    private String ruleName;
    private RuleDomain domain;
    private RuleOperator operator;
    private RuleCondition condition;
    private Boolean isActive;
}

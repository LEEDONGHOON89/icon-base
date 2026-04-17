package com.itmasters.icon.engine.dto;

import com.itmasters.icon.common.domain.scenario.ScenarioOperator;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Scenario 정의를 담는 엔진용 DTO
 * API 모듈로부터 이 형태로 데이터를 전달받음
 */
@Getter
@Builder
public class ScenarioDto {
    private String scenarioId;
    private String scenarioName;
    private Boolean isActive;
    private List<ScenarioRuleDto> rules;

    /**
     * 시나리오에 포함된 개별 규칙의 정보를 담는 DTO
     */
    @Getter
    @Builder
    public static class ScenarioRuleDto {
        private String ruleId;
        private ScenarioOperator operator;
    }
}

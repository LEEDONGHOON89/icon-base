package com.itmasters.icon.api.scenario.application.result;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 시나리오 결과 DTO
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ScenarioResult {
    private String scenarioId; // TSID
    private String scenarioName;
    private String description;
    private Boolean isActive;
    private LocalDateTime regDt;
    private List<ScenarioRuleResult> rules;

    /**
     * 간단한 시나리오 결과 (규칙 제외)
     */
    public static ScenarioResult of(String scenarioId, String scenarioName,
                                    String description, boolean isActive, LocalDateTime regDt) {
        return new ScenarioResult(scenarioId
                , scenarioName
                , description
                , isActive
                , regDt
                , new ArrayList<>());
    }

    /**
     * 완전한 시나리오 결과 (규칙 포함)
     */
    public static ScenarioResult withRules(String scenarioId, String scenarioName,
                                           String description, boolean isActive, LocalDateTime regDt,
                                           List<ScenarioRuleResult> rules) {
        return new ScenarioResult(scenarioId
                , scenarioName
                , description
                , isActive
                , regDt
                , rules);
    }
}
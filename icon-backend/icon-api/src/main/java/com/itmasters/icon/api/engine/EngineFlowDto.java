package com.itmasters.icon.api.engine;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class EngineFlowDto {
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleInfo {
        private String ruleId;
        private String ruleName;
        private String whereClause;
        private String ruleDomain;
        private String operator;
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AggregateInfo {
        private String aggregateId;
        private String aggregateName;
        private String predicateRuleId;
        private String operation;
        private String targetColumn;
        private String withinUnit;
        private Integer withinValue;
        private String groupByField;
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScenarioInfo {
        private String scenarioId;
        private String scenarioName;
        private String description;
        private String severity;
        private String entityFilterJson;
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScenarioAggregateInfo {
        private String scenarioId;
        private String aggregateId;
        private Double threshold;
        private String operator;
        private Integer displayOrder;
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityUpdateRuleInfo {
        private String entityUpdateRuleId;
        private String scenarioId;
        private String entityType;
        private String fieldName;
        private String fieldValue;
        private String fieldType;
        private String description;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private List<RuleInfo> rules;
        private List<AggregateInfo> aggregates;
        private List<ScenarioInfo> scenarios;
        private List<ScenarioAggregateInfo> scenarioAggregates;
        private List<EntityUpdateRuleInfo> entityUpdateRules;
    }
}

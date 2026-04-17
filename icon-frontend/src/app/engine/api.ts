import api from "@/lib/api";

export interface Rule {
  ruleId: string;
  ruleName: string;
  whereClause: string;
  ruleDomain: string;
  operator: string;
}

export interface Aggregate {
  ruleId: string;
  ruleName: string;
  predicateRuleId: string | null;
  operation: string;
  targetColumn: string | null;
  withinUnit: string | null;
  withinValue: number | null;
  groupByField: string;
}

export interface Scenario {
  scenarioId: string;
  scenarioName: string;
  description: string | null;
  severity: string;
  entityFilterJson: string | null;
}

export interface ScenarioAggregate {
  scenarioId: string;
  ruleId: string;
  threshold: number;
  operator: string;
  displayOrder: number;
}

export interface EntityUpdateRule {
  entityUpdateRuleId: string;
  scenarioId: string;
  entityType: string;
  fieldName: string;
  fieldValue: string;
  fieldType: string | null;
  description: string | null;
}

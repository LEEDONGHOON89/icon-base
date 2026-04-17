import api from "@/lib/api";

export interface DetectedAggregate {
  groupKey: string;
  ruleId: string;
  ruleName?: string;
  operator: string;
  windowMinutes: number;
  matchedCount?: string | number | null;
  thresholdCount?: string | number | null;
  detectedAt: string; // ISO datetime
  mappedStorageId?: number | null;
}

export interface DetectedScenario {
  scenarioId: string;
  scenarioName?: string;
  groupKey: string;
  detectedAt: string; // ISO datetime
  windowStart?: string | null;
  windowEnd?: string | null;
  aggregateCount?: number | null;
  passedCount?: number | null;
  allPassed?: boolean | null;
  detectionAreaId?: string | null;   // 탐지영역 ID
  detectionAreaName?: string | null; // 탐지영역명
  transactionId?: string | null;     // 트랜잭션 ID
}

export interface ScenarioAggregateHit {
  ruleId: string;
  ruleName?: string | null;
  operator?: string | null;
  matchedCount?: string | number | null;
  thresholdCount?: string | number | null;
  windowMinutes?: number | null;
  detectedAt?: string | null;
  predicateRuleId?: string | null;
  predicateRuleName?: string | null;
  pass: boolean;
}

export interface ScenarioDetail {
  scenarioId: string;
  scenarioName?: string | null;
  groupKey: string;
  detectedAt: string;
  windowStart?: string | null;
  windowEnd?: string | null;
  aggregates: ScenarioAggregateHit[];
}

export interface DetectedRule {
  detectRuleId: number;
  detectRuleResultId: number;
  ruleId?: string;
  ruleName?: string;
  groupKey?: string | null;
  mappedStorageId?: number | null;
  rowNumber?: number | null;
  detectedAt?: string | null;
  matchedFields?: Record<string, unknown> | null;
}

export interface ExecutionSummary {
  landingRecordId: number;
  mappedStorageId?: number | null;
  execDsMpId: number;
  dataSourceId: string;
  sourceType?: string | null;
  executionMode?: string | null;
  status: string;
  startDt?: string | null;
  completeAt?: string | null;
  totalRows?: number | null;
  executedBy?: string | null;
  rowIndex?: number | null;
  batchKey?: string | null;
  extractedAt?: string | null;
  ingestionStatus?: string | null;
  ingestionMessage?: string | null;
  ruleCount: number;
  aggregateCount: number;
  scenarioCount: number;
}

export interface ExecutionPage {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  items: ExecutionSummary[];
}

export interface ExecutionDetail {
  summary: ExecutionSummary;
  groupKeys: string[];
  rules: DetectedRule[];
  aggregates: DetectedAggregate[];
  scenarios: DetectedScenario[];
  rawPayload?: Record<string, unknown> | null;
  mappedRow?: Record<string, unknown> | null;
}

export async function fetchDetectedAggregates(params: {
  groupKey?: string;
  startDate?: string; // YYYY-MM-DD
  endDate?: string;   // YYYY-MM-DD
  limit?: number;
}): Promise<DetectedAggregate[]> {
  const res = await api.get<DetectedAggregate[]>("/api/v1/analytics/aggregates", {
    params,
  });
  return res.data;
}

export async function fetchDetectedScenarios(params: {
  groupKey?: string;
  startDate?: string; // YYYY-MM-DD
  endDate?: string;   // YYYY-MM-DD
  limit?: number;
}): Promise<DetectedScenario[]> {
  const res = await api.get<DetectedScenario[]>("/api/v1/analytics/scenarios", {
    params,
  });
  return res.data;
}

export async function fetchScenarioDetail(params: { groupKey: string; scenarioId: string; detectedAt: string }) {
  const res = await api.get<ScenarioDetail>("/api/v1/analytics/scenarios/detail", { params });
  return res.data;
}

export async function fetchExecutions(params: { page?: number; size?: number } = {}): Promise<ExecutionPage> {
  const res = await api.get<ExecutionPage>("/api/v1/analytics/executions", { params });
  return res.data;
}

export async function fetchExecutionDetail(landingRecordId: number): Promise<ExecutionDetail> {
  const res = await api.get<ExecutionDetail>(`/api/v1/analytics/executions/${landingRecordId}`);
  return res.data;
}

export interface DetectionStats {
  totalRules: number;
  totalAggregates: number;
  totalScenarios: number;
}

export interface EntityProfile {
  entityId: string;
  entityType: string | null;
  attributes: Record<string, unknown> | null;
  detectedRules: DetectedRule[];
  detectedAggregates: DetectedAggregate[];
  detectedScenarios: DetectedScenario[];
  stats: DetectionStats;
}

export async function fetchEntityProfile(groupKey: string, limit?: number): Promise<EntityProfile> {
  const res = await api.get<EntityProfile>(`/api/v1/analytics/entities/${groupKey}`, {
    params: { limit },
  });
  return res.data;
}

// 실시간 탐지 시뮬레이션
export interface SimulationRequest {
  dataSourceId: string;
  executedBy: string;
  row: Record<string, any>;
}

export interface SimulationResponse {
  execDsMpId: number | null;
  savedEventStreams: number;
  updatedEntityAttributes: number;
  ruleResults: number;
  savedAggregates: number;
  savedScenarios: number;
  success: boolean;
  errorMessage: string | null;
}

export async function runSimulation(request: SimulationRequest): Promise<SimulationResponse> {
  const res = await api.post<SimulationResponse>("/api/v1/detections/realtime/run", request);
  return res.data;
}

// Aggregate Operator types and labels
export type AggregateOperator = "COUNT" | "SUM" | "MAX" | "MIN" | "AVG";

export function labelAggregateOperator(op: AggregateOperator | string | null | undefined): string {
  if (!op) return "-";
  switch (op) {
    case "COUNT":
      return "건수";
    case "SUM":
      return "합계";
    case "MAX":
      return "최대";
    case "MIN":
      return "최소";
    case "AVG":
      return "평균";
    default:
      return op;
  }
}

// Rule (formerly Aggregate) Definition
export interface AggregateDef {
  ruleId: string; // formerly aggregateId
  sensorName: string; // 백엔드에서 sensorName으로 반환
  name?: string; // 호환성을 위해 옵셔널로 유지
  operator?: AggregateOperator | string;
  windowMinutes?: number;
  thresholdCount?: number;
  thresholdAmount?: number;
  predicateSensorId?: string; // formerly predicateRuleId
  groupByField?: string;
  isActive?: boolean;
}

// Fetch all rules (formerly aggregates)
export async function fetchAggregates(): Promise<AggregateDef[]> {
  const res = await api.get<{ data: AggregateDef[] }>("/api/v1/sensors");
  return res.data.data || [];
}

// 엔티티 행적 조회 타입
export interface EntityHistoryStats {
  totalDetections: number;
  scenarioCount: number;
  aggregateCount: number;
  ruleCount: number;
  totalActivities: number;
  firstActivityAt: string | null;
  lastActivityAt: string | null;
}

export interface DetectionRecord {
  type: "SCENARIO" | "AGGREGATE" | "RULE";
  detectionId: string;
  detectionName: string | null;
  groupKey: string;
  riskLevel: string | null;
  detectedAt: string;
  details: Record<string, unknown>;
}

export interface ActivityLog {
  eventStreamId: number;
  groupKey: string | null;
  dataSourceId: string | null;
  transactionId: string | null;
  eventDt: string;
  eventData: Record<string, unknown>;
}

export interface EntityHistory {
  entityId: string;
  entityType: string | null;  // 엔티티 타입 (예: CUSTOMER, PRODUCT 등)
  stats: EntityHistoryStats;
  detections: DetectionRecord[];
  activities: ActivityLog[];
}

// 엔티티 행적 조회 API
export async function fetchEntityHistory(
  entityId: string,
  params?: { detectionLimit?: number; activityLimit?: number }
): Promise<EntityHistory> {
  const res = await api.get<EntityHistory>(`/api/v1/analytics/entity-history/${entityId}`, {
    params,
  });
  return res.data;
}

// 엔티티 그래프 (연결 관계)
export interface GraphNode {
  id: string;           // "CUSTOMER:CUS001"
  label: string;        // "CUS001"
  type: string;         // "CUSTOMER"
  data: Record<string, unknown> | null;
  totalConnections: number | null;      // 전체 연결 개수
  displayedConnections: number | null;  // 표시된 연결 개수
}

export interface GraphEdge {
  id: string;           // "CUSTOMER:CUS001->TRADED->PRODUCT:PRD001"
  source: string;       // "CUSTOMER:CUS001"
  target: string;       // "PRODUCT:PRD001"
  label: string;        // "TRADED"
  type: string;         // "TRADED"
  data: Record<string, unknown> | null;
}

export interface EntityGraph {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

// 엔티티 그래프 조회 API
export async function fetchEntityGraph(
  entityType: string,
  entityId: string,
  depth?: number,
  limit?: number
): Promise<EntityGraph> {
  const res = await api.get<EntityGraph>(`/api/v1/analytics/entities/${entityType}/${entityId}/graph`, {
    params: {
      depth: depth ?? 2,
      limit: limit ?? 10
    },
  });
  return res.data;
}

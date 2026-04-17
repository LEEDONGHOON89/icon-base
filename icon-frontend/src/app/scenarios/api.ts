import api from "@/lib/api";

// 시나리오 연산자 타입
export type ScenarioOperator = 'AND' | 'OR';

// 시나리오-집계 매핑 정보
export interface ScenarioRuleMapping {
  ruleId: string;
  orderNo?: number;
  operator?: ScenarioOperator; // 첫 번째 집계는 operator가 없을 수 있음
}

// 시나리오-집계 상세 정보 (API 응답용)
export interface ScenarioRuleDetail {
  ruleId: string; // aggregateId
  ruleName: string; // aggregate name
  orderNo: number;
  operator: ScenarioOperator; // 논리 연산자 (AND/OR)

  // 집계 상세 정보
  windowMinutes?: number; // 집계 시간 창 (분 단위)
  thresholdCount?: number; // 카운트 임계값
  thresholdAmount?: number; // 금액 임계값
  aggregateOperator?: string; // 집계 연산자 (SUM_WITHIN, COUNT_WITHIN 등)
}

// 시나리오 생성 요청
export interface CreateScenarioRequest {
  scenarioName: string;
  description?: string;
  riskLevelId?: string; // 위험 레벨 ID
  detectionAreaId?: string; // 탐지영역 ID
  primaryEntityType?: string; // 주요 엔티티 타입
  entityFilterJson?: string;
  // 엔진 설정 필드
  dedupMinutes?: number; // 중복 제거 창 (분)
  rules?: ScenarioRuleMapping[];
}

// 시나리오 수정 요청
export interface UpdateScenarioRequest {
  scenarioName?: string;
  description?: string;
  riskLevelId?: string; // 위험 레벨 ID
  detectionAreaId?: string; // 탐지영역 ID
  primaryEntityType?: string; // 주요 엔티티 타입
  entityFilterJson?: string;
  isActive?: boolean;
  // 엔진 설정 필드
  dedupMinutes?: number; // 중복 제거 창 (분)
  rules?: ScenarioRuleMapping[];
}

// 시나리오에 집계 추가 요청
export interface AddRuleToScenarioRequest {
  ruleId: string;
  orderNo?: number;
  operator: ScenarioOperator;
}

// 시나리오 응답 (집계 정보 포함)
export interface ScenarioWithRules {
  scenarioId: string;
  scenarioName: string;
  description?: string;
  riskLevelId?: string; // 위험 레벨 ID
  riskLevelName?: string; // 위험 레벨명
  detectionAreaId?: string; // 탐지영역 ID
  detectionAreaName?: string; // 탐지영역명
  primaryEntityType?: string; // 주요 엔티티 타입
  entityFilterJson?: string;
  isActive: boolean;
  regDt: string; // 생성일시
  // 엔진 설정 필드
  dedupMinutes?: number; // 중복 제거 창 (분)
  rules: ScenarioRuleDetail[]; // 집계 상세 정보 + operator
  createdAt?: string;
  updatedAt?: string;
}

export interface ResponseList<T> { data: T[]; total?: number }

// 시나리오 기본 정보
export interface Scenario {
  scenarioId: string;
  scenarioName: string;
  description?: string;
  riskLevelId?: string; // 위험 레벨 ID
  riskLevelName?: string; // 위험 레벨명
  detectionAreaId?: string; // 탐지영역 ID
  detectionAreaName?: string; // 탐지영역명
  primaryEntityType?: string; // 주요 엔티티 타입
  entityFilterJson?: string;
  isActive: boolean;
  regDt: string; // 생성일시
  // 엔진 설정 필드
  dedupMinutes?: number; // 중복 제거 창 (분)
  createdAt?: string;
  updatedAt?: string;
}

// 시나리오 API 함수들

// 시나리오 생성
export const createScenario = async (data: CreateScenarioRequest): Promise<ScenarioWithRules> => {
  const response = await api.post('/api/v1/scenarios', data);
  return response.data.data;
};

// 시나리오 조회
export const fetchScenario = async (scenarioId: string): Promise<ScenarioWithRules> => {
  const response = await api.get(`/api/v1/scenarios/${scenarioId}`);
  return response.data.data;
};

// 시나리오 목록 조회
export const fetchScenarios = async (): Promise<ScenarioWithRules[]> => {
  const response = await api.get('/api/v1/scenarios');
  return response.data.data;
};

export const fetchScenariosPaged = async (params: Record<string, any>): Promise<ResponseList<ScenarioWithRules>> => {
  const response = await api.get('/api/v1/scenarios', { params });
  return response.data;
};

// 시나리오 수정
export const updateScenario = async (
  scenarioId: string, 
  data: UpdateScenarioRequest
): Promise<ScenarioWithRules> => {
  const response = await api.put(`/api/v1/scenarios/${scenarioId}`, data);
  return response.data.data;
};

// 시나리오 활성화
export const activateScenario = async (scenarioId: string): Promise<ScenarioWithRules> => {
  const response = await api.patch(`/api/v1/scenarios/${scenarioId}/activate`);
  return response.data.data;
};

// 시나리오 비활성화
export const deactivateScenario = async (scenarioId: string): Promise<ScenarioWithRules> => {
  const response = await api.patch(`/api/v1/scenarios/${scenarioId}/deactivate`);
  return response.data.data;
};

// 시나리오 삭제
export const deleteScenario = async (scenarioId: string): Promise<void> => {
  await api.delete(`/api/v1/scenarios/${scenarioId}`);
};

// 시나리오에 집계 추가
export const addRuleToScenario = async (
  scenarioId: string, 
  data: AddRuleToScenarioRequest
): Promise<ScenarioWithRules> => {
  const response = await api.post(`/api/v1/scenarios/${scenarioId}/rules`, data);
  return response.data.data;
};

// 시나리오에서 집계 제거
export const removeRuleFromScenario = async (
  scenarioId: string, 
  ruleId: string
): Promise<ScenarioWithRules> => {
  const response = await api.delete(`/api/v1/scenarios/${scenarioId}/rules/${ruleId}`);
  return response.data.data;
};

// 시나리오 검색
export const searchScenarios = async (keyword: string): Promise<ScenarioWithRules[]> => {
  const response = await api.get('/api/v1/scenarios', {
    params: { search: keyword }
  });
  return response.data.data;
};

// ========== 세분화된 수정 API ==========

// 시나리오 기본정보 수정 요청
export interface UpdateBasicInfoRequest {
  scenarioName: string;
  description?: string;
  riskLevelId?: string; // 위험 레벨 ID
  detectionAreaId?: string; // 탐지영역 ID
  primaryEntityType?: string; // 주요 엔티티 타입
  isActive?: boolean;
  // 엔진 설정 필드
  dedupMinutes?: number; // 중복 제거 창 (분)
}

// 시나리오 엔티티 필터 수정 요청
export interface UpdateEntityFilterRequest {
  entityFilterJson?: string;
}

// 시나리오 집계구성 수정 요청
export interface UpdateRulesRequest {
  rules: ScenarioRuleMapping[];
}

// 시나리오 기본정보 수정
export const updateBasicInfo = async (
  scenarioId: string,
  data: UpdateBasicInfoRequest
): Promise<ScenarioWithRules> => {
  const response = await api.patch(`/api/v1/scenarios/${scenarioId}/basic-info`, data);
  return response.data.data;
};

// 시나리오 엔티티 필터 수정
export const updateEntityFilter = async (
  scenarioId: string,
  data: UpdateEntityFilterRequest
): Promise<ScenarioWithRules> => {
  const response = await api.patch(`/api/v1/scenarios/${scenarioId}/entity-filter`, data);
  return response.data.data;
};

// 시나리오 집계구성 수정
export const updateScenarioRules = async (
  scenarioId: string,
  data: UpdateRulesRequest
): Promise<ScenarioWithRules> => {
  const response = await api.patch(`/api/v1/scenarios/${scenarioId}/rules`, data);
  return response.data.data;
};

// ========== 메타데이터 API ==========

// 위험 레벨 타입
export interface RiskLevelMetadata {
  value: string; // ID (예: MONITOR, REVIEW, BLOCK)
  levelCode: number; // 레벨 코드 (예: 10, 20, 30, 40)
  label: string; // 표시명 (예: 모니터링, 심사, 차단)
  actionType: string; // 액션 타입 (예: ALERT, HOLD, BLOCK)
  description: string; // 설명
}

// 엔티티 타입 메타데이터
export interface EntityTypeMetadata {
  value: string; // ID (예: CUSTOMER, ACCOUNT, AUTHENTICATION)
  label: string; // 표시명 (예: 고객, 계좌, 인증)
  description: string; // 설명
}

// 위험 레벨 목록 조회
export const fetchRiskLevels = async (): Promise<RiskLevelMetadata[]> => {
  const response = await api.get('/api/v1/metadata/risk-levels');
  return response.data.data;
};

// 엔티티 타입 목록 조회
export const fetchEntityTypes = async (): Promise<EntityTypeMetadata[]> => {
  const response = await api.get('/api/v1/metadata/entity-types');
  return response.data.data;
};

// 시나리오 시각화 데이터
export interface RuleWithSensor {
  ruleId: string;
  ruleName: string;
  operator: string | null;
  windowMinutes: number | null;
  orderNo: number;
  scenarioOperator: ScenarioOperator | null;
  predicateSensorId: string | null;
  predicateSensorName: string | null;
}

export interface ScenarioVisualization {
  scenarioId: string;
  scenarioName: string;
  rules: RuleWithSensor[];
}

// 시나리오 시각화 데이터 조회
export const fetchScenarioVisualization = async (scenarioId: string): Promise<ScenarioVisualization> => {
  const response = await api.get(`/api/v1/scenarios/${scenarioId}/visualization`);
  return response.data;
};

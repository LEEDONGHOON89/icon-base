import api from "@/lib/api";

// 응답 리스트 타입
interface ResponseList<T> {
  data: T[];
  total?: number;
}

// 룰 카테고리
export type RuleCategory =
  | "SECURITY"
  | "COMPLIANCE"
  | "MONITORING"
  | "BACKUP"
  | "NETWORK"
  | "CUSTOM";

// 룰 (백엔드 RuleDto.Response 객체에 맞춤)
export interface Rule {
  ruleId: string; // 룰 ID
  name: string; // 룰 이름
  description?: string;
  operator?: string; // 연산자 (COUNT_WITHIN, SUM_WITHIN 등)
  predicateSensorId?: string; // 대상 센서 ID
  prevSensorId?: string; // 시퀀스 이전 센서
  nextSensorId?: string; // 시퀀스 다음 센서
  anchorSensorId?: string; // 앵커 센서
  windowMinutes?: number; // 윈도우 시간(분)
  thresholdCount?: number; // 임계값 - 횟수
  thresholdAmount?: number; // 임계값 - 금액
  dedupMinutes?: number; // 중복 제거 시간(분)
  isActive?: boolean; // 활성화 여부
  groupByFields?: string[]; // 그룹화 필드
  aggregationField?: string; // 집계 필드
  entityType?: string; // 엔티티 타입
  whereJson?: string; // 조건 JSON
  evaluationMode?: string; // 평가 모드 (WINDOW, IMMEDIATE)
}

export interface RuleCondition {
  fieldName: string; // 필드명
  operator: string; // 연산자 enum name (예: "GREATER_THAN_OR_EQUALS")
  operatorSymbol: string; // 연산자 기호 (예: ">=")
  operatorLabel: string; // 연산자 한글명
  value?: any; // 값
}

export interface RuleCreateRequest {
  ruleId: string; // 사용자 정의 룰 ID (예: AGG_MULTIPLE_FAIL)
  name: string; // 룰 이름
  description?: string;
  operator: string; // 연산자 (필수)
  predicateSensorId?: string; // 대상 센서 ID
  prevSensorId?: string; // 시퀀스 이전 센서
  nextSensorId?: string; // 시퀀스 다음 센서
  anchorSensorId?: string; // 앵커 센서
  windowMinutes?: number; // 윈도우 시간(분)
  thresholdCount?: number; // 임계값 - 횟수
  thresholdAmount?: number; // 임계값 - 금액
  dedupMinutes?: number; // 중복 제거 시간(분)
  groupByFields?: string[]; // 그룹화 필드
  aggregationField?: string; // 집계 필드
  entityType: string; // 엔티티 타입 (필수)
  whereJson?: string; // 조건 JSON
  evaluationMode?: string; // 평가 모드
}

export interface RuleUpdateRequest {
  name?: string; // 룰 이름
  description?: string;
  operator?: string; // 연산자
  predicateSensorId?: string; // 대상 센서 ID
  prevSensorId?: string; // 시퀀스 이전 센서
  nextSensorId?: string; // 시퀀스 다음 센서
  anchorSensorId?: string; // 앵커 센서
  windowMinutes?: number; // 윈도우 시간(분)
  thresholdCount?: number; // 임계값 - 횟수
  thresholdAmount?: number; // 임계값 - 금액
  dedupMinutes?: number; // 중복 제거 시간(분)
  isActive?: boolean;
  groupByFields?: string[]; // 그룹화 필드
  aggregationField?: string; // 집계 필드
  entityType?: string; // 엔티티 타입
  whereJson?: string; // 조건 JSON
  evaluationMode?: string; // 평가 모드
}

// 룰 목록 조회 (API 엔드포인트: /api/v1/rules)
export const fetchRules = async (): Promise<ResponseList<Rule>> => {
  const response = await api.get<ResponseList<Rule>>("/api/v1/rules");
  return response.data;
};

// 룰 생성 (API 엔드포인트: /api/v1/rules)
export const createRule = async (data: RuleCreateRequest): Promise<Rule> => {
  const response = await api.post("/api/v1/rules", data);
  return response.data;
};

// 룰 수정 (API 엔드포인트: /api/v1/rules/:id)
export const updateRule = async (
  ruleId: string,
  data: RuleUpdateRequest
): Promise<Rule> => {
  const response = await api.put(`/api/v1/rules/${ruleId}`, data);
  return response.data;
};

// 룰 삭제 (API 엔드포인트: /api/v1/rules/:id)
export const deleteRule = async (ruleId: string): Promise<void> => {
  await api.delete(`/api/v1/rules/${ruleId}`);
};

// 단일 룰 조회 (API 엔드포인트: /api/v1/rules/:id)
export const fetchRuleById = async (ruleId: string): Promise<Rule> => {
  const response = await api.get(`/api/v1/rules/${ruleId}`);
  return response.data;
};

// 카테고리별 룰 조회
export const fetchRulesByCategory = async (
  category: RuleCategory
): Promise<ResponseList<Rule>> => {
  throw new Error("Category filter is deprecated in v4");
};

// 활성화된 룰 조회 (API 엔드포인트: /api/v1/rules/active)
export const fetchActiveRules = async (): Promise<ResponseList<Rule>> => {
  const response = await api.get<ResponseList<Rule>>("/api/v1/rules/active");
  return response.data;
};

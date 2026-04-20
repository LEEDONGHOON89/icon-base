// [2026-04-20] 파서 관리 API - 재설계: 파서를 데이터소스 레벨에서 관리
import api from "@/lib/api";

export type ParserType = "DELIMITER" | "FIXED_WIDTH" | "REGEX";

// ─── 파서 규칙 아이템 ────────────────────────────────────────────────────────
// [2026-04-20] configJson은 DELIMITER에서 null (파서레벨 delimiter 사용), FIXED_WIDTH/REGEX에서만 사용
export interface ParserRuleItem {
  ruleOrder: number;
  configJson?: string | null;
  targetStandardFieldId?: string | null;
  targetFieldName: string;
}

export interface ParserRuleResponse {
  parserRuleId: string;
  ruleOrder: number;
  configJson?: string | null;
  targetStandardFieldId?: string | null;
  targetFieldName?: string | null;
}

// ─── 파서 요약 (목록용) ──────────────────────────────────────────────────────
export interface ParserSummary {
  parserId: string;
  parserName: string;
  parserType: ParserType;
  // [2026-04-20] sourceField: 파싱 대상 원본 필드명
  sourceField?: string | null;
  description?: string | null;
  isActive: boolean;
  ruleCount: number;
}

// ─── 파서 상세 (편집/조회용) ─────────────────────────────────────────────────
export interface ParserDetail {
  parserId: string;
  parserName: string;
  parserType: ParserType;
  // [2026-04-20] sourceField: 파싱 대상 원본 필드명
  sourceField?: string | null;
  // [2026-04-20] configJson: 파서 레벨 설정 (DELIMITER: {"delimiter":"|"})
  configJson?: string | null;
  description?: string | null;
  isActive: boolean;
  rules: ParserRuleResponse[];
  createdAt: string;
  updatedAt: string;
}

// ─── 파서 생성 요청 ──────────────────────────────────────────────────────────
export interface CreateParserRequest {
  parserName: string;
  parserType: ParserType;
  // [2026-04-20] sourceField 필수: 파싱할 원본 필드
  sourceField: string;
  // [2026-04-20] configJson: DELIMITER는 {"delimiter":"|"}, FIXED_WIDTH/REGEX는 null
  configJson?: string | null;
  description?: string;
  rules: ParserRuleItem[];
}

// ─── 파서 수정 요청 ──────────────────────────────────────────────────────────
export interface UpdateParserRequest {
  parserName?: string;
  // [2026-04-20] sourceField 수정 가능
  sourceField?: string;
  // [2026-04-20] configJson 수정 가능
  configJson?: string | null;
  description?: string;
  isActive?: boolean;
  rules?: ParserRuleItem[];
}

// ─── 데이터소스-파서 연결 ─────────────────────────────────────────────────────
// [2026-04-20] 파서를 데이터소스에 연결하는 요청/응답
export interface LinkParserRequest {
  parserId: string;
  parserOrder: number;
}

export interface LinkedParserResponse {
  dataSourceParserId: string;
  dataSourceId: string;
  parser: ParserSummary;
  parserOrder: number;
  isActive: boolean;
}

// ─── 파서 전체 목록 조회 ─────────────────────────────────────────────────────
export async function fetchParsers(): Promise<ParserSummary[]> {
  const res = await api.get("/api/v1/parsers");
  return Array.isArray(res.data) ? res.data : [];
}

/** 활성 파서 목록 조회 */
export async function fetchActiveParsers(): Promise<ParserSummary[]> {
  const res = await api.get("/api/v1/parsers/active");
  return Array.isArray(res.data) ? res.data : [];
}

/** 파서 상세 조회 */
export async function fetchParser(parserId: string): Promise<ParserDetail> {
  const res = await api.get(`/api/v1/parsers/${parserId}`);
  return res.data;
}

/** 파서 생성 */
export async function createParser(data: CreateParserRequest): Promise<ParserDetail> {
  const res = await api.post("/api/v1/parsers", data);
  return res.data;
}

/** 파서 수정 */
export async function updateParser(parserId: string, data: UpdateParserRequest): Promise<ParserDetail> {
  const res = await api.put(`/api/v1/parsers/${parserId}`, data);
  return res.data;
}

/** 파서 삭제 */
export async function deleteParser(parserId: string): Promise<void> {
  await api.delete(`/api/v1/parsers/${parserId}`);
}

// ─── 데이터소스-파서 연결 API ─────────────────────────────────────────────────
// [2026-04-20] 데이터소스에 파서 연결/해제

/** 데이터소스에 연결된 파서 목록 조회 */
export async function fetchLinkedParsers(dataSourceId: string): Promise<LinkedParserResponse[]> {
  const res = await api.get(`/api/v1/data-sources/${dataSourceId}/parsers`);
  return Array.isArray(res.data) ? res.data : [];
}

/** 데이터소스에 파서 연결 */
export async function linkParser(dataSourceId: string, data: LinkParserRequest): Promise<LinkedParserResponse> {
  const res = await api.post(`/api/v1/data-sources/${dataSourceId}/parsers`, data);
  return res.data;
}

/** 데이터소스에서 파서 연결 해제 */
export async function unlinkParser(dataSourceId: string, parserId: string): Promise<void> {
  await api.delete(`/api/v1/data-sources/${dataSourceId}/parsers/${parserId}`);
}

// ─── 상수 ────────────────────────────────────────────────────────────────────

/**
 * 파서 타입별 규칙 configJson 기본값
 * DELIMITER: null (파서 레벨 configJson 사용)
 * FIXED_WIDTH: {"byteLength":4}
 * REGEX: {"pattern":"^(\\w+)","group":1}
 */
export const DEFAULT_RULE_CONFIG_JSON: Record<ParserType, string> = {
  DELIMITER: "",
  FIXED_WIDTH: JSON.stringify({ byteLength: 4 }),
  REGEX: JSON.stringify({ pattern: "^(\\w+)", group: 1 }),
};

/** [하위호환용] DEFAULT_CONFIG_JSON */
export const DEFAULT_CONFIG_JSON = DEFAULT_RULE_CONFIG_JSON;

/** 파서 타입 표시명 */
export const PARSER_TYPE_LABELS: Record<ParserType, string> = {
  DELIMITER: "구분자 파서",
  FIXED_WIDTH: "고정폭 파서",
  REGEX: "정규식 파서",
};

/** 파서 타입 배지 색상 */
export const PARSER_TYPE_COLORS: Record<ParserType, string> = {
  DELIMITER: "bg-blue-100 text-blue-700",
  FIXED_WIDTH: "bg-green-100 text-green-700",
  REGEX: "bg-purple-100 text-purple-700",
};

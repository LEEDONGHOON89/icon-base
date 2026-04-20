// [2026-04-20] 파서 관리 API
import api from "@/lib/api";

export type ParserType = "DELIMITER" | "FIXED_WIDTH" | "REGEX";

export interface ParserRuleItem {
  ruleOrder: number;
  configJson: string;
  targetStandardFieldId?: string | null;
  targetFieldName?: string | null;
}

export interface ParserRuleResponse {
  parserRuleId: string;
  ruleOrder: number;
  configJson: string;
  targetStandardFieldId?: string | null;
  targetFieldName?: string | null;
}

export interface ParserSummary {
  parserId: string;
  parserName: string;
  parserType: ParserType;
  description?: string | null;
  isActive: boolean;
  ruleCount: number;
}

export interface ParserDetail {
  parserId: string;
  parserName: string;
  parserType: ParserType;
  description?: string | null;
  isActive: boolean;
  rules: ParserRuleResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateParserRequest {
  parserName: string;
  parserType: ParserType;
  description?: string;
  rules: ParserRuleItem[];
}

export interface UpdateParserRequest {
  parserName?: string;
  description?: string;
  isActive?: boolean;
  rules?: ParserRuleItem[];
}

/** 파서 전체 목록 조회 */
export async function fetchParsers(): Promise<ParserSummary[]> {
  const res = await api.get("/api/v1/parsers");
  return Array.isArray(res.data) ? res.data : [];
}

/** 활성 파서 목록 조회 (데이터소스 원본 필드 선택용) */
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

/** 파서 타입 표시명 */
export const PARSER_TYPE_LABELS: Record<ParserType, string> = {
  DELIMITER: "구분자 파서",
  FIXED_WIDTH: "고정폭 파서",
  REGEX: "정규식 파서",
};

/** config_json 기본값 */
export const DEFAULT_CONFIG_JSON: Record<ParserType, string> = {
  DELIMITER: JSON.stringify({ delimiter: "|", index: 0 }),
  FIXED_WIDTH: JSON.stringify({ startByte: 0, byteLength: 10 }),
  REGEX: JSON.stringify({ pattern: "^(\\w+)", group: 1 }),
};

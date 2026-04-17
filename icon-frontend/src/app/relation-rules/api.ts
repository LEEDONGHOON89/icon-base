import api from "@/lib/api";

/**
 * 도메인 관계 규칙 응답 타입
 */
export interface RelationRule {
  /**
   * 규칙 ID
   */
  ruleId: number;

  /**
   * 데이터소스 ID
   */
  dataSourceId: string;

  /**
   * From 도메인 타입 (CUSTOMER, ACCOUNT 등)
   */
  fromEntityType: string;

  /**
   * From ID 필드명 (이벤트 데이터의 필드명)
   */
  fromIdField: string;

  /**
   * 관계 타입 (OWNS, USES, AUTHENTICATES, ACCESSES)
   */
  relationType: string;

  /**
   * To 도메인 타입
   */
  toEntityType: string;

  /**
   * To ID 필드명 (이벤트 데이터의 필드명)
   */
  toIdField: string;

  /**
   * 규칙 설명
   */
  description?: string;

  /**
   * 활성화 여부
   */
  isActive: boolean;

  /**
   * 생성 일시
   */
  createdAt: string;

  /**
   * 수정 일시
   */
  updatedAt: string;
}

/**
 * 도메인 관계 규칙 생성 요청 타입
 */
export interface CreateRelationRuleRequest {
  dataSourceId: string;
  fromEntityType: string;
  fromIdField: string;
  relationType: string;
  toEntityType: string;
  toIdField: string;
  description?: string;
}

/**
 * 도메인 관계 규칙 수정 요청 타입
 */
export interface UpdateRelationRuleRequest {
  dataSourceId: string;
  fromEntityType: string;
  fromIdField: string;
  relationType: string;
  toEntityType: string;
  toIdField: string;
  description?: string;
  isActive?: boolean;
}

/**
 * 전체 도메인 관계 규칙 조회
 *
 * @param dataSourceId (Optional) 데이터소스 ID로 필터링
 * @returns 규칙 목록
 */
export async function fetchRelationRules(
  dataSourceId?: string
): Promise<RelationRule[]> {
  const params = dataSourceId ? { dataSourceId } : {};
  const res = await api.get<RelationRule[]>("/api/v1/relation-rules", {
    params,
  });
  return res.data;
}

/**
 * 단일 도메인 관계 규칙 조회
 *
 * @param ruleId 규칙 ID
 * @returns 규칙 정보
 */
export async function fetchRelationRule(
  ruleId: number
): Promise<RelationRule> {
  const res = await api.get<RelationRule>(`/api/v1/relation-rules/${ruleId}`);
  return res.data;
}

/**
 * 도메인 관계 규칙 생성
 *
 * @param request 생성 요청 데이터
 * @returns 생성된 규칙 정보
 */
export async function createRelationRule(
  request: CreateRelationRuleRequest
): Promise<RelationRule> {
  const res = await api.post<RelationRule>("/api/v1/relation-rules", request);
  return res.data;
}

/**
 * 도메인 관계 규칙 수정
 *
 * @param ruleId 규칙 ID
 * @param request 수정 요청 데이터
 * @returns 수정된 규칙 정보
 */
export async function updateRelationRule(
  ruleId: number,
  request: UpdateRelationRuleRequest
): Promise<RelationRule> {
  const res = await api.put<RelationRule>(
    `/api/v1/relation-rules/${ruleId}`,
    request
  );
  return res.data;
}

/**
 * 도메인 관계 규칙 삭제
 *
 * @param ruleId 규칙 ID
 */
export async function deleteRelationRule(ruleId: number): Promise<void> {
  await api.delete(`/api/v1/relation-rules/${ruleId}`);
}

/**
 * 도메인 관계 규칙 활성화
 *
 * @param ruleId 규칙 ID
 * @returns 수정된 규칙 정보
 */
export async function activateRelationRule(
  ruleId: number
): Promise<RelationRule> {
  const res = await api.post<RelationRule>(
    `/api/v1/relation-rules/${ruleId}/activate`
  );
  return res.data;
}

/**
 * 도메인 관계 규칙 비활성화
 *
 * @param ruleId 규칙 ID
 * @returns 수정된 규칙 정보
 */
export async function deactivateRelationRule(
  ruleId: number
): Promise<RelationRule> {
  const res = await api.post<RelationRule>(
    `/api/v1/relation-rules/${ruleId}/deactivate`
  );
  return res.data;
}

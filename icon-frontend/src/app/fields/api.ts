import api from "@/lib/api";

// 표준 필드 타입
export interface StandardField {
  fieldId: string;
  fieldName: string | null;
  displayName: string;
  dataType: string;
  category: string | null;
  description?: string | null;
  required: boolean;
  active: boolean;
  searchable: boolean;
  sampleValue?: string | null;
  validationRule?: string | null;
}

// [2026-04-20] 표준 필드 생성/수정 요청 타입 추가
export interface CreateStandardFieldRequest {
  fieldName: string;
  displayName: string;
  dataType: string;
  category?: string;
  description?: string;
}

export interface UpdateStandardFieldRequest {
  displayName: string;
  dataType: string;
  category?: string;
  description?: string;
  isActive: boolean;
}

// 엔티티 필드 타입
export interface EntityField {
  entityFieldId: string;
  displayName: string;
  dataType: string;
  description?: string;
  isActive: boolean;
}

/**
 * 표준 필드 목록 조회
 */
export async function fetchStandardFields(): Promise<StandardField[]> {
  const response = await api.get("/api/v1/standard-fields");
  // ResponseList 구조: { total: number, data: T[] }
  return response.data.data || [];
}

// [2026-04-20] 표준 필드 생성
export async function createStandardField(data: CreateStandardFieldRequest): Promise<StandardField> {
  const response = await api.post("/api/v1/standard-fields", data);
  return response.data;
}

// [2026-04-20] 표준 필드 수정
export async function updateStandardField(fieldId: string, data: UpdateStandardFieldRequest): Promise<StandardField> {
  const response = await api.put(`/api/v1/standard-fields/${fieldId}`, data);
  return response.data;
}

// [2026-04-20] 표준 필드 삭제
export async function deleteStandardField(fieldId: string): Promise<void> {
  await api.delete(`/api/v1/standard-fields/${fieldId}`);
}

/**
 * 엔티티 필드 목록 조회 (전체)
 */
export async function fetchEntityFields(): Promise<EntityField[]> {
  const response = await api.get("/api/v1/entity-fields/all");
  // 배열로 직접 반환 (ResponseList 아님)
  return Array.isArray(response.data) ? response.data : [];
}

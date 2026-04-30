// [2026-04-24] CRUD API 추가
import api from "@/lib/api";

export interface EntityField {
  entityFieldId: string;
  displayName: string;
  dataType: string;
  description?: string;
  isActive: boolean;
}

export interface CreateEntityFieldRequest {
  entityFieldId: string;
  displayName: string;
  dataType: string;
  description?: string;
}

export interface UpdateEntityFieldRequest {
  displayName: string;
  dataType: string;
  description?: string;
  isActive?: boolean;
}

export const DATA_TYPES = [
  { value: 'STRING',    label: 'STRING (문자열)' },
  { value: 'NUMBER',    label: 'NUMBER (숫자)' },
  { value: 'BOOLEAN',   label: 'BOOLEAN (참/거짓)' },
  { value: 'DATE',      label: 'DATE (날짜)' },
  { value: 'DATETIME',  label: 'DATETIME (날짜+시간)' },
  { value: 'TIMESTAMP', label: 'TIMESTAMP (타임스탬프)' },
  { value: 'ARRAY',     label: 'ARRAY (배열)' },
  { value: 'OBJECT',    label: 'OBJECT (객체)' },
];

/** 활성화된 엔티티 필드 목록 조회 */
export const fetchEntityFields = async (): Promise<EntityField[]> => {
  const response = await api.get('/api/v1/entity-fields');
  return response.data;
};

/** 모든 엔티티 필드 목록 조회 (관리용) */
export const fetchAllEntityFields = async (): Promise<EntityField[]> => {
  const response = await api.get('/api/v1/entity-fields/all');
  return response.data;
};

/** 엔티티 필드 등록 */
export const createEntityField = async (data: CreateEntityFieldRequest): Promise<EntityField> => {
  const response = await api.post('/api/v1/entity-fields', data);
  return response.data;
};

/** 엔티티 필드 수정 */
export const updateEntityField = async (entityFieldId: string, data: UpdateEntityFieldRequest): Promise<EntityField> => {
  const response = await api.put(`/api/v1/entity-fields/${entityFieldId}`, data);
  return response.data;
};

/** 엔티티 필드 삭제 */
export const deleteEntityField = async (entityFieldId: string): Promise<void> => {
  await api.delete(`/api/v1/entity-fields/${entityFieldId}`);
};

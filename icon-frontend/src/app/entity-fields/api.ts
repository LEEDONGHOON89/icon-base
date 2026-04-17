import api from "@/lib/api";

export interface EntityField {
  entityFieldId: string;
  displayName: string;
  dataType: string;
  description?: string;
  isActive: boolean;
}

/**
 * 활성화된 엔티티 필드 목록 조회
 */
export const fetchEntityFields = async (): Promise<EntityField[]> => {
  const response = await api.get('/api/v1/entity-fields');
  return response.data;
};

/**
 * 모든 엔티티 필드 목록 조회
 */
export const fetchAllEntityFields = async (): Promise<EntityField[]> => {
  const response = await api.get('/api/v1/entity-fields/all');
  return response.data;
};

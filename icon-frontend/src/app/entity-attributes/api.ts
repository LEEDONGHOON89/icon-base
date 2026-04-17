import api from "@/lib/api";

/**
 * entity_attributes 응답 타입
 */
export interface EntityAttribute {
  /**
   * 엔티티 타입 (CUSTOMER, ACCOUNT, DEVICE 등)
   */
  entityType: string;

  /**
   * 엔티티 ID (CUS001, ACC001 등)
   */
  entityId: string;

  /**
   * 정적 속성 정보
   * 예: { age: 68, grade: "VIP", region: "부산", owned_accounts: ["ACC001"] }
   */
  attributes: Record<string, unknown>;

  /**
   * 마지막 업데이트 시각 (ISO datetime)
   */
  updatedAt: string;
}

/**
 * 엔티티 타입과 ID로 entity_attributes 조회
 *
 * @param entityType 엔티티 타입 (CUSTOMER, ACCOUNT, DEVICE 등)
 * @param entityId 엔티티 ID (CUS001, ACC001 등)
 * @returns EntityAttribute 또는 null (404일 경우)
 */
export async function fetchEntityAttribute(
  entityType: string,
  entityId: string
): Promise<EntityAttribute | null> {
  try {
    const res = await api.get<EntityAttribute>(
      `/api/v1/entity-attributes/${entityType}/${entityId}`
    );
    return res.data;
  } catch (error) {
    // 404 에러일 경우 null 반환
    if ((error as any)?.response?.status === 404) {
      return null;
    }
    throw error;
  }
}

/**
 * 엔티티 타입과 ID로 attributes만 조회
 *
 * @param entityType 엔티티 타입
 * @param entityId 엔티티 ID
 * @returns attributes Record 또는 null (404일 경우)
 */
export async function fetchEntityAttributes(
  entityType: string,
  entityId: string
): Promise<Record<string, unknown> | null> {
  try {
    const res = await api.get<Record<string, unknown>>(
      `/api/v1/entity-attributes/${entityType}/${entityId}/attributes`
    );
    return res.data;
  } catch (error) {
    if ((error as any)?.response?.status === 404) {
      return null;
    }
    throw error;
  }
}

/**
 * 특정 타입의 모든 entity_attributes 조회
 *
 * @param entityType 엔티티 타입
 * @returns EntityAttribute 배열
 */
export async function fetchEntitiesByType(
  entityType: string
): Promise<EntityAttribute[]> {
  const res = await api.get<EntityAttribute[]>(
    `/api/v1/entity-attributes/type/${entityType}`
  );
  return res.data;
}

/**
 * 특정 타입과 ID 목록으로 배치 조회
 *
 * @param entityType 엔티티 타입
 * @param entityIds 엔티티 ID 목록
 * @returns EntityAttribute 배열
 */
export async function fetchBatchEntities(
  entityType: string,
  entityIds: string[]
): Promise<EntityAttribute[]> {
  const res = await api.post<EntityAttribute[]>(
    `/api/v1/entity-attributes/batch/${entityType}`,
    entityIds
  );
  return res.data;
}

/**
 * 고객 정보 조회 (CUSTOMER 타입 단축 함수)
 *
 * @param customerId 고객 ID (예: CUS001)
 * @returns EntityAttribute 또는 null
 */
export async function fetchCustomerInfo(
  customerId: string
): Promise<EntityAttribute | null> {
  return fetchEntityAttribute("CUSTOMER", customerId);
}

/**
 * 계좌 정보 조회 (ACCOUNT 타입 단축 함수)
 *
 * @param accountId 계좌 ID (예: ACC001)
 * @returns EntityAttribute 또는 null
 */
export async function fetchAccountInfo(
  accountId: string
): Promise<EntityAttribute | null> {
  return fetchEntityAttribute("ACCOUNT", accountId);
}

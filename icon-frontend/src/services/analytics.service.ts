import api from "@/lib/api";

/**
 * Entity Graph Types
 */
export interface GraphNode {
  id: string;           // "ACCOUNT:ACC001"
  label: string;        // "ACC001"
  type: string;         // "ACCOUNT"
  data?: any;          // 추가 데이터 (attributes 등)
}

export interface GraphEdge {
  id: string;           // "ACCOUNT:ACC001->TRANSFERS_TO->ACCOUNT:ACC002"
  source: string;       // "ACCOUNT:ACC001"
  target: string;       // "ACCOUNT:ACC002"
  label: string;        // "TRANSFERS_TO"
  type: string;         // "TRANSFERS_TO"
  data?: any;          // 추가 데이터 (properties 등)
}

export interface EntityGraphDto {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

/**
 * 엔티티 관계 그래프 조회
 */
export async function fetchEntityGraph(
  entityType: string,
  entityId: string,
  depth: number = 1
): Promise<EntityGraphDto> {
  const response = await api.get(
    `/api/v1/analytics/entities/${entityType}/${entityId}/graph`,
    {
      params: { depth },
    }
  );
  return response.data;
}

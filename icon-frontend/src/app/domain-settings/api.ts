import api from "@/lib/api";

/**
 * 도메인 인터페이스
 */
export interface DetectionArea {
  detectionAreaId: string;
  areaName: string;
  description?: string;
  icon?: string;
  color?: string;
  isActive: boolean;
  displayOrder?: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * 도메인 생성 요청
 */
export interface CreateDetectionAreaRequest {
  detectionAreaId: string; // 영문 대문자, 숫자, 언더스코어만 허용 (^[A-Z0-9_]+$)
  areaName: string;
  description?: string;
  icon?: string;
  color?: string;
  displayOrder?: number;
}

/**
 * 도메인 수정 요청
 */
export interface UpdateDetectionAreaRequest {
  areaName: string;
  description?: string;
  icon?: string;
  color?: string;
  isActive?: boolean;
  displayOrder?: number;
}

/**
 * 전체 도메인 조회
 */
export async function fetchDetectionAreas(): Promise<DetectionArea[]> {
  const res = await api.get<DetectionArea[]>("/api/v1/detection-areas");
  return res.data;
}

/**
 * 활성화된 도메인만 조회
 */
export async function fetchActiveDetectionAreas(): Promise<DetectionArea[]> {
  const res = await api.get<DetectionArea[]>("/api/v1/detection-areas", {
    params: { activeOnly: true },
  });
  return res.data;
}

/**
 * 단일 도메인 조회
 */
export async function fetchDetectionArea(
  detectionAreaId: string
): Promise<DetectionArea> {
  const res = await api.get<DetectionArea>(
    `/api/v1/detection-areas/${detectionAreaId}`
  );
  return res.data;
}

/**
 * 도메인 생성
 */
export async function createDetectionArea(
  data: CreateDetectionAreaRequest
): Promise<DetectionArea> {
  const res = await api.post<DetectionArea>("/api/v1/detection-areas", data);
  return res.data;
}

/**
 * 도메인 수정
 */
export async function updateDetectionArea(
  detectionAreaId: string,
  data: UpdateDetectionAreaRequest
): Promise<DetectionArea> {
  const res = await api.put<DetectionArea>(
    `/api/v1/detection-areas/${detectionAreaId}`,
    data
  );
  return res.data;
}

/**
 * 도메인 삭제
 */
export async function deleteDetectionArea(detectionAreaId: string): Promise<void> {
  await api.delete(`/api/v1/detection-areas/${detectionAreaId}`);
}

/**
 * 도메인 활성화
 */
export async function activateDetectionArea(
  detectionAreaId: string
): Promise<DetectionArea> {
  const res = await api.post<DetectionArea>(
    `/api/v1/detection-areas/${detectionAreaId}/activate`
  );
  return res.data;
}

/**
 * 도메인 비활성화
 */
export async function deactivateDetectionArea(
  detectionAreaId: string
): Promise<DetectionArea> {
  const res = await api.post<DetectionArea>(
    `/api/v1/detection-areas/${detectionAreaId}/deactivate`
  );
  return res.data;
}

import api from "@/lib/api";

// 감사 대상 타입
export type ConfigAuditTargetType = 'SENSOR' | 'RULE' | 'SCENARIO';

// 감사 액션 타입
export type ConfigAuditAction = 'CREATE' | 'UPDATE' | 'DELETE';

// 감사 로그 요약 (목록용)
export interface AuditSummary {
  auditId: number;
  targetType: ConfigAuditTargetType;
  targetId: string;
  targetName: string;
  action: ConfigAuditAction;
  changedBy: string;
  changedAt: string;
}

// 감사 로그 상세
export interface AuditDetail extends AuditSummary {
  changedFields?: string; // JSON string
  beforeSnapshot?: string; // JSON string
  afterSnapshot?: string; // JSON string
  changeReason?: string;
  ipAddress?: string;
}

// 페이징 응답
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // current page (0-based)
}

// 최근 변경이력 조회 (최근 50건)
export const fetchRecentAuditHistory = async (): Promise<AuditSummary[]> => {
  const response = await api.get('/api/v1/audit/detection-config/recent');
  return response.data;
};

// 특정 대상의 변경이력 조회
export const fetchAuditHistoryByTarget = async (
  targetType: ConfigAuditTargetType,
  targetId: string
): Promise<AuditDetail[]> => {
  const response = await api.get(`/api/v1/audit/detection-config/${targetType}/${targetId}`);
  return response.data;
};

// 특정 대상의 변경이력 조회 (페이징)
export const fetchAuditHistoryByTargetPaged = async (
  targetType: ConfigAuditTargetType,
  targetId: string,
  page: number = 0,
  size: number = 20
): Promise<PageResponse<AuditSummary>> => {
  const response = await api.get(`/api/v1/audit/detection-config/${targetType}/${targetId}/page`, {
    params: { page, size }
  });
  return response.data;
};

// 사용자별 변경이력 조회
export const fetchAuditHistoryByUser = async (
  userId: string,
  page: number = 0,
  size: number = 20
): Promise<PageResponse<AuditSummary>> => {
  const response = await api.get(`/api/v1/audit/detection-config/user/${userId}`, {
    params: { page, size }
  });
  return response.data;
};

// 기간별 변경이력 조회
export const fetchAuditHistoryByDateRange = async (
  startDate: string,
  endDate: string,
  page: number = 0,
  size: number = 20
): Promise<PageResponse<AuditSummary>> => {
  const response = await api.get('/api/v1/audit/detection-config/date-range', {
    params: { startDate, endDate, page, size }
  });
  return response.data;
};

// 타입별 + 기간별 변경이력 조회
export const fetchAuditHistoryByTypeAndDateRange = async (
  targetType: ConfigAuditTargetType,
  startDate: string,
  endDate: string,
  page: number = 0,
  size: number = 20
): Promise<PageResponse<AuditSummary>> => {
  const response = await api.get(`/api/v1/audit/detection-config/type/${targetType}/date-range`, {
    params: { startDate, endDate, page, size }
  });
  return response.data;
};

// 통합 검색 요청 타입
export interface AuditSearchParams {
  targetType?: ConfigAuditTargetType;
  action?: ConfigAuditAction;
  startDate?: string; // ISO-8601 format
  endDate?: string;   // ISO-8601 format
  search?: string;
  page?: number;
  size?: number;
}

// 통합 검색
export const searchAuditHistory = async (
  params: AuditSearchParams
): Promise<PageResponse<AuditSummary>> => {
  const { page = 0, size = 20, ...filters } = params;
  const response = await api.get('/api/v1/audit/detection-config/search', {
    params: { ...filters, page, size }
  });
  return response.data;
};

// 액션 타입 한글 변환
export const getActionLabel = (action: ConfigAuditAction): string => {
  switch (action) {
    case 'CREATE': return '생성';
    case 'UPDATE': return '수정';
    case 'DELETE': return '삭제';
    default: return action;
  }
};

// 대상 타입 한글 변환
export const getTargetTypeLabel = (targetType: ConfigAuditTargetType): string => {
  switch (targetType) {
    case 'SENSOR': return '센서';
    case 'RULE': return '룰';
    case 'SCENARIO': return '시나리오';
    default: return targetType;
  }
};

// 액션별 색상 클래스
export const getActionColorClass = (action: ConfigAuditAction): string => {
  switch (action) {
    case 'CREATE': return 'bg-green-100 text-green-800';
    case 'UPDATE': return 'bg-blue-100 text-blue-800';
    case 'DELETE': return 'bg-red-100 text-red-800';
    default: return 'bg-gray-100 text-gray-800';
  }
};

// 대상 타입별 색상 클래스
export const getTargetTypeColorClass = (targetType: ConfigAuditTargetType): string => {
  switch (targetType) {
    case 'SENSOR': return 'bg-amber-100 text-amber-800';
    case 'RULE': return 'bg-indigo-100 text-indigo-800';
    case 'SCENARIO': return 'bg-purple-100 text-purple-800';
    default: return 'bg-gray-100 text-gray-800';
  }
};

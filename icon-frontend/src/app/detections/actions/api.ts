import api from "@/lib/api";

export interface DetectAction {
  detectActionId: number;
  detectScenarioId: number;
  scenarioId: string;
  scenarioName: string;
  groupKey: string;
  actionType: string;
  actionStatus: string;
  riskLevel: string;
  actionMemo?: string;
  actionReason?: string;
  requestedBy: string;
  requestedAt: string;
  approvedBy?: string;
  approvedAt?: string;
  completedBy?: string;
  completedAt?: string;
  regDt: string;
}

export interface DetectActionPage {
  content: DetectAction[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface DetectActionSearchParams {
  startDate?: string;
  endDate?: string;
  riskLevel?: string;
  actionStatus?: string;
  page?: number;
  size?: number;
}

export interface UpdateDetectActionRequest {
  actionMemo?: string;
  actionReason?: string;
  actionStatus?: string;
}

export const detectActionApi = {
  async getDetectActions(params: DetectActionSearchParams): Promise<DetectActionPage> {
    const response = await api.get("/api/v1/detect-actions", { params });
    return response.data;
  },

  async updateDetectAction(id: number, data: UpdateDetectActionRequest): Promise<DetectAction> {
    const response = await api.put(`/api/v1/detect-actions/${id}`, data);
    return response.data;
  },
};

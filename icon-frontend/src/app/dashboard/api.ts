import api from "@/lib/api";

export interface ActionStatusStats {
  pending: number;       // 미처리
  approved: number;      // 승인
  rejected: number;      // 거절
  completed: number;     // 완료
}

export interface RiskLevelStats {
  block: number;                       // 차단 탐지 수
  blockActions: ActionStatusStats;     // 차단 조치 상태별 수
  review: number;                      // 심사 탐지 수
  reviewActions: ActionStatusStats;    // 심사 조치 상태별 수
  intensive: number;                   // 집중모니터링
  monitor: number;                     // 모니터링
}

export interface ScenarioSummary {
  scenarioId: string;
  scenarioName: string;
  riskLevelId: string;
  count: number;
}

export interface DetectionAreaStats {
  detectionAreaId: string;
  detectionAreaName: string;
  colorCode: string;
  detectionCount: number;
  topScenarios: ScenarioSummary[];
}

export interface DashboardStats {
  totalTransactions: number;
  detectedTransactions: number;
  detectionRate: number;
  riskLevelStats: RiskLevelStats;
  detectionAreaStats: DetectionAreaStats[];
}

export interface HourlyStats {
  hour: number;           // 0-23
  transactionCount: number;
  detectionCount: number;
}

export const dashboardApi = {
  /**
   * 금일 대시보드 통계 조회
   */
  async getTodayStats(): Promise<DashboardStats> {
    const response = await api.get("/api/v1/dashboard/stats");
    return response.data;
  },

  /**
   * 금일 시간대별 통계 조회
   */
  async getHourlyStats(): Promise<HourlyStats[]> {
    const response = await api.get("/api/v1/dashboard/hourly");
    return response.data;
  },
};

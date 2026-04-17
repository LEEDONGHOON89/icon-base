"use client";

import {
  ChartBarIcon,
  DocumentTextIcon,
  ShieldExclamationIcon,
  LockClosedIcon,
  EyeIcon,
  CheckCircleIcon,
  PlayIcon,
  PauseIcon,
  BellAlertIcon,
  ArrowTrendingUpIcon,
} from "@heroicons/react/24/outline";
import { useEffect, useState, useRef } from "react";
import { dashboardApi, DashboardStats, HourlyStats } from "./api";
import HourlyChart from "./HourlyChart";

export default function DashboardPage() {
  const [dashboardStats, setDashboardStats] = useState<DashboardStats | null>(null);
  const [hourlyStats, setHourlyStats] = useState<HourlyStats[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshInterval, setRefreshInterval] = useState<number>(60);
  const [isAutoRefresh, setIsAutoRefresh] = useState(true);
  const intervalRef = useRef<NodeJS.Timeout | null>(null);

  const fetchStats = async () => {
    try {
      const [statsData, hourlyData] = await Promise.all([
        dashboardApi.getTodayStats(),
        dashboardApi.getHourlyStats(),
      ]);

setDashboardStats(statsData);
      setHourlyStats(hourlyData);
    } catch (error) {
      console.error("Failed to fetch dashboard stats:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStats();
  }, []);

  useEffect(() => {
    if (isAutoRefresh) {
      intervalRef.current = setInterval(() => {
        fetchStats();
      }, refreshInterval * 1000);
    } else {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    }

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [isAutoRefresh, refreshInterval]);

  const toggleAutoRefresh = () => {
    setIsAutoRefresh(!isAutoRefresh);
  };

  const riskStats = dashboardStats?.riskLevelStats;

  // 메인 지표 - 그라데이션 배경 적용
  const summaryStats = [
    {
      name: "금일 전체거래",
      value: loading ? "..." : dashboardStats?.totalTransactions.toLocaleString() || "0",
      icon: DocumentTextIcon,
      gradient: "from-blue-500 to-blue-600",
      iconBg: "bg-blue-400/30",
    },
    {
      name: "탐지거래",
      value: loading ? "..." : dashboardStats?.detectedTransactions.toLocaleString() || "0",
      icon: BellAlertIcon,
      gradient: "from-indigo-500 to-indigo-600",
      iconBg: "bg-indigo-400/30",
    },
    {
      name: "탐지율",
      value: loading ? "..." : `${dashboardStats?.detectionRate.toFixed(2) || "0.00"}%`,
      icon: ArrowTrendingUpIcon,
      gradient: "from-violet-500 to-violet-600",
      iconBg: "bg-violet-400/30",
    },
  ];

  // 위험수준별 통계
  const riskLevelStats = [
    {
      name: "차단",
      riskLevel: "BLOCK",
      detectionCount: loading ? "..." : riskStats?.block?.toLocaleString() || "0",
      actionStats: riskStats?.blockActions,
      icon: LockClosedIcon,
      color: "text-red-600",
      bgColor: "bg-red-50",
      borderColor: "border-red-200",
      iconBg: "bg-red-100",
    },
    {
      name: "심사",
      riskLevel: "REVIEW",
      detectionCount: loading ? "..." : riskStats?.review?.toLocaleString() || "0",
      actionStats: riskStats?.reviewActions,
      icon: CheckCircleIcon,
      color: "text-orange-600",
      bgColor: "bg-orange-50",
      borderColor: "border-orange-200",
      iconBg: "bg-orange-100",
    },
    {
      name: "집중모니터링",
      riskLevel: "INTENSIVE",
      detectionCount: loading ? "..." : riskStats?.intensive?.toLocaleString() || "0",
      actionStats: null, // 집중모니터링은 조치가 없음
      icon: ShieldExclamationIcon,
      color: "text-yellow-600",
      bgColor: "bg-yellow-50",
      borderColor: "border-yellow-200",
      iconBg: "bg-yellow-100",
    },
    {
      name: "모니터링",
      riskLevel: "MONITOR",
      detectionCount: loading ? "..." : riskStats?.monitor?.toLocaleString() || "0",
      actionStats: null, // 모니터링은 조치가 없음
      icon: EyeIcon,
      color: "text-green-600",
      bgColor: "bg-green-50",
      borderColor: "border-green-200",
      iconBg: "bg-green-100",
    },
  ];

  return (
    <div className="space-y-6">
      {/* 헤더 영역 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">실시간 탐지 대시보드</h1>
          {/* <p className="text-sm text-gray-500 mt-1">금일 이상거래 탐지 현황을 실시간으로 모니터링합니다</p> */}
        </div>

        {/* 자동 새로고침 컨트롤 */}
        <div className="flex items-center gap-3 bg-white px-4 py-2 rounded-xl shadow-sm border border-gray-100">
          <span className="text-sm text-gray-500">새로고침</span>
          <select
            value={refreshInterval}
            onChange={(e) => setRefreshInterval(Number(e.target.value))}
            className="px-3 py-1.5 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-gray-50"
          >
            <option value={60}>1분</option>
            <option value={300}>5분</option>
            <option value={600}>10분</option>
            <option value={900}>15분</option>
          </select>

          <button
            onClick={toggleAutoRefresh}
            className={`flex items-center gap-2 px-4 py-1.5 rounded-lg font-medium text-sm transition-all ${isAutoRefresh
                ? "bg-green-500 hover:bg-green-600 text-white shadow-sm"
                : "bg-gray-100 hover:bg-gray-200 text-gray-600"
              }`}
          >
            {isAutoRefresh ? (
              <>
                <div className="w-2 h-2 bg-white rounded-full animate-pulse"></div>
                <PauseIcon className="h-4 w-4" />
                <span>실행 중</span>
              </>
            ) : (
              <>
                <PlayIcon className="h-4 w-4" />
                <span>자동갱신</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* 메인 지표 카드 - 풀 와이드 그라데이션 */}
      <div className="grid grid-cols-3 gap-4">
        {summaryStats.map((stat) => (
          <div
            key={stat.name}
            className={`bg-gradient-to-br ${stat.gradient} rounded-2xl p-6 text-white shadow-lg hover:shadow-xl transition-all duration-300 hover:-translate-y-0.5`}
          >
            <div className="flex items-center justify-between">
              <div>
                <p className="text-white/80 text-sm font-medium mb-1">{stat.name}</p>
                <p className="text-4xl font-bold tracking-tight">{stat.value}</p>
              </div>
              <div className={`${stat.iconBg} p-3 rounded-xl`}>
                <stat.icon className="h-8 w-8 text-white" />
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* 위험수준별 통계 및 조치 상태 */}
      <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-sm">
        {/* 조치 상태 범례 - 우측 상단 */}
        <div className="flex items-center justify-end gap-4 mb-4">
          <span className="text-sm text-gray-500">조치 상태:</span>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-gray-400 rounded-full"></div>
            <span className="text-sm text-gray-700">미처리</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-blue-500 rounded-full"></div>
            <span className="text-sm text-blue-700">승인</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-red-500 rounded-full"></div>
            <span className="text-sm text-red-700">거절</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 bg-green-500 rounded-full"></div>
            <span className="text-sm text-green-700">완료</span>
          </div>
        </div>

        {/* 위험수준별 통계 */}
        <div className="grid grid-cols-4 gap-4">
          {riskLevelStats.map((stat) => (
            <div
              key={stat.name}
              onClick={stat.actionStats ? () => window.open(`/detections/actions?riskLevel=${stat.riskLevel}`, '_blank') : undefined}
              className={`${stat.bgColor} ${stat.borderColor} border rounded-xl p-5 transition-all duration-200 ${
                stat.actionStats
                  ? 'hover:shadow-md cursor-pointer'
                  : 'cursor-default opacity-90'
              }`}
            >
              <div className="flex items-center gap-4 mb-3">
                <div className={`${stat.iconBg} p-2.5 rounded-lg`}>
                  <stat.icon className={`h-5 w-5 ${stat.color}`} />
                </div>
                <div>
                  <p className="text-gray-500 text-sm">{stat.name}</p>
                  <p className={`text-2xl font-bold ${stat.color}`}>{stat.detectionCount}</p>
                </div>
              </div>

              {/* 조치 상태별 칩 */}
              {stat.actionStats && (
                <div className="flex items-center gap-1.5 flex-wrap">
                  {/* 미처리 */}
                  <div className="flex items-center gap-1 bg-gray-100 px-2 py-1 rounded-full">
                    <div className="w-2 h-2 bg-gray-400 rounded-full"></div>
                    <span className="text-xs font-medium text-gray-700">
                      {stat.actionStats.pending || 0}
                    </span>
                  </div>

                  {/* 승인 */}
                  <div className="flex items-center gap-1 bg-blue-100 px-2 py-1 rounded-full">
                    <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                    <span className="text-xs font-medium text-blue-700">
                      {stat.actionStats.approved || 0}
                    </span>
                  </div>

                  {/* 거절 */}
                  <div className="flex items-center gap-1 bg-red-100 px-2 py-1 rounded-full">
                    <div className="w-2 h-2 bg-red-500 rounded-full"></div>
                    <span className="text-xs font-medium text-red-700">
                      {stat.actionStats.rejected || 0}
                    </span>
                  </div>

                  {/* 완료 */}
                  <div className="flex items-center gap-1 bg-green-100 px-2 py-1 rounded-full">
                    <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                    <span className="text-xs font-medium text-green-700">
                      {stat.actionStats.completed || 0}
                    </span>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* 시간대별 차트 */}
      <HourlyChart data={hourlyStats} loading={loading} />

      {/* 탐지영역별 탐지 통계 */}
      {dashboardStats?.detectionAreaStats && dashboardStats.detectionAreaStats.length > 0 && (
        <div>
          <h2 className="text-lg font-semibold text-gray-800 mb-4">탐지영역별 현황</h2>
          <div className="grid grid-cols-2 gap-5">
            {dashboardStats.detectionAreaStats.map((area) => (
              <div
                key={area.detectionAreaId}
                className="bg-white rounded-xl shadow-sm hover:shadow-md transition-all duration-200 p-8 border border-gray-100 min-h-[220px]"
              >
                <div className="flex items-start gap-8 h-full">
                  {/* 왼쪽: 원형 카운트 (입체적 스타일) */}
                  <div
                    className="flex-shrink-0 w-40 h-40 rounded-full flex flex-col items-center justify-center shadow-lg border-4"
                    style={{
                      backgroundColor: `${area.colorCode || '#6B7280'}20`,
                      borderColor: area.colorCode || '#6B7280',
                      boxShadow: `0 4px 14px ${area.colorCode || '#6B7280'}30, inset 0 2px 4px ${area.colorCode || '#6B7280'}10`
                    }}
                  >
                    <span
                      className="text-base font-medium"
                      style={{ color: area.colorCode || '#6B7280' }}
                    >
                      {area.detectionAreaName}
                    </span>
                    <span
                      className="text-5xl font-bold"
                      style={{ color: area.colorCode || '#6B7280' }}
                    >
                      {area.detectionCount.toLocaleString()}
                    </span>
                    <span
                      className="text-sm opacity-75"
                      style={{ color: area.colorCode || '#6B7280' }}
                    >
                      건
                    </span>
                  </div>

                  {/* 오른쪽: 시나리오 목록 */}
                  <div className="flex-1 min-w-0">
                    <h3 className="font-medium text-gray-700 text-base mb-4 flex items-center gap-2">
                      <ChartBarIcon className="h-5 w-5 text-gray-400" />
                      탐지 시나리오 Top 5
                    </h3>

                    {area.topScenarios && area.topScenarios.length > 0 ? (
                      <ul className="space-y-1.5">
                        {area.topScenarios.map((scenario, idx) => (
                          <li
                            key={scenario.scenarioId}
                            className="flex items-start justify-between text-sm"
                          >
                            <span className="text-gray-600 truncate flex-1 mr-3">
                              <span className="text-gray-400 mr-2 font-medium">{idx + 1}.</span>
                              {scenario.scenarioName}
                            </span>
                            <span
                              className={`font-semibold px-3 py-1.5 rounded-full text-xs ${scenario.riskLevelId === 'BLOCK' ? 'bg-red-100 text-red-700' :
                                  scenario.riskLevelId === 'REVIEW' ? 'bg-orange-100 text-orange-700' :
                                    scenario.riskLevelId === 'INTENSIVE' ? 'bg-yellow-100 text-yellow-700' :
                                      'bg-green-100 text-green-700'
                                }`}
                            >
                              {scenario.count}
                            </span>
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <p className="text-sm text-gray-400 italic">탐지된 시나리오가 없습니다</p>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 데이터 없을 때 */}
      {!loading && (!dashboardStats?.detectionAreaStats || dashboardStats.detectionAreaStats.length === 0) && (
        <div className="bg-gray-50 rounded-xl p-12 text-center border border-gray-100">
          <ShieldExclamationIcon className="h-12 w-12 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-500">탐지영역별 데이터가 없습니다</h3>
          <p className="text-sm text-gray-400 mt-1">시뮬레이션을 실행하면 탐지 현황이 표시됩니다</p>
        </div>
      )}
    </div>
  );
}

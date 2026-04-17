"use client";

import { useState, useEffect, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { AxiosError } from "axios";
import {
  fetchEntityHistory,
  fetchDetectedScenarios,
  fetchEntityGraph,
  fetchEntityProfile,
  EntityHistory,
  DetectionRecord,
  ActivityLog,
  EntityGraph,
  EntityProfile,
} from "@/app/detections/api";
import EntityRelationshipGraph from "@/components/EntityRelationshipGraph";
import {
  MagnifyingGlassIcon,
  UserCircleIcon,
  ClockIcon,
  DocumentTextIcon,
  ExclamationTriangleIcon,
  TableCellsIcon,
  ChartBarIcon,
  LinkIcon,
} from "@heroicons/react/24/outline";

type TabType = "detections" | "attributes" | "activities" | "connections";

// 에러 메세지 추출 함수
function getErrorMessage(error: unknown): string {
  if (error instanceof AxiosError) {
    return error.response?.data?.message || error.message || "알 수 없는 오류";
  }
  if (error instanceof Error) {
    return error.message;
  }
  return "알 수 없는 오류";
}

function formatDateTime(dateStr: string | null | undefined): string {
  if (!dateStr) return "-";
  try {
    const d = new Date(dateStr);
    return d.toLocaleString("ko-KR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  } catch {
    return dateStr;
  }
}

function DetectionTypeBadge({ type }: { type: string }) {
  if (type === "SCENARIO") {
    return (
      <span className="px-2 py-1 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
        시나리오
      </span>
    );
  } else if (type === "RULE") {
    return (
      <span className="px-2 py-1 rounded-full text-xs font-medium bg-red-100 text-red-800">
        룰
      </span>
    );
  } else if (type === "AGGREGATE") {
    return (
      <span className="px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
        집계
      </span>
    );
  }
  return (
    <span className="px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
      {type}
    </span>
  );
}

function StatsCard({ label, value, icon: Icon, color }: { label: string; value: number | string; icon: React.ElementType; color: string }) {
  return (
    <div className="bg-white rounded-xl p-4 shadow-sm border border-gray-100">
      <div className="flex items-center gap-3">
        <div className={`w-10 h-10 rounded-lg ${color} flex items-center justify-center`}>
          <Icon className="h-5 w-5 text-white" />
        </div>
        <div>
          <p className="text-sm text-gray-500">{label}</p>
          <p className="text-xl font-bold text-gray-900">{value}</p>
        </div>
      </div>
    </div>
  );
}

function EntityHistoryPageContent() {
  const searchParams = useSearchParams();
  const [searchKey, setSearchKey] = useState("");
  const [originalCompositeKey, setOriginalCompositeKey] = useState<string | null>(null); // 복합키 원본 저장
  const [entityId, setEntityId] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<TabType>("detections");
  const [detectionFilter, setDetectionFilter] = useState<"all" | "SCENARIO" | "RULE">("all");

  // URL 파라미터 처리 (?entityId=xxx 또는 ?groupKey=xxx)
  const groupKeyParam = searchParams.get("groupKey");
  const entityIdParam = searchParams.get("entityId");
  
  useEffect(() => {
    const paramEntityId = entityIdParam || groupKeyParam;
    if (paramEntityId) {
      // 복합키인 경우 첫 번째 부분만 검색창에 표시
      const displayKey = paramEntityId.includes("|") 
        ? paramEntityId.split("|")[0] 
        : paramEntityId;
      
      setSearchKey(displayKey);
      setEntityId(paramEntityId); // 실제 조회는 전체 복합키로
      
      // 복합키인 경우 원본 저장
      if (paramEntityId.includes("|")) {
        setOriginalCompositeKey(paramEntityId);
      }
    }
  }, [entityIdParam, groupKeyParam]);

  // 최근 탐지된 엔티티 목록
  const { data: recentScenarios } = useQuery({
    queryKey: ["recentEntitiesForHistory"],
    queryFn: () => fetchDetectedScenarios({ limit: 30 }),
  });

  // group_key에서 첫 번째 엔티티 ID 추출 (예: EMP004|330-444-555666 -> EMP004)
  const extractEntityIds = (groupKeys: string[]): string[] => {
    const entitySet = new Set<string>();
    groupKeys.forEach((gk) => {
      const parts = gk.split("|");
      parts.forEach((part) => {
        if (part && part.trim()) {
          entitySet.add(part.trim());
        }
      });
    });
    return Array.from(entitySet).slice(0, 20);
  };

  const recentEntityIds = recentScenarios
    ? extractEntityIds(recentScenarios.map((s) => s.groupKey))
    : [];

  // 엔티티 행적 조회
  const { data: entityHistory, isLoading, error } = useQuery({
    queryKey: ["entityHistory", entityId],
    queryFn: () => fetchEntityHistory(entityId!, { detectionLimit: 100, activityLimit: 200 }),
    enabled: !!entityId,
    retry: false, // 실패 시 재시도 안 함 (바로 에러 표시)
  });

  // 엔티티 프로필 조회 (속성 정보용)
  const { data: entityProfile } = useQuery({
    queryKey: ["entityProfile", entityHistory?.entityId],
    queryFn: () => fetchEntityProfile(entityHistory!.entityId, 100),
    enabled: !!entityHistory?.entityId,
    retry: false,
  });

  // 엔티티 그래프 (연결 관계) 조회
  const { data: entityGraph, isLoading: isGraphLoading } = useQuery({
    queryKey: ["entityGraph", entityHistory?.entityType, entityId],
    queryFn: () => fetchEntityGraph(entityHistory!.entityType!, entityId!, 2, 10),
    enabled: !!entityHistory?.entityType && !!entityId,
    retry: false,
  });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchKey.trim()) {
      const trimmedKey = searchKey.trim();
      setEntityId(trimmedKey);
      // 복합키인 경우 원본 저장
      if (trimmedKey.includes("|")) {
        setOriginalCompositeKey(trimmedKey);
      } else {
        setOriginalCompositeKey(null);
      }
    }
  };

  const handleEntityClick = (id: string) => {
    setSearchKey(id);
    setEntityId(id);
  };

  return (
    <div className="p-6 space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">엔티티 행적 조회</h1>
          <p className="text-sm text-gray-500 mt-1">
            엔티티 ID를 입력하여 관련된 모든 탐지 이력과 활동 로그를 확인하세요
          </p>
        </div>
      </div>

      {/* 검색 폼 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h2 className="text-lg font-semibold text-gray-800 mb-4">엔티티 ID로 검색</h2>
        <form onSubmit={handleSearch} className="flex gap-4">
          <div className="flex-1">
            <input
              type="text"
              value={searchKey}
              onChange={(e) => setSearchKey(e.target.value)}
              placeholder="엔티티 ID 입력 (예: EMP004, CUS001, ACC123 또는 복합키: EMP004|330-444-555666)"
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all"
            />
          </div>
          <button
            type="submit"
            disabled={!searchKey.trim()}
            className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
          >
            <MagnifyingGlassIcon className="h-5 w-5" />
            조회
          </button>
        </form>

          {/* 복합키 칩 분리 표시 */}
          {originalCompositeKey && (
            <div className="mt-4 flex flex-wrap gap-2">
              {originalCompositeKey.split("|").map((part) => part.trim()).filter(Boolean).map((id, idx) => (
                <button
                  key={`${id}-${idx}`}
                  onClick={() => handleEntityClick(id)}
                  className={`px-3 py-1.5 rounded-lg text-sm font-mono transition-colors border shadow-sm ${
                    entityId === id
                      ? "bg-blue-600 text-white border-blue-600"
                      : "bg-white hover:bg-blue-100 text-blue-700 hover:text-blue-800 border-blue-200"
                  }`}
                >
                  {id}
                </button>
              ))}
            </div>
          )}

          {/* 최근 엔티티 ID 목록 */}
          {!entityId && recentEntityIds.length > 0 && (
            <div className="mt-6">
              <p className="text-sm text-gray-500 mb-3">최근 탐지된 엔티티:</p>
              <div className="flex flex-wrap gap-2">
                {recentEntityIds.map((id) => (
                  <button
                    key={id}
                    onClick={() => handleEntityClick(id)}
                    className="px-3 py-1.5 bg-gray-100 hover:bg-blue-100 text-gray-700 hover:text-blue-700 rounded-lg text-sm font-mono transition-colors"
                  >
                    {id}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* 로딩/에러 상태 */}
        {isLoading && (
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-12 text-center">
            <div className="animate-spin w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full mx-auto mb-4" />
            <p className="text-gray-600">엔티티 행적을 조회 중입니다...</p>
          </div>
        )}

        {error && (
          <div className="bg-red-50 rounded-xl shadow-sm border border-red-200 p-8 text-center">
            <ExclamationTriangleIcon className="h-12 w-12 text-red-500 mx-auto mb-4" />
            <p className="text-red-700 font-medium mb-2">조회 중 오류가 발생했습니다.</p>
            <p className="text-red-600 text-sm">{getErrorMessage(error)}</p>
          </div>
        )}

        {/* 결과 표시 */}
        {entityHistory && (
          <>
            {/* 통계 카드 */}
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
              <StatsCard
                label="시나리오 탐지"
                value={entityHistory.stats.scenarioCount}
                icon={ExclamationTriangleIcon}
                color="bg-purple-600"
              />
              <StatsCard
                label="활동 로그"
                value={entityHistory.stats.totalActivities}
                icon={TableCellsIcon}
                color="bg-green-600"
              />
              <StatsCard
                label="탐지율"
                value={entityHistory.stats.totalActivities > 0
                  ? `${Math.round((entityHistory.stats.scenarioCount / entityHistory.stats.totalActivities) * 100)}%`
                  : "0%"}
                icon={ChartBarIcon}
                color="bg-blue-600"
              />
            </div>

            {/* 기간 정보 */}
            {(entityHistory.stats.firstActivityAt || entityHistory.stats.lastActivityAt) && (
              <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4 flex items-center gap-4 text-sm text-gray-600">
                <ClockIcon className="h-5 w-5 text-gray-400" />
                <span>
                  활동 기간: {formatDateTime(entityHistory.stats.firstActivityAt)} ~ {formatDateTime(entityHistory.stats.lastActivityAt)}
                </span>
              </div>
            )}

            {/* 탭 네비게이션 */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
              <div className="border-b border-gray-200">
                <nav className="flex -mb-px overflow-x-auto">
                  <button
                    onClick={() => setActiveTab("detections")}
                    className={`px-6 py-4 text-sm font-medium border-b-2 transition-colors whitespace-nowrap ${
                      activeTab === "detections"
                        ? "border-purple-500 text-purple-600"
                        : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                    }`}
                  >
                    <ExclamationTriangleIcon className="h-5 w-5 inline mr-2" />
                    탐지 이력 ({entityHistory.detections.length})
                  </button>
                  <button
                    onClick={() => setActiveTab("activities")}
                    className={`px-6 py-4 text-sm font-medium border-b-2 transition-colors whitespace-nowrap ${
                      activeTab === "activities"
                        ? "border-green-500 text-green-600"
                        : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                    }`}
                  >
                    <TableCellsIcon className="h-5 w-5 inline mr-2" />
                    활동 로그 ({entityHistory.activities.length})
                  </button>
                  <button
                    onClick={() => setActiveTab("attributes")}
                    className={`px-6 py-4 text-sm font-medium border-b-2 transition-colors whitespace-nowrap ${
                      activeTab === "attributes"
                        ? "border-blue-500 text-blue-600"
                        : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                    }`}
                  >
                    <DocumentTextIcon className="h-5 w-5 inline mr-2" />
                    엔티티 속성
                  </button>
                  <button
                    onClick={() => setActiveTab("connections")}
                    className={`px-6 py-4 text-sm font-medium border-b-2 transition-colors whitespace-nowrap ${
                      activeTab === "connections"
                        ? "border-indigo-500 text-indigo-600"
                        : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                    }`}
                  >
                    <LinkIcon className="h-5 w-5 inline mr-2" />
                    엔티티 연결 ({entityGraph?.edges?.length ?? 0})
                  </button>
                </nav>
              </div>

              {/* 탭 콘텐츠 */}
              <div className="p-6">
                {activeTab === "detections" && (
                  <DetectionsTab
                    detections={entityHistory.detections}
                    filter={detectionFilter}
                    onFilterChange={setDetectionFilter}
                  />
                )}
                {activeTab === "activities" && (
                  <ActivitiesTab activities={entityHistory.activities} />
                )}
                {activeTab === "attributes" && (
                  <AttributesTab attributes={entityProfile?.attributes} />
                )}
                {activeTab === "connections" && (
                  <ConnectionsTab
                    entityGraph={entityGraph}
                    isLoading={isGraphLoading}
                    entityType={entityHistory.entityType}
                    entityId={entityHistory.entityId}
                  />
                )}
              </div>
            </div>
          </>
        )}

        {/* 조회 전 안내 */}
        {!entityId && !isLoading && (
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-12 text-center">
            <UserCircleIcon className="h-16 w-16 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-500">
              엔티티 ID를 입력하여 행적을 조회하세요.
              <br />
              <span className="text-sm">
                예: EMP004, CUS001 등 group_key에 포함된 개별 엔티티 ID
              </span>
            </p>
          </div>
        )}
    </div>
  );
}

function DetectionsTab({
  detections,
  filter,
  onFilterChange,
}: {
  detections: DetectionRecord[];
  filter: "all" | "SCENARIO" | "RULE";
  onFilterChange: (filter: "all" | "SCENARIO" | "RULE") => void;
}) {
  // 필터에 따라 데이터 필터링
  const filteredDetections = filter === "all"
    ? detections
    : detections.filter(d => d.type === filter);

  const scenarioCount = detections.filter(d => d.type === "SCENARIO").length;
  const ruleCount = detections.filter(d => d.type === "RULE").length;

  return (
    <div className="space-y-4">
      {/* 필터 버튼 */}
      <div className="flex gap-2">
        <button
          onClick={() => onFilterChange("all")}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
            filter === "all"
              ? "bg-blue-600 text-white"
              : "bg-gray-100 text-gray-700 hover:bg-gray-200"
          }`}
        >
          전체 ({detections.length})
        </button>
        <button
          onClick={() => onFilterChange("SCENARIO")}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
            filter === "SCENARIO"
              ? "bg-purple-600 text-white"
              : "bg-gray-100 text-gray-700 hover:bg-gray-200"
          }`}
        >
          시나리오 ({scenarioCount})
        </button>
        <button
          onClick={() => onFilterChange("RULE")}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
            filter === "RULE"
              ? "bg-red-600 text-white"
              : "bg-gray-100 text-gray-700 hover:bg-gray-200"
          }`}
        >
          룰 ({ruleCount})
        </button>
      </div>

      {/* 테이블 */}
      {filteredDetections.length === 0 ? (
        <div className="text-center py-12 text-gray-500">
          {filter === "all" ? "탐지 이력이 없습니다." : `${filter === "SCENARIO" ? "시나리오" : "룰"} 탐지 이력이 없습니다.`}
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  유형
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  탐지명
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Group Key
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  탐지 시각
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  상세
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredDetections.map((det, idx) => (
                <tr key={`${det.type}-${det.detectionId}-${idx}`} className="hover:bg-gray-50">
                  <td className="px-4 py-3 whitespace-nowrap">
                    <DetectionTypeBadge type={det.type} />
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-900">
                    {det.detectionName ? `${det.detectionName}(${det.detectionId})` : det.detectionId}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-sm font-mono text-gray-600">
                    {det.groupKey}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                    {formatDateTime(det.detectedAt)}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">
                    {det.details && Object.keys(det.details).length > 0 &&
                      // RULE 타입이거나, SCENARIO인데 transactionId가 있는 경우만 상세보기 표시
                      (det.type === 'RULE' || (det.type === 'SCENARIO' && det.details.transactionId)) ? (
                        <details className="cursor-pointer">
                          <summary className="text-blue-600 hover:underline">
                            상세 보기
                          </summary>
                          <div className="mt-2 p-3 bg-gray-50 rounded text-xs space-y-1">
                            {det.type === 'RULE' && (
                              <>
                                {det.details.evaluationMode && (
                                  <div className="flex gap-2">
                                    <span className="font-medium text-gray-700 min-w-[80px]">평가 모드:</span>
                                    <span className={`px-2 py-0.5 rounded text-[11px] font-medium ${
                                      det.details.evaluationMode === 'SINGLE_ROW' 
                                        ? 'bg-blue-100 text-blue-700' 
                                        : 'bg-purple-100 text-purple-700'
                                    }`}>
                                      {det.details.evaluationMode as string}
                                    </span>
                                  </div>
                                )}
                                {det.details.eventDt && (
                                  <div className="flex gap-2">
                                    <span className="font-medium text-gray-700 min-w-[80px]">이벤트 시간:</span>
                                    <span className="text-gray-600">{det.details.eventDt as string}</span>
                                  </div>
                                )}
                                {det.details.matchedCount && (
                                  <div className="flex gap-2">
                                    <span className="font-medium text-gray-700 min-w-[80px]">매칭 수:</span>
                                    <span className="text-gray-600">{det.details.matchedCount as string}</span>
                                  </div>
                                )}
                                {det.details.thresholdCount && (
                                  <div className="flex gap-2">
                                    <span className="font-medium text-gray-700 min-w-[80px]">임계값:</span>
                                    <span className="text-gray-600">{det.details.thresholdCount as string}</span>
                                  </div>
                                )}
                                {det.details.transactionId && (
                                  <div className="flex gap-2">
                                    <span className="font-medium text-gray-700 min-w-[80px]">트랜잭션ID:</span>
                                    <span className="text-gray-600 font-mono text-[11px]">{det.details.transactionId as string}</span>
                                  </div>
                                )}
                              </>
                            )}
                            {det.type === 'SCENARIO' && det.details.transactionId ? (
                              <div className="flex gap-2">
                                <span className="font-medium text-gray-700 min-w-[80px]">트랜잭션ID:</span>
                                <span className="text-gray-600 font-mono text-[11px]">{det.details.transactionId as string}</span>
                              </div>
                            ) : null}
                          </div>
                        </details>
                      ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function ActivitiesTab({ activities }: { activities: ActivityLog[] }) {
  if (activities.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500">
        활동 로그가 없습니다.
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              ID
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              이벤트 시각
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              트랜잭션 ID
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              이벤트 데이터
            </th>
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {activities.map((act) => (
            <tr key={act.eventStreamId} className="hover:bg-gray-50">
              <td className="px-4 py-3 whitespace-nowrap text-sm font-mono text-gray-600">
                {act.eventStreamId}
              </td>
              <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                {formatDateTime(act.eventDt)}
              </td>
              <td className="px-4 py-3 whitespace-nowrap text-sm font-mono text-gray-600">
                {act.transactionId || "-"}
              </td>
              <td className="px-4 py-3 text-sm text-gray-500">
                {act.eventData && Object.keys(act.eventData).length > 0 && (
                  <details className="cursor-pointer">
                    <summary className="text-blue-600 hover:underline">
                      데이터 보기
                    </summary>
                    <pre className="mt-2 p-2 bg-gray-100 rounded text-xs overflow-auto max-w-lg max-h-48">
                      {JSON.stringify(act.eventData, null, 2)}
                    </pre>
                  </details>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function AttributesTab({ attributes }: { attributes: Record<string, unknown> | null | undefined }) {
  if (!attributes || Object.keys(attributes).length === 0) {
    return (
      <div className="text-center py-12 text-gray-500">
        엔티티 속성 정보가 없습니다.
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      {Object.entries(attributes).map(([key, value]) => (
        <div
          key={key}
          className="border border-gray-200 rounded-lg p-4 hover:border-blue-300 transition-colors"
        >
          <div className="text-sm font-medium text-gray-500 mb-1">{key}</div>
          <div className="text-base text-gray-900 font-mono break-all">
            {typeof value === "object" ? JSON.stringify(value, null, 2) : String(value)}
          </div>
        </div>
      ))}
    </div>
  );
}

function ConnectionsTab({
  entityGraph,
  isLoading,
  entityType,
  entityId,
}: {
  entityGraph: EntityGraph | undefined;
  isLoading: boolean;
  entityType: string | null;
  entityId: string;
}) {
  if (!entityType) {
    return (
      <div className="text-center py-12 text-gray-500">
        <LinkIcon className="h-12 w-12 mx-auto mb-4 text-gray-300" />
        <p>엔티티 타입 정보가 없습니다.</p>
        <p className="text-sm">entity_attributes 테이블에 엔티티가 등록되어야 연결 관계를 조회할 수 있습니다.</p>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="text-center py-12">
        <div className="animate-spin w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full mx-auto mb-4" />
        <p className="text-gray-600">연결 관계를 조회 중입니다...</p>
      </div>
    );
  }

  if (!entityGraph || entityGraph.nodes.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500">
        <LinkIcon className="h-12 w-12 mx-auto mb-4 text-gray-300" />
        <p>연결된 엔티티가 없습니다.</p>
      </div>
    );
  }

  return (
    <div>
      <EntityRelationshipGraph
        nodes={entityGraph.nodes}
        edges={entityGraph.edges}
        rootEntityId={entityId}
      />
    </div>
  );
}

export default function EntityHistoryPage() {
  return (
    <Suspense fallback={<div className="p-6">불러오는 중…</div>}>
      <EntityHistoryPageContent />
    </Suspense>
  );
}

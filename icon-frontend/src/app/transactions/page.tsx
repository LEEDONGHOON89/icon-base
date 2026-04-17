"use client";

import { useState, useEffect, Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { trackTransaction, getRecentTransactions } from "./api";
import { TransactionTrackingInfo, PipelineStageInfo, TransactionListItem } from "@/types/transaction";
import {
  MagnifyingGlassIcon,
  ExclamationCircleIcon,
  ClockIcon,
  ChartBarIcon
} from "@heroicons/react/24/outline";

// 날짜/시간 포맷팅
function formatDateTime(dateStr: string | null | undefined): string {
  if (!dateStr) return "-";
  try {
    const date = new Date(dateStr);
    return new Intl.DateTimeFormat("ko-KR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
    }).format(date);
  } catch {
    return dateStr;
  }
}

// JSON 데이터 포맷팅
function formatJson(data: any): string {
  try {
    return JSON.stringify(data, null, 2);
  } catch {
    return String(data);
  }
}

function TransactionsPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [activeTab, setActiveTab] = useState<"list" | "search">("search");
  const [searchTxId, setSearchTxId] = useState("");
  const [activeTxId, setActiveTxId] = useState<string | null>(null);
  const [activeStage, setActiveStage] = useState<string>("overview");

  // URL 파라미터 처리 (tab=search&txId=xxx)
  useEffect(() => {
    const tab = searchParams.get("tab");
    const txId = searchParams.get("txId");

    // tab 파라미터가 "search"이면 검색 탭으로 전환
    if (tab === "search") {
      setActiveTab("search");
    }

    // txId 파라미터가 있으면 자동으로 검색
    if (txId) {
      setSearchTxId(txId);
      setActiveTxId(txId);
      setActiveStage("overview");
    }
  }, [searchParams]);

  // 최근 트랜잭션 목록 조회
  const {
    data: recentTransactions,
    isLoading: isLoadingList,
    error: listError,
  } = useQuery({
    queryKey: ["recent-transactions"],
    queryFn: getRecentTransactions,
    enabled: activeTab === "list",
  });

  // 트랜잭션 추적 데이터 조회
  const {
    data: trackingInfo,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ["transaction-tracking", activeTxId],
    queryFn: () => trackTransaction(activeTxId!),
    enabled: !!activeTxId,
  });

  // 검색 실행
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchTxId.trim()) {
      setActiveTxId(searchTxId.trim());
      setActiveStage("overview");
    }
  };

  // 목록에서 트랜잭션 클릭 시 추적 탭으로 이동
  const handleSelectTransaction = (txId: string) => {
    // URL 업데이트
    router.push(`/transactions?tab=search&txId=${encodeURIComponent(txId)}`);
  };

  // 파이프라인 단계 정보 계산
  const stageInfoList: PipelineStageInfo[] = trackingInfo
    ? [
        {
          stage: "PREP-1",
          label: "데이터 수집/변환",
          description: "원본 데이터를 표준 형식으로 변환",
          count: trackingInfo.mappedStorages.length,
          hasData: trackingInfo.mappedStorages.length > 0,
        },
        {
          stage: "DET-1",
          label: "이벤트 스트림",
          description: "표준화된 이벤트 생성",
          count: trackingInfo.eventStreams.length,
          hasData: trackingInfo.eventStreams.length > 0,
        },
        {
          stage: "DET-2-1",
          label: "센서 탐지",
          description: "센서(룰/집계) 조건 평가",
          count: trackingInfo.detectRules.length,
          hasData: trackingInfo.detectRules.length > 0,
        },
        {
          stage: "DET-2-2",
          label: "시나리오 탐지",
          description: "복합 조건 시나리오 판정",
          count: trackingInfo.detectScenarios.length,
          hasData: trackingInfo.detectScenarios.length > 0,
        },
      ]
    : [];

  return (
    <div className="p-6 space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">트랜잭션 추적</h1>
          <p className="text-sm text-gray-500 mt-1">
            트랜잭션 ID로 전체 파이프라인을 추적하고 각 단계별 처리 결과를 확인합니다.
          </p>
        </div>
      </div>

      {/* 탭 네비게이션 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200">
        <div className="border-b">
          <nav className="flex gap-4 px-6" aria-label="Tabs">
            <button
              onClick={() => setActiveTab("search")}
              className={`py-4 px-1 border-b-2 font-medium text-sm flex items-center gap-2 ${
                activeTab === "search"
                  ? "border-blue-500 text-blue-600"
                  : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
              }`}
            >
              <MagnifyingGlassIcon className="w-5 h-5" />
              트랜잭션 검색
            </button>
            <button
              onClick={() => setActiveTab("list")}
              className={`py-4 px-1 border-b-2 font-medium text-sm flex items-center gap-2 ${
                activeTab === "list"
                  ? "border-blue-500 text-blue-600"
                  : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
              }`}
            >
              <ClockIcon className="w-5 h-5" />
              최근 트랜잭션 목록
            </button>
          </nav>
        </div>

        {/* 탭 콘텐츠 */}
        <div className="p-6">
          {activeTab === "list" && (
            <RecentTransactionsTab
              transactions={recentTransactions}
              isLoading={isLoadingList}
              error={listError}
              onSelectTransaction={handleSelectTransaction}
            />
          )}

          {activeTab === "search" && (
            <div>
              {/* 검색 폼 */}
              <form onSubmit={handleSearch} className="flex gap-4 mb-6">
                <div className="flex-1">
                  <label htmlFor="txId" className="block text-sm font-medium text-gray-700 mb-2">
                    트랜잭션 ID
                  </label>
                  <input
                    id="txId"
                    type="text"
                    value={searchTxId}
                    onChange={(e) => setSearchTxId(e.target.value)}
                    placeholder="트랜잭션 ID를 입력하세요 (예: TX12345, ORD98765)"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
                <div className="flex items-end">
                  <button
                    type="submit"
                    disabled={!searchTxId.trim() || isLoading}
                    className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed flex items-center gap-2"
                  >
                    <MagnifyingGlassIcon className="w-5 h-5" />
                    추적
                  </button>
                </div>
              </form>

              {/* 로딩 상태 */}
              {isLoading && (
                <div className="bg-blue-50 p-4 rounded-lg flex items-center gap-3">
                  <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-600"></div>
                  <span className="text-blue-700">트랜잭션 추적 중...</span>
                </div>
              )}

              {/* 에러 상태 */}
              {error && (
                <div className="bg-red-50 p-4 rounded-lg flex items-start gap-3">
                  <ExclamationCircleIcon className="w-5 h-5 text-red-600 mt-0.5" />
                  <div>
                    <h3 className="text-red-800 font-medium">조회 실패</h3>
                    <p className="text-red-600 text-sm mt-1">
                      {error instanceof Error ? error.message : "트랜잭션을 찾을 수 없습니다."}
                    </p>
                  </div>
                </div>
              )}

              {/* 추적 결과 */}
              {trackingInfo && (
                <div className="space-y-6">
                  {/* 요약 정보 */}
                  <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
                    <h2 className="text-lg font-semibold text-gray-900 mb-4">트랜잭션 요약</h2>
                    <dl className="grid grid-cols-2 md:grid-cols-4 gap-4">
                      <div>
                        <dt className="text-sm font-medium text-gray-500">트랜잭션 ID</dt>
                        <dd className="mt-1 text-sm text-gray-900 font-mono">{trackingInfo.transactionId}</dd>
                      </div>
                      <div>
                        <dt className="text-sm font-medium text-gray-500">데이터소스</dt>
                        <dd className="mt-1 text-sm text-gray-900">
                          {trackingInfo.dataSourceName || trackingInfo.dataSourceId}
                        </dd>
                      </div>
                      <div>
                        <dt className="text-sm font-medium text-gray-500">최초 수집</dt>
                        <dd className="mt-1 text-sm text-gray-900">{formatDateTime(trackingInfo.firstSeenAt)}</dd>
                      </div>
                      <div>
                        <dt className="text-sm font-medium text-gray-500">최종 처리</dt>
                        <dd className="mt-1 text-sm text-gray-900">{formatDateTime(trackingInfo.lastSeenAt)}</dd>
                      </div>
                    </dl>
                  </div>

                  {/* 파이프라인 타임라인 */}
                  <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
                    <h2 className="text-lg font-semibold text-gray-900 mb-6">파이프라인 흐름</h2>
                    <div className="relative">
                      {/* 진행선 */}
                      <div className="absolute top-8 left-0 right-0 h-0.5 bg-gray-200"></div>

                      {/* 단계들 */}
                      <div className="relative grid grid-cols-4 gap-4">
                        {stageInfoList.map((stage, index) => (
                          <button
                            key={stage.stage}
                            onClick={() => setActiveStage(stage.stage)}
                            className={`relative flex flex-col items-center ${
                              activeStage === stage.stage ? "scale-105" : ""
                            } transition-transform`}
                          >
                            {/* 노드 */}
                            <div
                              className={`w-16 h-16 rounded-full border-4 flex items-center justify-center font-bold z-10 ${
                                stage.hasData
                                  ? "bg-green-500 border-green-600 text-white"
                                  : "bg-gray-100 border-gray-300 text-gray-400"
                              } ${activeStage === stage.stage ? "ring-4 ring-blue-300" : ""}`}
                            >
                              {stage.count}
                            </div>

                            {/* 레이블 */}
                            <div className="mt-3 text-center">
                              <div className="text-xs font-medium text-gray-500">{stage.stage}</div>
                              <div className="text-sm font-semibold text-gray-900 mt-1">{stage.label}</div>
                              <div className="text-xs text-gray-500 mt-1">{stage.description}</div>
                            </div>
                          </button>
                        ))}
                      </div>
                    </div>
                  </div>

                  {/* 상세 데이터 탭 */}
                  <div className="bg-white rounded-xl shadow-sm border border-gray-200">
                    {/* 탭 헤더 */}
                    <div className="border-b">
                      <nav className="flex gap-4 px-6" aria-label="Tabs">
                        <button
                          onClick={() => setActiveStage("overview")}
                          className={`py-4 px-1 border-b-2 font-medium text-sm ${
                            activeStage === "overview"
                              ? "border-blue-500 text-blue-600"
                              : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                          }`}
                        >
                          전체 개요
                        </button>
                        {stageInfoList.map((stage) => (
                          <button
                            key={stage.stage}
                            onClick={() => setActiveStage(stage.stage)}
                            className={`py-4 px-1 border-b-2 font-medium text-sm flex items-center gap-2 ${
                              activeStage === stage.stage
                                ? "border-blue-500 text-blue-600"
                                : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                            }`}
                          >
                            {stage.label}
                            <span
                              className={`px-2 py-0.5 rounded-full text-xs ${
                                stage.hasData ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-500"
                              }`}
                            >
                              {stage.count}
                            </span>
                          </button>
                        ))}
                      </nav>
                    </div>

                    {/* 탭 콘텐츠 */}
                    <div className="p-6">
                      {activeStage === "overview" && (
                        <div className="space-y-6">
                          <h3 className="text-lg font-semibold text-gray-900">전체 처리 흐름</h3>
                          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                            {stageInfoList.map((stage) => (
                              <div
                                key={stage.stage}
                                className={`p-4 rounded-lg border-2 ${
                                  stage.hasData
                                    ? "border-green-200 bg-green-50"
                                    : "border-gray-200 bg-gray-50"
                                }`}
                              >
                                <div className="flex items-center justify-between mb-2">
                                  <span className="text-sm font-medium text-gray-600">{stage.stage}</span>
                                  <span
                                    className={`text-2xl font-bold ${
                                      stage.hasData ? "text-green-600" : "text-gray-400"
                                    }`}
                                  >
                                    {stage.count}
                                  </span>
                                </div>
                                <h4 className="font-semibold text-gray-900">{stage.label}</h4>
                                <p className="text-sm text-gray-600 mt-1">{stage.description}</p>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}

                      {activeStage === "PREP-1" && (
                        <StageDataTable
                          title="데이터 수집/변환 (PREP-1)"
                          data={trackingInfo.mappedStorages}
                          columns={[
                            { key: "mappedDataStorageId", label: "Storage ID" },
                            { key: "landingRecordId", label: "Landing Record ID" },
                            { key: "rowIndex", label: "Row Index" },
                            { key: "regDt", label: "수집 시각", format: formatDateTime },
                            { key: "rowData", label: "데이터", format: (v) => JSON.stringify(v) },
                          ]}
                        />
                      )}

                      {activeStage === "DET-1" && (
                        <StageDataTable
                          title="이벤트 스트림 (DET-1)"
                          data={trackingInfo.eventStreams}
                          columns={[
                            { key: "eventStreamId", label: "Event ID" },
                            { key: "groupKey", label: "Group Key" },
                            { key: "eventDt", label: "이벤트 시각", format: formatDateTime },
                            { key: "eventData", label: "이벤트 데이터", format: (v) => JSON.stringify(v) },
                          ]}
                        />
                      )}

                      {activeStage === "DET-2-1" && (
                        <StageDataTable
                          title="센서 탐지 (DET-2-1)"
                          data={trackingInfo.detectRules}
                          columns={[
                            { key: "detectRuleId", label: "Detect Rule ID" },
                            { key: "ruleId", label: "센서 ID" },
                            { key: "ruleName", label: "센서 이름" },
                            { key: "groupKey", label: "Group Key" },
                            { key: "detectedAt", label: "탐지 시각", format: formatDateTime },
                            { key: "matchedFields", label: "매칭 필드", format: (v) => JSON.stringify(v) },
                          ]}
                        />
                      )}

                      {activeStage === "DET-2-2" && (
                        <StageDataTable
                          title="시나리오 탐지 (DET-2-2)"
                          data={trackingInfo.detectScenarios}
                          columns={[
                            { key: "detectScenarioId", label: "Detect Scenario ID" },
                            { key: "scenarioId", label: "Scenario ID" },
                            { key: "groupKey", label: "Group Key" },
                            { key: "detectedAt", label: "탐지 시각", format: formatDateTime },
                          ]}
                        />
                      )}
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// 최근 트랜잭션 목록 탭 컴포넌트
interface RecentTransactionsTabProps {
  transactions?: TransactionListItem[];
  isLoading: boolean;
  error: Error | null;
  onSelectTransaction: (txId: string) => void;
}

function RecentTransactionsTab({ transactions, isLoading, error, onSelectTransaction }: RecentTransactionsTabProps) {
  if (isLoading) {
    return (
      <div className="bg-blue-50 p-4 rounded-lg flex items-center gap-3">
        <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-600"></div>
        <span className="text-blue-700">최근 트랜잭션 목록 조회 중...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 p-4 rounded-lg flex items-start gap-3">
        <ExclamationCircleIcon className="w-5 h-5 text-red-600 mt-0.5" />
        <div>
          <h3 className="text-red-800 font-medium">조회 실패</h3>
          <p className="text-red-600 text-sm mt-1">
            {error instanceof Error ? error.message : "트랜잭션 목록을 불러올 수 없습니다."}
          </p>
        </div>
      </div>
    );
  }

  if (!transactions || transactions.length === 0) {
    return (
      <div className="text-center py-12">
        <ChartBarIcon className="w-12 h-12 text-gray-400 mx-auto mb-4" />
        <p className="text-gray-500">최근 트랜잭션이 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold text-gray-900">최근 30개 트랜잭션</h3>
        <span className="text-sm text-gray-500">총 {transactions.length}건</span>
      </div>

      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                트랜잭션 ID
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                데이터소스
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                최초 수집
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                최종 처리
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                레코드 수
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                시나리오 탐지
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                동작
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {transactions.map((tx) => (
              <tr key={tx.transactionId} className="hover:bg-gray-50">
                <td className="px-4 py-3 text-sm font-mono text-gray-900">
                  {tx.transactionId}
                </td>
                <td className="px-4 py-3 text-sm text-gray-900">
                  {tx.dataSourceName || tx.dataSourceId}
                </td>
                <td className="px-4 py-3 text-sm text-gray-900">
                  {formatDateTime(tx.firstSeenAt)}
                </td>
                <td className="px-4 py-3 text-sm text-gray-900">
                  {formatDateTime(tx.lastSeenAt)}
                </td>
                <td className="px-4 py-3 text-sm text-gray-900">
                  {tx.totalRecords}
                </td>
                <td className="px-4 py-3 text-sm">
                  {tx.scenariosDetected > 0 ? (
                    <span className="px-2 py-1 bg-red-100 text-red-700 rounded-full text-xs font-medium">
                      {tx.scenariosDetected}건 탐지
                    </span>
                  ) : (
                    <span className="text-gray-500">-</span>
                  )}
                </td>
                <td className="px-4 py-3 text-sm">
                  <button
                    onClick={() => onSelectTransaction(tx.transactionId)}
                    className="text-blue-600 hover:text-blue-700 font-medium flex items-center gap-1"
                  >
                    <MagnifyingGlassIcon className="w-4 h-4" />
                    추적
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// 단계별 데이터 테이블 컴포넌트
interface Column {
  key: string;
  label: string;
  format?: (value: any) => string;
}

interface StageDataTableProps {
  title: string;
  data: any[];
  columns: Column[];
}

function StageDataTable({ title, data, columns }: StageDataTableProps) {
  if (data.length === 0) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500">이 단계에서 처리된 데이터가 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <h3 className="text-lg font-semibold text-gray-900">{title}</h3>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              {columns.map((col) => (
                <th
                  key={col.key}
                  className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                >
                  {col.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {data.map((row, rowIndex) => (
              <tr key={rowIndex} className="hover:bg-gray-50">
                {columns.map((col) => {
                  const value = row[col.key];
                  const displayValue = col.format ? col.format(value) : String(value ?? "-");

                  return (
                    <td key={col.key} className="px-4 py-3 text-sm text-gray-900">
                      {typeof value === "object" && value !== null ? (
                        <details className="cursor-pointer">
                          <summary className="text-blue-600 hover:text-blue-700">
                            객체 보기
                          </summary>
                          <pre className="mt-2 p-2 bg-gray-50 rounded text-xs overflow-x-auto">
                            {formatJson(value)}
                          </pre>
                        </details>
                      ) : (
                        <span className="break-all">{displayValue}</span>
                      )}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="text-sm text-gray-500">총 {data.length}건</div>
    </div>
  );
}

export default function TransactionsPage() {
  return (
    <Suspense fallback={<div className="p-6">불러오는 중…</div>}>
      <TransactionsPageContent />
    </Suspense>
  );
}

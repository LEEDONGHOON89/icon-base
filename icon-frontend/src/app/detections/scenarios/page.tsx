"use client";

import { useState, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchDetectedScenarios, fetchScenarioDetail, type DetectedScenario, type ScenarioDetail } from "../api";
import api from "@/lib/api";
import { fetchScenario, type ScenarioWithRules } from "@/app/scenarios/api";
import { fetchCustomerInfo, fetchAccountInfo, type EntityAttribute } from "@/app/entity-attributes/api";

function toDateInput(dt: Date) {
  const pad = (n: number) => n.toString().padStart(2, "0");
  const yyyy = dt.getFullYear();
  const mm = pad(dt.getMonth() + 1);
  const dd = pad(dt.getDate());
  return `${yyyy}-${mm}-${dd}`;
}

// 분 단위를 적절한 단위로 변환 (초, 분, 시간, 일)
function formatWindowMinutes(minutes: number | null | undefined): string {
  if (minutes == null) return '-';

  // 1분 미만 (60초 미만)
  if (minutes < 1) {
    const seconds = Math.round(minutes * 60);
    return `${seconds}초`;
  }

  // 60분 미만
  if (minutes < 60) {
    return `${minutes}분`;
  }

  // 24시간(1440분) 미만
  if (minutes < 1440) {
    const hours = Math.round(minutes / 60);
    return `${hours}시간`;
  }

  // 1440분 이상 (일 단위)
  const days = Math.round(minutes / 1440);
  return `${days}일`;
}

// 숫자를 천 단위 구분자로 포맷팅
function formatNumber(value: number | null | undefined): string {
  if (value == null) return '-';
  return value.toLocaleString('ko-KR');
}

// 이벤트 JSON 필드를 카테고리별로 분류
function categorizeEventFields(eventData: Record<string, any>) {
  const categories: Record<string, Record<string, any>> = {
    transaction: {},
    customer: {},
    device: {},
    account: {},
    risk: {},
    etc: {},
  };

  Object.entries(eventData).forEach(([key, value]) => {
    const lowerKey = key.toLowerCase();

    if (
      lowerKey.includes('transaction') ||
      lowerKey.includes('trx_') ||
      lowerKey.includes('transfer') ||
      lowerKey.includes('sender') ||
      lowerKey.includes('receiver') ||
      lowerKey.includes('balance')
    ) {
      categories.transaction[key] = value;
    } else if (
      lowerKey.includes('customer') ||
      lowerKey.includes('login_fail') ||
      lowerKey.includes('new_account_days')
    ) {
      categories.customer[key] = value;
    } else if (
      lowerKey.includes('device') ||
      lowerKey.includes('source_ip') ||
      lowerKey.includes('access_country') ||
      lowerKey.includes('is_night_time') ||
      lowerKey.includes('non_face')
    ) {
      categories.device[key] = value;
    } else if (
      lowerKey.includes('account') ||
      lowerKey.includes('atm_count')
    ) {
      categories.account[key] = value;
    } else if (
      lowerKey.includes('is_high_amount') ||
      lowerKey.includes('is_transfer')
    ) {
      categories.risk[key] = value;
    } else {
      categories.etc[key] = value;
    }
  });

  return categories;
}

// 카테고리 한글 이름
const categoryNames: Record<string, string> = {
  transaction: "💰 거래 정보",
  customer: "👤 고객 정보",
  device: "📱 디바이스 정보",
  account: "💳 계좌 정보",
  risk: "⚠️ 위험 지표",
  etc: "🔧 기타",
  raw: "📄 원본",
};

// 이벤트 상세 컴포넌트 (카테고리별 탭 표시)
type EventStream = { groupKey: string; eventDt: string; eventData: Record<string, any> };

function EventDetail({ event, index }: { event: EventStream; index: number }) {
  const [activeTab, setActiveTab] = useState<string>("");
  const categories = categorizeEventFields(event.eventData);

  // 빈 카테고리 제거 및 카테고리 이름 목록 (원본 탭은 맨 끝에 추가)
  const availableCategories = [
    ...Object.entries(categories)
      .filter(([_, data]) => Object.keys(data).length > 0)
      .map(([cat]) => cat),
    'raw' // 원본 탭은 항상 맨 끝에
  ];

  // 첫 번째 탭을 기본값으로 설정
  if (!activeTab && availableCategories.length > 0) {
    setActiveTab(availableCategories[0]);
  }

  return (
    <div className="border border-gray-200 rounded-2xl overflow-hidden bg-white shadow-sm hover:shadow-md transition-shadow">
      {/* 이벤트 시각 헤더 */}
      <div className="bg-gradient-to-r from-indigo-50 to-blue-50 px-5 py-3 border-b border-blue-100">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 bg-blue-500 rounded-lg flex items-center justify-center text-white font-semibold text-sm">
            {index + 1}
          </div>
          <div className="text-sm font-medium text-gray-800">
            {new Date(event.eventDt).toLocaleString('ko-KR', {
              year: 'numeric',
              month: '2-digit',
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit',
              second: '2-digit'
            })}
          </div>
        </div>
      </div>

      {/* 탭 헤더 */}
      <div className="flex border-b bg-gradient-to-r from-gray-50 to-white overflow-x-auto">
        {availableCategories.map((cat) => (
          <button
            key={cat}
            onClick={() => setActiveTab(cat)}
            className={`px-5 py-3 text-sm font-semibold transition-all duration-200 border-b-3 whitespace-nowrap relative ${activeTab === cat
                ? "border-blue-500 text-blue-600 bg-white shadow-sm"
                : "border-transparent text-gray-600 hover:text-gray-800 hover:bg-white/50"
              }`}
          >
            {categoryNames[cat] || cat}
            {activeTab === cat && (
              <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-blue-500"></div>
            )}
          </button>
        ))}
      </div>

      {/* 탭 내용 */}
      <div className="p-5 bg-white">
        {availableCategories.map((cat) => {
          if (cat !== activeTab) return null;

          // 원본 탭인 경우
          if (cat === 'raw') {
            return (
              <div key={cat}>
                <pre className="text-xs bg-gray-50 p-4 rounded-lg overflow-x-auto border border-gray-200">
                  {JSON.stringify(event.eventData, null, 2)}
                </pre>
              </div>
            );
          }

          // 일반 카테고리 탭
          const data = categories[cat];

          return (
            <div key={cat} className="grid grid-cols-2 gap-x-8 gap-y-1">
              {Object.entries(data).map(([key, value]) => (
                <div key={key} className="flex items-start py-2 border-b border-gray-100 last:border-b-0">
                  <div className="text-sm font-semibold text-gray-600 w-1/2 flex-shrink-0">{key}:</div>
                  <div className="text-sm text-gray-900 w-1/2">
                    {typeof value === "object" ? (
                      <pre className="text-xs bg-gray-50 p-2 rounded overflow-x-auto">
                        {JSON.stringify(value, null, 2)}
                      </pre>
                    ) : (
                      <span className="break-words">{String(value)}</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default function ScenarioDetectionsPage() {
  const now = useMemo(() => new Date(), []);
  const weekAgo = useMemo(() => new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000), [now]);

  const [groupKey, setGroupKey] = useState("");
  const [startDate, setStartDate] = useState(toDateInput(weekAgo));
  const [endDate, setEndDate] = useState(toDateInput(now));
  const [submitCount, setSubmitCount] = useState(0);

  const scenariosQuery = useQuery({
    queryKey: ["detections", "scenarios-only", groupKey || "_all_", startDate, endDate, submitCount],
    queryFn: () => fetchDetectedScenarios({ groupKey: groupKey || undefined, startDate, endDate }),
    enabled: submitCount > 0,
  });

  const onSearch = () => setSubmitCount((c) => c + 1);
  const scenarios = (scenariosQuery.data || []) as DetectedScenario[];

  // Detail modal state
  const [detailParams, setDetailParams] = useState<{ groupKey: string; scenarioId: string; detectedAt: string } | null>(null);
  const detailQuery = useQuery({
    queryKey: ["scenario-detail", detailParams?.groupKey, detailParams?.scenarioId, detailParams?.detectedAt],
    queryFn: () => fetchScenarioDetail(detailParams as any),
    enabled: !!detailParams,
  });
  const closeDetail = () => {
    setDetailParams(null);
    setExpandedAggregateId(null); // 펼침 상태 초기화
  };

  // 정적 데이터 조회 (고객 정보)
  const customerQuery = useQuery({
    queryKey: ["customer-info", detailParams?.groupKey],
    queryFn: () => fetchCustomerInfo(detailParams!.groupKey),
    enabled: !!detailParams?.groupKey,
  });

  // 정적 데이터 조회 (계좌 정보 - 고객의 owned_accounts 기반)
  const accountIdsFromCustomer = useMemo(() => {
    if (!customerQuery.data?.attributes?.owned_accounts) return [];
    const owned = customerQuery.data.attributes.owned_accounts;
    return Array.isArray(owned) ? owned : [];
  }, [customerQuery.data]);

  const accountQueries = useQuery({
    queryKey: ["account-infos", accountIdsFromCustomer],
    queryFn: async () => {
      if (accountIdsFromCustomer.length === 0) return [];
      const results = await Promise.all(
        accountIdsFromCustomer.map((id: any) => fetchAccountInfo(String(id)))
      );
      return results.filter((r): r is EntityAttribute => r !== null);
    },
    enabled: accountIdsFromCustomer.length > 0,
  });

  // 펼쳐진 집계 ID 추적 (걸린 집계 테이블에서 상세 보기)
  const [expandedAggregateId, setExpandedAggregateId] = useState<string | null>(null);

  // Aggregate events detail
  type EventStream = { groupKey: string; eventDt: string; eventData: Record<string, any> };
  type AggregateDetail = {
    groupKey: string;
    aggregateId: string;
    detectedAt: string;
    windowStart: string;
    windowEnd: string;
    predicateRuleId?: string | null;
    predicateRuleName?: string | null;
    matchedEvents: EventStream[];
    matchedCount: number;
  };
  async function fetchAggDetail(params: { groupKey: string; aggregateId: string; anchor: string }): Promise<AggregateDetail> {
    const res = await api.get<AggregateDetail>("/api/v1/analytics/aggregates/detail", { params });
    return res.data;
  }

  // 펼쳐진 집계의 상세 조회
  const expandedAggQuery = useQuery({
    queryKey: ["expanded-agg", detailParams?.groupKey, expandedAggregateId],
    queryFn: async () => {
      const agg = detailQuery.data?.aggregates.find(a => a.ruleId === expandedAggregateId);
      if (!agg || !agg.detectedAt) {
        console.warn('집계에 detectedAt이 없습니다:', agg);
        return null;
      }
      try {
        return await fetchAggDetail({
          groupKey: detailParams!.groupKey,
          aggregateId: expandedAggregateId!,
          anchor: agg.detectedAt,
        });
      } catch (error) {
        console.error('매칭 이벤트 조회 실패:', error);
        throw error;
      }
    },
    enabled: !!expandedAggregateId && !!detailParams,
  });

  return (
    <div className="p-6 space-y-6">
      {/* 헤더 */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">시나리오 결과</h1>
        <p className="text-sm text-gray-500 mt-1">날짜·그룹키로 시나리오 결과를 조회합니다.</p>
      </div>

      {/* 검색 필터 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
          <div className="flex items-end gap-3">
            {/* 시작일 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">시작일</label>
              <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500" />
            </div>
            {/* 종료일 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">종료일</label>
              <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500" />
            </div>
            {/* 그룹키 */}
            <div className="flex-1 max-w-xs">
              <label className="block text-sm font-medium text-gray-700 mb-1">그룹키(group_key)</label>
              <input
                type="text"
                value={groupKey}
                onChange={(e) => setGroupKey(e.target.value)}
                placeholder="예: CUS012"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
            {/* 조회 버튼 */}
            <button onClick={onSearch} disabled={scenariosQuery.isFetching} className="px-4 py-2 bg-blue-600 text-white rounded-lg shadow hover:bg-blue-700 disabled:opacity-50">조회</button>
          </div>
      </div>

      {/* 결과 테이블 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          {scenariosQuery.isFetching ? (
            <div className="text-center py-12 text-gray-500">로딩 중…</div>
          ) : scenarios.length === 0 ? (
            <div className="text-center py-12 text-gray-500">결과가 없습니다.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">시나리오</th>
                    <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">탐지영역</th>
                    <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">그룹키</th>
                    <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">트랜잭션 ID</th>
                    <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">탐지시각</th>
                    <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">결과</th>
                    <th className="px-3 py-2 text-right text-xs font-medium text-gray-500">상세</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {scenarios.map((row) => (
                    <tr key={`${row.scenarioId}-${row.detectedAt}-${row.groupKey}`} className="hover:bg-gray-50">
                      <td className="px-3 py-2 text-sm">
                        <a
                          href={`/scenarios/${row.scenarioId}/edit`}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-blue-600 hover:text-blue-700 hover:underline"
                          title="새 창에서 시나리오 편집"
                        >
                          {row.scenarioName ? (
                            <>
                              <span className="font-medium">{row.scenarioName}</span>
                              <span className="ml-2 text-xs font-mono text-gray-500">({row.scenarioId})</span>
                            </>
                          ) : (
                            <span className="font-mono">{row.scenarioId}</span>
                          )}
                        </a>
                      </td>
                      <td className="px-3 py-2 text-sm">
                        {row.detectionAreaName ? (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-indigo-100 text-indigo-800">
                            {row.detectionAreaName}
                          </span>
                        ) : (
                          <span className="text-gray-400">-</span>
                        )}
                      </td>
                      <td className="px-3 py-2 text-sm">
                        <a
                          href={`/detections/entity-history?groupKey=${encodeURIComponent(row.groupKey)}`}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="font-mono text-blue-600 hover:text-blue-700 hover:underline"
                          title="엔티티 행적 조회 (새 탭)"
                        >
                          {row.groupKey}
                        </a>
                      </td>
                      <td className="px-3 py-2 text-sm">
                        {row.transactionId ? (
                          <a
                            href={`http://localhost:5160/transactions?tab=search&txId=${encodeURIComponent(row.transactionId)}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="font-mono text-blue-600 hover:text-blue-700 hover:underline"
                            title="트랜잭션 검색으로 이동 (새 탭)"
                          >
                            {row.transactionId}
                          </a>
                        ) : (
                          <span className="text-gray-400">-</span>
                        )}
                      </td>
                      <td className="px-3 py-2 text-sm text-gray-900">{new Date(row.detectedAt).toLocaleString()}</td>
                      <td className="px-3 py-2 text-sm">
                        {row.allPassed ? (
                          <span className="px-2 py-0.5 rounded bg-green-100 text-green-700">전체 통과</span>
                        ) : (
                          <span className="px-2 py-0.5 rounded bg-amber-100 text-amber-700">부분 통과 {typeof row.passedCount === 'number' && typeof row.aggregateCount === 'number' ? `(${row.passedCount}/${row.aggregateCount})` : ''}</span>
                        )}
                      </td>
                      <td className="px-3 py-2 text-sm text-right">
                        <button
                          className="text-blue-600 hover:underline"
                          onClick={() => setDetailParams({ groupKey: row.groupKey, scenarioId: row.scenarioId, detectedAt: row.detectedAt })}
                        >
                          상세
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
      </div>

      {/* Detail Modal */}
        {detailParams && (
          <div className="fixed inset-0 z-50 flex items-center justify-center">
            <div className="absolute inset-0 bg-black/40" onClick={closeDetail} />
            <div className="relative bg-white w-full max-w-5xl max-h-[90vh] overflow-y-auto rounded-2xl shadow-2xl border border-gray-200">
              <div className="flex items-center justify-between px-6 py-4 border-b sticky top-0 bg-gradient-to-r from-blue-600 to-indigo-600 z-10 shadow-md">
                <div>
                  <div className="text-xs text-blue-100 font-medium uppercase tracking-wide mb-1">시나리오 상세</div>
                  <div className="text-lg font-bold text-white flex items-center gap-2">
                    {detailQuery.data?.scenarioName}
                    <span className="text-sm font-mono text-blue-100">({detailQuery.data?.scenarioId})</span>
                    <span className="text-blue-100">·</span>
                    <a
                      href={`/detections/entity-history?groupKey=${encodeURIComponent(detailQuery.data?.groupKey || '')}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-sm text-blue-100 hover:text-white hover:underline font-mono"
                      title="엔티티 행적 조회 (새 탭)"
                    >
                      {detailQuery.data?.groupKey}
                    </a>
                  </div>
                </div>
                <button
                  onClick={closeDetail}
                  className="px-4 py-2 rounded-lg bg-white/20 hover:bg-white/30 text-white font-semibold transition-all shadow-sm hover:shadow-md backdrop-blur-sm"
                >
                  ✕ 닫기
                </button>
              </div>

              <div className="p-6 bg-gradient-to-br from-gray-50/50 to-blue-50/30">
                {detailQuery.isLoading ? (
                  <div className="py-20 text-center text-gray-500">
                    <div className="inline-block w-12 h-12 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mb-3"></div>
                    <div>불러오는 중…</div>
                  </div>
                ) : detailQuery.error || !detailQuery.data ? (
                  <div className="py-20 text-center">
                    <div className="inline-block p-4 bg-red-50 rounded-full mb-3">
                      <span className="text-4xl">⚠️</span>
                    </div>
                    <div className="text-red-600 font-semibold">상세를 불러오지 못했습니다.</div>
                  </div>
                ) : (
                  <>
                    {/* 1. 탐지시각 */}
                    <div className="mb-6 flex items-center gap-2">
                      <span className="text-sm font-semibold text-gray-600">탐지시각:</span>
                      <span className="text-sm font-bold text-gray-900">{new Date(detailQuery.data.detectedAt).toLocaleString('ko-KR')}</span>
                    </div>

                    {/* 2. 걸린 집계 */}
                    <div className="mb-6">
                      <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center gap-2">
                          <div className="w-8 h-8 bg-gradient-to-br from-green-500 to-emerald-500 rounded-lg flex items-center justify-center">
                            <span className="text-white text-lg">📊</span>
                          </div>
                          <h3 className="text-base font-bold text-gray-900">걸린 집계</h3>
                        </div>
                        <div className="px-3 py-1.5 bg-red-100 text-red-700 rounded-lg text-sm font-semibold">
                          탐지 {detailQuery.data.aggregates.filter(a => a.pass).length}건
                        </div>
                      </div>
                      {detailQuery.data.aggregates.length === 0 ? (
                        <div className="text-gray-500 py-8 text-center">연결된 집계가 없습니다.</div>
                      ) : (
                        <div className="space-y-4">
                          {detailQuery.data.aggregates.map((a) => (
                            <div key={a.ruleId} className="border border-gray-200 rounded-2xl overflow-hidden shadow-sm hover:shadow-md transition-all bg-white">
                              {/* 집계 정보 행 */}
                              <div className="bg-gradient-to-r from-slate-50 to-gray-50 p-4">
                                <div className="flex items-center justify-between gap-4">
                                  <div className="flex-1 grid grid-cols-6 gap-3 text-sm">
                                    <div className="col-span-2">
                                      {a.ruleName ? (
                                        <div className="flex flex-col">
                                          <span className="font-semibold text-gray-900">{a.ruleName}</span>
                                          <span className="text-xs font-mono text-gray-500 mt-0.5">{a.ruleId}</span>
                                        </div>
                                      ) : (
                                        <span className="font-mono text-gray-800 font-medium">{a.ruleId}</span>
                                      )}
                                    </div>
                                    <div className="flex flex-col">
                                      <span className="text-xs text-gray-500 mb-0.5">룰</span>
                                      <span className="text-gray-800 font-medium text-xs">
                                        {a.predicateRuleName ? `${a.predicateRuleName}` : (a.predicateRuleId || '-')}
                                      </span>
                                    </div>
                                    <div className="flex flex-col">
                                      <span className="text-xs text-gray-500 mb-0.5">연산자</span>
                                      <span className="text-gray-800 font-medium">{a.operator || '-'}</span>
                                    </div>
                                    <div className="flex flex-col items-end">
                                      <span className="text-xs text-gray-500 mb-0.5">매칭/임계</span>
                                      <span className="text-gray-900 font-semibold">{formatNumber(a.matchedCount as number)} / {formatNumber(a.thresholdCount as number)}</span>
                                    </div>
                                    <div className="flex flex-col items-end">
                                      <span className="text-xs text-gray-500 mb-0.5">집계 기간</span>
                                      <span className="text-gray-900 font-semibold">{formatWindowMinutes(a.windowMinutes)}</span>
                                    </div>
                                  </div>
                                  {a.detectedAt ? (
                                    <button
                                      onClick={() => setExpandedAggregateId(expandedAggregateId === a.ruleId ? null : a.ruleId)}
                                      className="px-3 py-1.5 text-xs font-semibold bg-blue-500 text-white hover:bg-blue-600 rounded-lg transition-colors shadow-sm"
                                    >
                                      {expandedAggregateId === a.ruleId ? '▲ 접기' : '▼ 상세'}
                                    </button>
                                  ) : (
                                    <div className="px-3 py-1.5 text-xs text-gray-400 bg-gray-100 rounded-lg">
                                      상세 없음
                                    </div>
                                  )}
                                </div>
                              </div>

                              {/* 펼쳐진 매칭 이벤트 */}
                              {expandedAggregateId === a.ruleId && (
                                <div className="p-5 bg-gradient-to-br from-blue-50/30 to-indigo-50/30 border-t border-gray-200">
                                  {expandedAggQuery.isLoading ? (
                                    <div className="text-center py-10 text-gray-500">
                                      <div className="inline-block w-8 h-8 border-3 border-blue-500 border-t-transparent rounded-full animate-spin mb-2"></div>
                                      <div>로딩 중…</div>
                                    </div>
                                  ) : expandedAggQuery.error ? (
                                    <div className="text-center py-10 text-red-600 bg-red-50 rounded-lg border border-red-200">
                                      ⚠️ 매칭 이벤트를 불러오는 중 오류가 발생했습니다.
                                    </div>
                                  ) : !expandedAggQuery.data || expandedAggQuery.data.matchedEvents.length === 0 ? (
                                    <div className="text-center py-10 text-gray-500 bg-white rounded-lg border border-gray-200">
                                      📭 이 집계에 대한 이벤트 데이터가 없습니다. (샘플 탐지 결과)
                                    </div>
                                  ) : (
                                    <div className="space-y-4">
                                      <div className="flex items-center gap-2 mb-3">
                                        <div className="h-1 w-1 bg-blue-500 rounded-full"></div>
                                        <span className="text-sm font-bold text-gray-800">
                                          매칭 이벤트 ({expandedAggQuery.data.matchedCount}건)
                                        </span>
                                        <div className="h-px flex-1 bg-gradient-to-r from-blue-200 to-transparent"></div>
                                      </div>
                                      {expandedAggQuery.data.matchedEvents.map((ev, i) => (
                                        <EventDetail key={`${ev.groupKey}-${ev.eventDt}-${i}`} event={ev} index={i} />
                                      ))}
                                    </div>
                                  )}
                                </div>
                              )}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>

                    {/* 3. 탐지 대상 정보 */}
                    {customerQuery.data || (accountQueries.data && accountQueries.data.length > 0) ? (
                      <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm">
                        <div className="flex items-center gap-2 mb-4">
                          <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-cyan-500 rounded-lg flex items-center justify-center">
                            <span className="text-white text-lg">📋</span>
                          </div>
                          <h3 className="text-base font-bold text-gray-900">탐지 대상 정보</h3>
                        </div>
                        {customerQuery.data && (
                          <div className="mb-4 bg-gradient-to-br from-blue-50 to-cyan-50 rounded-xl p-4 border border-blue-100 shadow-sm">
                            <div className="flex items-center gap-2 mb-3">
                              <span className="text-sm font-bold text-blue-700">👤 고객 정보</span>
                              <span className="text-xs text-blue-600 bg-blue-100 px-2 py-0.5 rounded">CUSTOMER</span>
                              <span className="text-xs text-gray-600 bg-white px-2 py-0.5 rounded border border-blue-200">
                                ID: {customerQuery.data.entityId}
                              </span>
                            </div>
                            <div className="grid grid-cols-2 gap-3">
                              {Object.entries(customerQuery.data.attributes)
                                .map(([key, value]) => (
                                  <div key={key} className="bg-white rounded-lg px-3 py-2 border border-blue-100 flex items-center gap-2">
                                    <span className="text-xs font-semibold text-gray-500">{key}:</span>
                                    <span className="text-sm font-medium text-gray-900 break-words">
                                      {Array.isArray(value) ? JSON.stringify(value) : String(value)}
                                    </span>
                                  </div>
                                ))}
                            </div>
                          </div>
                        )}
                        {accountQueries.data && accountQueries.data.length > 0 && (
                          <div className="bg-gradient-to-br from-purple-50 to-pink-50 rounded-xl p-4 border border-purple-100 shadow-sm">
                            <div className="flex items-center gap-2 mb-3">
                              <span className="text-sm font-bold text-purple-700">💳 계좌 정보</span>
                              <span className="text-xs text-purple-600 bg-purple-100 px-2 py-0.5 rounded">ACCOUNT</span>
                            </div>
                            <div className="space-y-3">
                              {accountQueries.data.map((account) => (
                                <div key={account.entityId} className="bg-white rounded-lg p-3 border border-purple-100">
                                  <div className="text-xs font-semibold text-purple-600 mb-2">계좌 ID: {account.entityId}</div>
                                  <div className="grid grid-cols-2 gap-3">
                                    {Object.entries(account.attributes).map(([key, value]) => (
                                      <div key={key} className="bg-purple-50/50 rounded p-2">
                                        <div className="text-xs font-semibold text-gray-500 mb-0.5">{key}</div>
                                        <div className="text-sm font-medium text-gray-900 break-words">
                                          {Array.isArray(value) ? JSON.stringify(value) : String(value)}
                                        </div>
                                      </div>
                                    ))}
                                  </div>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>
                    ) : (
                      <div className="flex items-center gap-2 text-xs text-gray-500 bg-gray-50 rounded-lg p-2 border border-gray-200">
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        <span>정적 데이터 없음: 고객/계좌 등</span>
                      </div>
                    )}
                  </>
                )}
              </div>
            </div>
          </div>
        )}
    </div>
  );
}

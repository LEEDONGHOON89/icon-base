"use client";

import { useState, useMemo, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchDetectedAggregates, type DetectedAggregate, labelAggregateOperator, type AggregateOperator } from "../api";
import api from "@/lib/api";

type EventStream = { groupKey: string; eventDt: string; eventData: Record<string, any> };
type Detail = {
  groupKey: string;
  ruleId: string;
  detectedAt: string;
  windowStart: string;
  windowEnd: string;
  predicateRuleId?: string | null;
  predicateRuleName?: string | null;
  matchedEvents: EventStream[];
  matchedCount: number;
};

async function fetchDetail(params: { groupKey: string; ruleId: string; anchor: string }): Promise<Detail> {
  const res = await api.get<Detail>("/api/v1/analytics/aggregates/detail", { params });
  return res.data;
}

function toDateInput(dt: Date) {
  const pad = (n: number) => n.toString().padStart(2, "0");
  const yyyy = dt.getFullYear();
  const mm = pad(dt.getMonth() + 1);
  const dd = pad(dt.getDate());
  return `${yyyy}-${mm}-${dd}`;
}

export default function AggregateDetectionsPage() {
  const now = useMemo(() => new Date(), []);
  const weekAgo = useMemo(() => new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000), [now]);

  const [groupKey, setGroupKey] = useState("");
  const [startDate, setStartDate] = useState(toDateInput(weekAgo));
  const [endDate, setEndDate] = useState(toDateInput(now));
  const [submitCount, setSubmitCount] = useState(0);

  const aggregatesQuery = useQuery({
    queryKey: ["detections", "aggregates-only", groupKey || "_all_", startDate, endDate, submitCount],
    queryFn: () => fetchDetectedAggregates({ groupKey: groupKey || undefined, startDate, endDate }),
    enabled: submitCount > 0,
  });

  // Handle aggregates query error
  useEffect(() => {
    if (aggregatesQuery.isError) {
      const error = aggregatesQuery.error as any;
      const message = error?.response?.data?.message || error?.message || "룰 탐지 결과 조회 중 오류가 발생했습니다.";
      alert(message);
    }
  }, [aggregatesQuery.isError, aggregatesQuery.error]);

  const onSearch = () => setSubmitCount((c) => c + 1);

  const aggregates = (aggregatesQuery.data || []) as DetectedAggregate[];

  // Detail modal state
  const [detailParams, setDetailParams] = useState<{ groupKey: string; ruleId: string; anchor: string } | null>(null);
  const detailQuery = useQuery({
    queryKey: ["agg-detail", detailParams?.groupKey, detailParams?.ruleId, detailParams?.anchor],
    queryFn: () => fetchDetail(detailParams as any),
    enabled: !!detailParams,
  });

  // Handle detail query error
  useEffect(() => {
    if (detailQuery.isError) {
      const error = detailQuery.error as any;
      const message = error?.response?.data?.message || error?.message || "상세 정보 조회 중 오류가 발생했습니다.";
      alert(message);
    }
  }, [detailQuery.isError, detailQuery.error]);

  const closeDetail = () => setDetailParams(null);

  return (
    <div className="p-6 space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">룰 탐지 결과</h1>
          <p className="text-sm text-gray-500 mt-1">날짜·그룹키로 룰 탐지 결과를 조회합니다.</p>
        </div>
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
          <button onClick={onSearch} disabled={aggregatesQuery.isFetching} className="px-4 py-2 bg-blue-600 text-white rounded-lg shadow hover:bg-blue-700 disabled:opacity-50">조회</button>
        </div>
      </div>

      {/* 결과 테이블 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        {aggregatesQuery.isFetching ? (
          <div className="text-center py-12 text-gray-500">로딩 중…</div>
        ) : aggregates.length === 0 ? (
          <div className="text-center py-12 text-gray-500">결과가 없습니다.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">앵커시각</th>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">그룹키</th>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">룰</th>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">연산자</th>
                  <th className="px-3 py-2 text-right text-xs font-medium text-gray-500">매칭</th>
                  <th className="px-3 py-2 text-right text-xs font-medium text-gray-500">임계</th>
                  <th className="px-3 py-2 text-right text-xs font-medium text-gray-500">창(분)</th>
                  <th className="px-3 py-2 text-right text-xs font-medium text-gray-500">상세</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {aggregates.map((row) => (
                  <tr key={`${row.ruleId}-${row.detectedAt}-${row.groupKey}`} className="hover:bg-gray-50">
                    <td className="px-3 py-2 text-sm text-gray-900">{new Date(row.detectedAt).toLocaleString()}</td>
                    <td className="px-3 py-2 text-sm font-mono text-gray-800">{row.groupKey}</td>
                    <td className="px-3 py-2 text-sm text-gray-800">
                      <a href={`/aggregates/${row.ruleId}`} target="_blank" rel="noopener noreferrer" className="text-blue-600 hover:text-blue-700 hover:underline">
                        {row.ruleName ? (
                          <>
                            <span className="font-medium">{row.ruleName}</span>
                            <span className="ml-2 text-xs font-mono text-gray-500">({row.ruleId})</span>
                          </>
                        ) : (
                          <span className="font-mono">{row.ruleId}</span>
                        )}
                      </a>
                    </td>
                    <td className="px-3 py-2 text-sm text-gray-700">{labelAggregateOperator(row.operator as AggregateOperator)}</td>
                    <td className="px-3 py-2 text-sm text-right text-gray-900">{row.matchedCount ?? "-"}</td>
                    <td className="px-3 py-2 text-sm text-right text-gray-900">{row.thresholdCount ?? "-"}</td>
                    <td className="px-3 py-2 text-sm text-right text-gray-900">{row.windowMinutes ?? "-"}</td>
                    <td className="px-3 py-2 text-sm text-right">
                      <button
                        onClick={() => setDetailParams({ groupKey: row.groupKey, ruleId: row.ruleId, anchor: row.detectedAt })}
                        className="text-blue-600 hover:underline"
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
            {/* 헤더 */}
            <div className="flex items-center justify-between px-6 py-4 border-b sticky top-0 bg-gradient-to-r from-emerald-600 to-teal-600 z-10 shadow-md">
              <div>
                <div className="text-xs text-emerald-100 font-medium uppercase tracking-wide mb-1">룰 탐지 상세</div>
                <div className="text-lg font-bold text-white flex items-center gap-2">
                  <span className="font-mono">{detailParams.ruleId}</span>
                  <span className="text-emerald-100">·</span>
                  <a
                    href={`/detections/entity-history?groupKey=${encodeURIComponent(detailParams.groupKey)}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-sm text-emerald-100 hover:text-white hover:underline font-mono"
                    title="엔티티 행적 조회 (새 탭)"
                  >
                    {detailParams.groupKey}
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

            {/* 본문 */}
            <div className="p-6 bg-gradient-to-br from-gray-50/50 to-emerald-50/30">
              {detailQuery.isLoading ? (
                <div className="py-20 text-center text-gray-500">
                  <div className="inline-block w-12 h-12 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin mb-3"></div>
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
                  {/* 기본 정보 카드 */}
                  <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm mb-6">
                    <div className="flex items-center gap-2 mb-4">
                      <div className="w-8 h-8 bg-gradient-to-br from-emerald-500 to-teal-500 rounded-lg flex items-center justify-center">
                        <span className="text-white text-lg">📊</span>
                      </div>
                      <h3 className="text-base font-bold text-gray-900">탐지 정보</h3>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div className="bg-gradient-to-br from-emerald-50 to-teal-50 rounded-xl p-4 border border-emerald-100">
                        <div className="text-xs font-semibold text-emerald-700 mb-1">앵커 시각</div>
                        <div className="text-sm font-bold text-gray-900">{new Date(detailQuery.data.detectedAt).toLocaleString('ko-KR')}</div>
                      </div>
                      <div className="bg-gradient-to-br from-blue-50 to-cyan-50 rounded-xl p-4 border border-blue-100">
                        <div className="text-xs font-semibold text-blue-700 mb-1">집계 윈도우</div>
                        <div className="text-sm font-bold text-gray-900">
                          {new Date(detailQuery.data.windowStart).toLocaleString('ko-KR')}
                          <span className="text-gray-500 mx-2">~</span>
                          {new Date(detailQuery.data.windowEnd).toLocaleString('ko-KR')}
                        </div>
                      </div>
                      <div className="md:col-span-2 bg-gradient-to-br from-purple-50 to-pink-50 rounded-xl p-4 border border-purple-100">
                        <div className="text-xs font-semibold text-purple-700 mb-1">대상 센서 룰</div>
                        <div className="text-sm font-bold text-gray-900">
                          {detailQuery.data.predicateRuleId ? (
                            detailQuery.data.predicateRuleName
                              ? `${detailQuery.data.predicateRuleName} (${detailQuery.data.predicateRuleId})`
                              : detailQuery.data.predicateRuleId
                          ) : (
                            <span className="text-gray-500">(없음 - 모든 이벤트 대상)</span>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* 매칭 이벤트 카드 */}
                  <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm">
                    <div className="flex items-center justify-between mb-4">
                      <div className="flex items-center gap-2">
                        <div className="w-8 h-8 bg-gradient-to-br from-orange-500 to-amber-500 rounded-lg flex items-center justify-center">
                          <span className="text-white text-lg">📋</span>
                        </div>
                        <h3 className="text-base font-bold text-gray-900">매칭 이벤트</h3>
                      </div>
                      <div className="px-3 py-1.5 bg-emerald-100 text-emerald-700 rounded-lg text-sm font-semibold">
                        총 {detailQuery.data.matchedCount}건
                      </div>
                    </div>
                    {detailQuery.data.matchedEvents.length === 0 ? (
                      <div className="text-center py-10 text-gray-500 bg-gray-50 rounded-lg border border-gray-200">
                        📭 매칭된 이벤트가 없습니다.
                      </div>
                    ) : (
                      <div className="space-y-4">
                        {detailQuery.data.matchedEvents.map((ev, i) => (
                          <div key={`${ev.groupKey}-${ev.eventDt}-${i}`} className="border border-gray-200 rounded-xl overflow-hidden shadow-sm hover:shadow-md transition-all bg-white">
                            <div className="bg-gradient-to-r from-slate-50 to-gray-50 px-4 py-3 border-b border-gray-200">
                              <div className="flex items-center justify-between">
                                <div className="flex items-center gap-3">
                                  <span className="px-2 py-1 bg-emerald-100 text-emerald-700 rounded text-xs font-semibold">#{i + 1}</span>
                                  <span className="text-sm font-semibold text-gray-900">
                                    {new Date(ev.eventDt).toLocaleString('ko-KR')}
                                  </span>
                                </div>
                                <span className="text-xs text-gray-500 font-mono">{ev.groupKey}</span>
                              </div>
                            </div>
                            <div className="p-4 bg-gradient-to-br from-gray-50/30 to-slate-50/30">
                              <pre className="text-xs bg-white p-3 rounded-lg overflow-x-auto border border-gray-200 font-mono leading-relaxed">
                                {JSON.stringify(ev.eventData, null, 2)}
                              </pre>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

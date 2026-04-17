"use client";

import { useState, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchDetectedAggregates, fetchDetectedScenarios, type DetectedAggregate, type DetectedScenario, labelAggregateOperator, type AggregateOperator } from "./api";

function toLocalInput(dt: Date) {
  const pad = (n: number) => n.toString().padStart(2, "0");
  const yyyy = dt.getFullYear();
  const mm = pad(dt.getMonth() + 1);
  const dd = pad(dt.getDate());
  const hh = pad(dt.getHours());
  const mi = pad(dt.getMinutes());
  return `${yyyy}-${mm}-${dd}T${hh}:${mi}`;
}

export default function DetectionsPage() {
  const now = useMemo(() => new Date(), []);
  const weekAgo = useMemo(() => new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000), [now]);

  const [groupKey, setGroupKey] = useState("");
  const [startLocal, setStartLocal] = useState(toLocalInput(weekAgo));
  const [endLocal, setEndLocal] = useState(toLocalInput(now));
  const [submitCount, setSubmitCount] = useState(0); // trigger refetch

  const localToParam = (s: string) => (s && s.length === 16 ? `${s}:00` : s);

  const aggregatesQuery = useQuery({
    queryKey: ["detections", "aggregates", groupKey || "_all_", startLocal, endLocal, submitCount],
    queryFn: () => fetchDetectedAggregates({ groupKey: groupKey || undefined, startDate: localToParam(startLocal), endDate: localToParam(endLocal) }),
    enabled: submitCount > 0,
  });

  const scenariosQuery = useQuery({
    queryKey: ["detections", "scenarios", groupKey || "_all_", startLocal, endLocal, submitCount],
    queryFn: () => fetchDetectedScenarios({ groupKey: groupKey || undefined, startDate: localToParam(startLocal), endDate: localToParam(endLocal) }),
    enabled: submitCount > 0,
  });

  const onSearch = () => {
    // groupKey 없이도 조회 가능 (기간 기준 전체)
    setSubmitCount((c) => c + 1);
  };

  const aggregates = (aggregatesQuery.data || []) as DetectedAggregate[];
  const scenarios = (scenariosQuery.data || []) as DetectedScenario[];

  return (
    <div className="max-w-7xl mx-auto py-8">
      <div className="bg-white rounded-3xl shadow-xl overflow-hidden border border-gray-100">
        {/* Header */}
        <div className="bg-gradient-to-r from-sky-600 to-indigo-600 px-8 py-6">
          <h1 className="text-3xl font-bold text-white">탐지 결과</h1>
          <p className="mt-2 text-sky-100">집계 결과와 시나리오 결과를 기간·그룹키로 조회합니다.</p>
        </div>

        {/* Filters */}
        <div className="px-8 py-6 border-b border-gray-200 bg-gray-50">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">그룹키(group_key)</label>
              <input
                type="text"
                value={groupKey}
                onChange={(e) => setGroupKey(e.target.value)}
                placeholder="예: CUS012"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-sky-500 focus:border-sky-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">시작</label>
              <input
                type="datetime-local"
                value={startLocal}
                onChange={(e) => setStartLocal(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-sky-500 focus:border-sky-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">종료</label>
              <input
                type="datetime-local"
                value={endLocal}
                onChange={(e) => setEndLocal(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-sky-500 focus:border-sky-500"
              />
            </div>
          </div>
          <div className="mt-4 flex justify-end">
            <button
              onClick={onSearch}
              disabled={aggregatesQuery.isFetching || scenariosQuery.isFetching}
              className="px-4 py-2 bg-sky-600 text-white rounded-lg shadow hover:bg-sky-700 disabled:opacity-50"
            >
              조회
            </button>
          </div>
        </div>

        {/* Aggregates */}
        <div className="px-8 py-6">
          <h2 className="text-xl font-bold text-gray-800 mb-4">집계 결과</h2>

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
                    <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">집계</th>
                    <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">연산자</th>
                    <th className="px-3 py-2 text-right text-xs font-medium text-gray-500">매칭</th>
                    <th className="px-3 py-2 text-right text-xs font-medium text-gray-500">임계</th>
                    <th className="px-3 py-2 text-right text-xs font-medium text-gray-500">창(분)</th>
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
                      <td className="px-3 py-2 text-sm text-gray-700">
                        {labelAggregateOperator(row.operator as AggregateOperator)}
                      </td>
                      <td className="px-3 py-2 text-sm text-right text-gray-900">{row.matchedCount ?? "-"}</td>
                      <td className="px-3 py-2 text-sm text-right text-gray-900">{row.thresholdCount ?? "-"}</td>
                      <td className="px-3 py-2 text-sm text-right text-gray-900">{row.windowMinutes ?? "-"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Scenarios */}
        <div className="px-8 py-6 border-t border-gray-100">
          <h2 className="text-xl font-bold text-gray-800 mb-4">시나리오 결과</h2>

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
                    <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">탐지시각</th>
                    <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">평가구간</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {scenarios.map((row) => (
                    <tr key={`${row.scenarioId}-${row.detectedAt}-${row.groupKey}`} className="hover:bg-gray-50">
                      <td className="px-3 py-2 text-sm text-gray-800">
                        <a href={`/scenarios/${row.scenarioId}/edit`} target="_blank" rel="noopener noreferrer" className="text-blue-600 hover:text-blue-700 hover:underline">
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
                      <td className="px-3 py-2 text-sm text-gray-900">{new Date(row.detectedAt).toLocaleString()}</td>
                      <td className="px-3 py-2 text-sm text-gray-700">
                        {row.windowStart && row.windowEnd
                          ? `${new Date(row.windowStart).toLocaleString()} ~ ${new Date(row.windowEnd).toLocaleString()}`
                          : "-"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

"use client";

import { useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { ExecutionGraph } from "../ExecutionGraph";
import { fetchExecutionDetail, type ExecutionDetail } from "../../api";

function formatDate(value?: string | null) {
  if (!value) return "-";
  try {
    return new Date(value).toLocaleString();
  } catch {
    return value;
  }
}

export default function ExecutionDetailPage() {
  const params = useParams<{ landingRecordId: string }>();
  const router = useRouter();
  const [showGraphModal, setShowGraphModal] = useState(false);
  const landingId = useMemo(() => {
    const raw = params?.landingRecordId;
    const parsed = Number(raw);
    return Number.isFinite(parsed) ? parsed : null;
  }, [params]);

  const detailQuery = useQuery({
    queryKey: ["execution-detail", landingId],
    queryFn: () => {
      if (landingId == null) throw new Error("invalid id");
      return fetchExecutionDetail(landingId);
    },
    enabled: landingId != null,
  });

  const detail: ExecutionDetail | undefined = detailQuery.data;

  return (
    <div className="max-w-6xl mx-auto py-8 space-y-6">
      <button
        onClick={() => router.back()}
        className="text-sm text-sky-600 hover:underline"
      >
        ← 목록으로 돌아가기
      </button>

      {detailQuery.isLoading ? (
        <div className="py-12 text-center text-gray-500">실행 이력을 불러오는 중…</div>
      ) : detailQuery.isError || !detail ? (
        <div className="py-12 text-center text-red-500">상세 정보를 불러오지 못했습니다.</div>
      ) : (
        <div className="space-y-8">
          <div className="bg-white rounded-3xl shadow-xl border border-gray-100 p-8 space-y-4">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">이벤트 {detail.summary.landingRecordId}</h1>
              <p className="text-gray-600 mt-1">
                Exec {detail.summary.execDsMpId} · 데이터소스: {detail.summary.dataSourceId}
              </p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm text-gray-700">
              <div>
                <div className="text-gray-500">추출 시각</div>
                <div className="mt-1 text-gray-900">{formatDate(detail.summary.extractedAt)}</div>
              </div>
              <div>
                <div className="text-gray-500">Row Index</div>
                <div className="mt-1 text-gray-900">{detail.summary.rowIndex ?? "-"}</div>
              </div>
              <div>
                <div className="text-gray-500">Mapped Storage</div>
                <div className="mt-1 text-gray-900">{detail.summary.mappedStorageId ?? "-"}</div>
              </div>
              <div>
                <div className="text-gray-500">변환 상태</div>
                <div className={`mt-1 inline-block px-3 py-1 rounded-full text-sm font-semibold ${
                  detail.summary.ingestionStatus === "TRANSFORMED"
                    ? "bg-emerald-100 text-emerald-700"
                    : detail.summary.ingestionStatus === "FAILED"
                    ? "bg-rose-100 text-rose-700"
                    : "bg-sky-100 text-sky-700"
                }`}>
                  {detail.summary.ingestionStatus ?? "-"}
                </div>
              </div>
              <div>
                <div className="text-gray-500">실행 상태</div>
                <div className={`mt-1 inline-block px-3 py-1 rounded-full text-sm font-semibold ${
                  detail.summary.status === "SUCCESS"
                    ? "bg-emerald-100 text-emerald-700"
                    : detail.summary.status === "FAILED"
                    ? "bg-rose-100 text-rose-700"
                    : "bg-sky-100 text-sky-700"
                }`}>
                  {detail.summary.status}
                </div>
              </div>
              <div>
                <div className="text-gray-500">실행 모드</div>
                <div className="mt-1 text-gray-900">{detail.summary.executionMode ?? "-"}</div>
              </div>
              <div>
                <div className="text-gray-500">시작 시각</div>
                <div className="mt-1 text-gray-900">{formatDate(detail.summary.startDt)}</div>
              </div>
              <div>
                <div className="text-gray-500">완료 시각</div>
                <div className="mt-1 text-gray-900">{formatDate(detail.summary.completeAt)}</div>
              </div>
              <div>
                <div className="text-gray-500">실행자</div>
                <div className="mt-1 text-gray-900">{detail.summary.executedBy ?? "-"}</div>
              </div>
              <div>
                <div className="text-gray-500">총 행 수</div>
                <div className="mt-1 text-gray-900">{detail.summary.totalRows ?? "-"}</div>
              </div>
              <div className="md:col-span-3">
                <div className="text-gray-500">Ingestion 메시지</div>
                <div className="mt-1 text-gray-900 break-words">{detail.summary.ingestionMessage ?? "-"}</div>
              </div>
            </div>
            <div className="grid grid-cols-3 gap-4 text-center">
              <div className="bg-gray-50 rounded-2xl py-4">
                <div className="text-xs text-gray-500">룰 탐지</div>
                <div className="text-2xl font-bold text-gray-900">{detail.summary.ruleCount}</div>
              </div>
              <div className="bg-gray-50 rounded-2xl py-4">
                <div className="text-xs text-gray-500">집계 탐지</div>
                <div className="text-2xl font-bold text-gray-900">{detail.summary.aggregateCount}</div>
              </div>
              <div className="bg-gray-50 rounded-2xl py-4">
                <div className="text-xs text-gray-500">시나리오 탐지</div>
                <div className="text-2xl font-bold text-gray-900">{detail.summary.scenarioCount}</div>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-3xl shadow-xl border border-gray-100 p-8 space-y-6">
            <div className="flex items-start justify-between">
              <div>
                <h2 className="text-2xl font-bold text-gray-900">탐지 그래프</h2>
                <p className="text-gray-600 mt-1">이벤트 → 그룹키 → 룰·집계·시나리오 관계를 시각적으로 확인합니다.</p>
              </div>
              <button
                onClick={() => setShowGraphModal(true)}
                className="px-4 py-2 text-sm bg-sky-100 text-sky-700 rounded-lg hover:bg-sky-200"
              >
                전체 화면
              </button>
            </div>
            <ExecutionGraph detail={detail} />
          </div>

          {showGraphModal && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm">
              <div className="bg-white rounded-3xl shadow-2xl border border-gray-200 w-[92vw] h-[86vh] max-w-[1600px] max-h-[900px] p-6 flex flex-col">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-semibold text-gray-900">탐지 그래프 전체 보기</h3>
                  <button
                    onClick={() => setShowGraphModal(false)}
                    className="text-sm text-gray-500 hover:text-gray-700"
                  >
                    닫기
                  </button>
                </div>
                <div className="flex-1 overflow-hidden rounded-2xl border border-gray-100">
                  <ExecutionGraph detail={detail} fullscreen />
                </div>
              </div>
            </div>
          )}

          <div className="bg-white rounded-3xl shadow-xl border border-gray-100 p-8 space-y-6">
            <h2 className="text-xl font-semibold text-gray-900">룰 탐지 상세</h2>
            {detail.rules.length === 0 ? (
              <div className="text-gray-500 text-sm">탐지된 룰이 없습니다.</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-100 text-sm">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-3 py-2 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">룰</th>
                      <th className="px-3 py-2 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">그룹키</th>
                      <th className="px-3 py-2 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">탐지 시각</th>
                      <th className="px-3 py-2 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">MappedStorage</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {detail.rules.map((rule) => (
                      <tr key={rule.detectRuleId} className="hover:bg-gray-50">
                        <td className="px-3 py-2 text-gray-900">
                          <div className="font-medium">{rule.ruleName ?? rule.ruleId ?? "-"}</div>
                          {rule.ruleId && (
                            <div className="text-xs text-gray-500">{rule.ruleId}</div>
                          )}
                        </td>
                        <td className="px-3 py-2 text-gray-700">{rule.groupKey ?? "-"}</td>
                        <td className="px-3 py-2 text-gray-700">{formatDate(rule.detectedAt ?? undefined)}</td>
                        <td className="px-3 py-2 text-gray-700">{rule.mappedStorageId ?? "-"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          <div className="bg-white rounded-3xl shadow-xl border border-gray-100 p-8 space-y-6">
            <h2 className="text-xl font-semibold text-gray-900">집계 탐지 상세</h2>
            {detail.aggregates.length === 0 ? (
              <div className="text-gray-500 text-sm">탐지된 집계가 없습니다.</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-100 text-sm">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-3 py-2 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">집계</th>
                      <th className="px-3 py-2 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">그룹키</th>
                      <th className="px-3 py-2 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">앵커 시각</th>
                      <th className="px-3 py-2 text-right text-xs font-semibold text-gray-500 uppercase tracking-wide">매치 수</th>
                      <th className="px-3 py-2 text-right text-xs font-semibold text-gray-500 uppercase tracking-wide">임계값</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {detail.aggregates.map((agg, idx) => (
                      <tr key={`${agg.ruleId}-${idx}`} className="hover:bg-gray-50">
                        <td className="px-3 py-2 text-gray-900">
                          <div className="font-medium">{agg.ruleName ?? agg.ruleId}</div>
                          <div className="text-xs text-gray-500">{agg.ruleId}</div>
                        </td>
                        <td className="px-3 py-2 text-gray-700">{agg.groupKey}</td>
                        <td className="px-3 py-2 text-gray-700">{formatDate(agg.detectedAt)}</td>
                        <td className="px-3 py-2 text-right text-gray-900">{agg.matchedCount ?? "-"}</td>
                        <td className="px-3 py-2 text-right text-gray-900">{agg.thresholdCount ?? "-"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          <div className="bg-white rounded-3xl shadow-xl border border-gray-100 p-8 space-y-6">
            <h2 className="text-xl font-semibold text-gray-900">시나리오 탐지 상세</h2>
            {detail.scenarios.length === 0 ? (
              <div className="text-gray-500 text-sm">탐지된 시나리오가 없습니다.</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-100 text-sm">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-3 py-2 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">시나리오</th>
                      <th className="px-3 py-2 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">그룹키</th>
                      <th className="px-3 py-2 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">탐지 시각</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {detail.scenarios.map((sc, idx) => (
                      <tr key={`${sc.scenarioId}-${idx}`} className="hover:bg-gray-50">
                        <td className="px-3 py-2 text-gray-900">
                          <div className="font-medium">{sc.scenarioName ?? sc.scenarioId}</div>
                          <div className="text-xs text-gray-500">{sc.scenarioId}</div>
                        </td>
                        <td className="px-3 py-2 text-gray-700">{sc.groupKey ?? "-"}</td>
                        <td className="px-3 py-2 text-gray-700">{formatDate(sc.detectedAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {(detail.rawPayload || detail.mappedRow) && (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {detail.rawPayload && (
                <div className="bg-white rounded-3xl shadow-xl border border-gray-100 p-6">
                  <h2 className="text-lg font-semibold text-gray-900 mb-3">원본 Payload</h2>
                  <pre className="bg-gray-900 text-gray-100 text-xs rounded-2xl p-4 overflow-auto max-h-80 whitespace-pre-wrap">
                    {JSON.stringify(detail.rawPayload, null, 2)}
                  </pre>
                </div>
              )}
              {detail.mappedRow && (
                <div className="bg-white rounded-3xl shadow-xl border border-gray-100 p-6">
                  <h2 className="text-lg font-semibold text-gray-900 mb-3">매핑 결과</h2>
                  <pre className="bg-gray-900 text-gray-100 text-xs rounded-2xl p-4 overflow-auto max-h-80 whitespace-pre-wrap">
                    {JSON.stringify(detail.mappedRow, null, 2)}
                  </pre>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

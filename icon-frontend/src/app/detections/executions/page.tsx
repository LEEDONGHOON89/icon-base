"use client";

import Link from "next/link";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchExecutions, type ExecutionPage } from "../api";

function formatDate(value?: string | null) {
  if (!value) return "-";
  try {
    return new Date(value).toLocaleString();
  } catch {
    return value;
  }
}

const PAGE_SIZE = 20;

export default function ExecutionListPage() {
  const [page, setPage] = useState(1);

  const executionsQuery = useQuery({
    queryKey: ["execution-list", page, PAGE_SIZE],
    queryFn: () => fetchExecutions({ page, size: PAGE_SIZE }),
  });

  const data: ExecutionPage | undefined = executionsQuery.data;
  const totalPages = data?.totalPages ?? 0;

  const onPrev = () => {
    setPage((prev) => Math.max(1, prev - 1));
  };

  const onNext = () => {
    if (!data) return;
    setPage((prev) => Math.min(data.totalPages, prev + 1));
  };

  return (
    <div className="p-6 space-y-6">
      {/* 헤더 */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">탐지 이벤트 이력</h1>
        <p className="text-sm text-gray-500 mt-1">
          landing_raw_records 행 단위로 변환 상태와 탐지 결과(룰·집계·시나리오)를 확인합니다.
        </p>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200">
        <div className="px-8 py-4 flex items-center justify-between border-b border-gray-100 bg-gray-50">
          <div>
            <p className="text-sm text-gray-600">
              총 <span className="font-semibold text-gray-900">{data?.totalElements ?? 0}</span>건 · 페이지 {data?.page ?? page} / {totalPages}
            </p>
          </div>
          <div className="space-x-2">
            <button
              onClick={onPrev}
              disabled={executionsQuery.isFetching || page <= 1}
              className="px-3 py-2 text-sm rounded-lg border border-gray-300 bg-white text-gray-700 disabled:opacity-40"
            >
              이전
            </button>
            <button
              onClick={onNext}
              disabled={executionsQuery.isFetching || (data && page >= data.totalPages)}
              className="px-3 py-2 text-sm rounded-lg border border-gray-300 bg-white text-gray-700 disabled:opacity-40"
            >
              다음
            </button>
          </div>
        </div>

        {executionsQuery.isLoading ? (
          <div className="py-12 text-center text-gray-500">로딩 중…</div>
        ) : executionsQuery.isError ? (
          <div className="py-12 text-center text-red-500">실행 이력을 불러오지 못했습니다.</div>
        ) : (data?.items?.length ?? 0) === 0 ? (
          <div className="py-12 text-center text-gray-500">실행 이력이 없습니다.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-100">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">이벤트 ID</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">Exec ID</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">데이터소스</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">추출 시각</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">변환 상태</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">실행 상태</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-gray-500 uppercase tracking-wide">룰</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-gray-500 uppercase tracking-wide">집계</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-gray-500 uppercase tracking-wide">시나리오</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-gray-500 uppercase tracking-wide">상세</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {data?.items.map((item) => (
                  <tr key={item.landingRecordId} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm font-mono text-gray-900">{item.landingRecordId}</td>
                    <td className="px-4 py-3 text-sm font-mono text-gray-900">{item.execDsMpId}</td>
                    <td className="px-4 py-3 text-sm text-gray-800">{item.dataSourceId}</td>
                    <td className="px-4 py-3 text-sm text-gray-700">{formatDate(item.extractedAt)}</td>
                    <td className="px-4 py-3 text-sm">
                      <span className={`px-2 py-1 rounded-lg text-xs font-semibold ${
                        item.ingestionStatus === "TRANSFORMED"
                          ? "bg-emerald-100 text-emerald-700"
                          : item.ingestionStatus === "FAILED"
                          ? "bg-rose-100 text-rose-700"
                          : "bg-sky-100 text-sky-700"
                      }`}>
                        {item.ingestionStatus ?? "-"}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <span className={`px-2 py-1 rounded-lg text-xs font-semibold ${
                        item.status === "SUCCESS"
                          ? "bg-emerald-100 text-emerald-700"
                          : item.status === "FAILED"
                          ? "bg-rose-100 text-rose-700"
                          : "bg-sky-100 text-sky-700"
                      }`}>
                        {item.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-right text-gray-900">{item.ruleCount}</td>
                    <td className="px-4 py-3 text-sm text-right text-gray-900">{item.aggregateCount}</td>
                    <td className="px-4 py-3 text-sm text-right text-gray-900">{item.scenarioCount}</td>
                    <td className="px-4 py-3 text-sm text-right">
                      <Link
                        href={`/detections/executions/${item.landingRecordId}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-sky-600 hover:underline"
                      >
                        보기
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

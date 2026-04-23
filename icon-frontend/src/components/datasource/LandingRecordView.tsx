"use client";

// [2026-04-22] 수집 원본 조회 컴포넌트 — TODO-001
// landing_records 테이블: 기간/상태/JSONB 확장 필터 + 페이지네이션 + 상세 모달
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  MagnifyingGlassIcon,
  ClipboardDocumentIcon,
  XMarkIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  InboxIcon,
} from "@heroicons/react/24/outline";
import { toast } from "react-hot-toast";
import {
  fetchLandingRecords,
  LandingRecord,
  LandingRecordSearchParams,
} from "@/app/data-sources/api";
import JsonFilterBuilder, {
  JsonFilter,
  toJsonFilterStrings,
} from "./JsonFilterBuilder";

// ── 유틸 ──────────────────────────────────────────────────────────────────────

function formatDatetimeLocal(d: Date) {
  return d.toISOString().slice(0, 16);
}

function toIsoSec(dtLocal: string) {
  return dtLocal ? dtLocal + ":00" : undefined;
}

function formatDisplay(iso?: string | null) {
  if (!iso) return "-";
  return iso.replace("T", " ").slice(0, 19);
}

function jsonPreview(obj: Record<string, unknown>, max = 4) {
  const entries = Object.entries(obj || {}).slice(0, max);
  return entries.map(([k, v]) => `${k}: ${String(v)}`).join("  ·  ") || "-";
}

// ── 상태 배지 ─────────────────────────────────────────────────────────────────

function IngestionBadge({ status }: { status: string }) {
  const styles: Record<string, string> = {
    NEW: "bg-gray-100 text-gray-700",
    TRANSFORMED: "bg-green-100 text-green-700",
    FAILED: "bg-red-100 text-red-700",
  };
  const labels: Record<string, string> = {
    NEW: "신규",
    TRANSFORMED: "변환완료",
    FAILED: "실패",
  };
  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${styles[status] ?? "bg-gray-100 text-gray-700"}`}
    >
      {labels[status] ?? status}
    </span>
  );
}

// ── 상세 모달 ─────────────────────────────────────────────────────────────────

function DetailModal({
  record,
  onClose,
}: {
  record: LandingRecord;
  onClose: () => void;
}) {
  const formatted = JSON.stringify(record.rawPayload, null, 2);

  const copyToClipboard = async () => {
    await navigator.clipboard.writeText(formatted);
    toast.success("클립보드에 복사되었습니다.");
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl shadow-2xl w-full max-w-3xl mx-4 max-h-[85vh] flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 헤더 */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <div>
            <h3 className="text-lg font-semibold text-gray-900">
              수집 원본 상세
            </h3>
            <p className="text-xs text-gray-500 mt-0.5">
              ID: {record.landingRecordId} · {formatDisplay(record.extractedAt)}
            </p>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <XMarkIcon className="h-5 w-5" />
          </button>
        </div>

        {/* 메타 정보 */}
        <div className="px-6 py-3 bg-gray-50 border-b border-gray-100 grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
          <div>
            <dt className="text-gray-400">수집 상태</dt>
            <dd className="mt-0.5">
              <IngestionBadge status={record.ingestionStatus} />
            </dd>
          </div>
          <div>
            <dt className="text-gray-400">소스 유형</dt>
            <dd className="mt-0.5 font-medium text-gray-700">
              {record.sourceType}
            </dd>
          </div>
          <div>
            <dt className="text-gray-400">Row Index</dt>
            <dd className="mt-0.5 font-medium text-gray-700">
              {record.rowIndex ?? "-"}
            </dd>
          </div>
          <div>
            <dt className="text-gray-400">실행 이력 ID</dt>
            <dd className="mt-0.5 font-medium text-gray-700">
              {record.execDsMpId ?? "-"}
            </dd>
          </div>
        </div>

        {/* 오류 메시지 (FAILED일 때) */}
        {record.ingestionStatus === "FAILED" && record.ingestionMessage && (
          <div className="px-6 py-2 bg-red-50 border-b border-red-100">
            <p className="text-xs text-red-700">
              <span className="font-semibold">오류:</span>{" "}
              {record.ingestionMessage}
            </p>
          </div>
        )}

        {/* raw_payload JSON */}
        <div className="flex items-center justify-between px-6 py-2 border-b border-gray-100">
          <span className="text-sm font-medium text-gray-700">
            raw_payload
          </span>
          <button
            onClick={copyToClipboard}
            className="inline-flex items-center gap-1 text-xs text-gray-500 hover:text-blue-600"
          >
            <ClipboardDocumentIcon className="h-4 w-4" />
            복사
          </button>
        </div>
        <div className="overflow-auto flex-1 px-6 py-4">
          <pre className="text-xs text-gray-800 font-mono whitespace-pre-wrap break-all bg-gray-50 rounded-lg p-4 border border-gray-100">
            {formatted}
          </pre>
        </div>
      </div>
    </div>
  );
}

// ── 메인 컴포넌트 ─────────────────────────────────────────────────────────────

interface LandingRecordViewProps {
  dataSourceId: string;
}

export default function LandingRecordView({
  dataSourceId,
}: LandingRecordViewProps) {
  const now = new Date();
  const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);

  // 필터 상태 (pending — "조회" 버튼 클릭 전)
  const [startDate, setStartDate] = useState(formatDatetimeLocal(yesterday));
  const [endDate, setEndDate] = useState(formatDatetimeLocal(now));
  const [statuses, setStatuses] = useState<string[]>([]);
  const [custNo, setCustNo] = useState("");
  const [jsonFilters, setJsonFilters] = useState<JsonFilter[]>([]);

  // 적용된 필터 (query key)
  const [appliedParams, setAppliedParams] = useState<LandingRecordSearchParams>({
    startDate: toIsoSec(formatDatetimeLocal(yesterday)),
    endDate: toIsoSec(formatDatetimeLocal(now)),
    page: 0,
    size: 20,
  });
  const [page, setPage] = useState(0);
  // [2026-04-23] 조회 버튼 클릭 시 조건 변경 없어도 강제 재조회를 위한 카운터
  // queryKey에 포함시켜 동일 조건 재조회 시 캐시 미사용 → 항상 API 호출
  const [searchTick, setSearchTick] = useState(0);

  // 상세 모달
  const [selectedRecord, setSelectedRecord] = useState<LandingRecord | null>(
    null
  );

  const { data, isLoading, isFetching } = useQuery({
    // [2026-04-23] searchTick을 queryKey에 포함 → 조건 미변경 재조회 시에도 항상 API 호출
    queryKey: ["landing-records", dataSourceId, appliedParams, page, searchTick],
    queryFn: () =>
      fetchLandingRecords(dataSourceId, { ...appliedParams, page }),
    placeholderData: (prev) => prev,
  });

  const handleSearch = () => {
    const params: LandingRecordSearchParams = {
      startDate: toIsoSec(startDate),
      endDate: toIsoSec(endDate),
      ingestionStatus: statuses.length > 0 ? statuses : undefined,
      custNo: custNo.trim() || undefined,
      jsonFilters: toJsonFilterStrings(jsonFilters),
      size: 20,
    };
    setAppliedParams(params);
    setPage(0);
    // [2026-04-23] 조건 변경 없이 재조회 시에도 API 호출되도록 tick 증가
    setSearchTick((t) => t + 1);
  };

  const toggleStatus = (s: string) => {
    setStatuses((prev) =>
      prev.includes(s) ? prev.filter((x) => x !== s) : [...prev, s]
    );
  };

  const totalPages = data ? Math.ceil(data.total / (appliedParams.size ?? 20)) : 0;
  const summary = data?.summary;

  return (
    <div className="space-y-5">
      {/* ── 필터 영역 ─────────────────────────────────────────────────── */}
      <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
        <h4 className="text-sm font-semibold text-gray-700">조회 조건</h4>

        {/* 기본 필터 */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">
              조회 기간 (From)
            </label>
            <input
              type="datetime-local"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="w-full text-sm border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-400 focus:outline-none"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">
              조회 기간 (To)
            </label>
            <input
              type="datetime-local"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="w-full text-sm border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-400 focus:outline-none"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">
              cust_no
            </label>
            <input
              type="text"
              value={custNo}
              onChange={(e) => setCustNo(e.target.value)}
              placeholder="고객 번호 입력"
              className="w-full text-sm border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-400 focus:outline-none"
            />
          </div>
        </div>

        {/* 상태 필터 */}
        <div>
          <label className="block text-xs font-medium text-gray-600 mb-2">
            수집 상태
          </label>
          <div className="flex flex-wrap gap-2">
            {["NEW", "TRANSFORMED", "FAILED"].map((s) => (
              <button
                key={s}
                type="button"
                onClick={() => toggleStatus(s)}
                className={`px-3 py-1 rounded-full text-xs font-medium border transition-colors ${
                  statuses.includes(s)
                    ? "bg-blue-600 text-white border-blue-600"
                    : "bg-white text-gray-600 border-gray-300 hover:border-blue-400"
                }`}
              >
                {s}
              </button>
            ))}
            {statuses.length > 0 && (
              <button
                type="button"
                onClick={() => setStatuses([])}
                className="text-xs text-gray-400 hover:text-gray-600 underline"
              >
                초기화
              </button>
            )}
          </div>
        </div>

        {/* JSONB 확장 필터 */}
        <JsonFilterBuilder
          filters={jsonFilters}
          onChange={setJsonFilters}
          fieldHints={["cust_no", "account_id", "transaction_id"]}
        />

        {/* 조회 버튼 */}
        <div className="flex justify-end">
          <button
            type="button"
            onClick={handleSearch}
            className="inline-flex items-center gap-2 px-5 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors"
          >
            <MagnifyingGlassIcon className="h-4 w-4" />
            조회
          </button>
        </div>
      </div>

      {/* ── 통계 요약 카드 ─────────────────────────────────────────────── */}
      {summary && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          {[
            {
              label: "전체 건수",
              value: summary.total.toLocaleString(),
              color: "text-blue-600",
              bg: "bg-blue-50",
            },
            {
              label: "변환완료",
              value: summary.transformedCount.toLocaleString(),
              color: "text-green-600",
              bg: "bg-green-50",
            },
            {
              label: "실패",
              value: summary.failedCount.toLocaleString(),
              color: "text-red-600",
              bg: "bg-red-50",
            },
            {
              label: "최근 수집",
              value: formatDisplay(summary.lastExtractedAt),
              color: "text-gray-700",
              bg: "bg-gray-50",
            },
          ].map((card) => (
            <div
              key={card.label}
              className={`${card.bg} rounded-xl p-4 border border-white`}
            >
              <p className="text-xs text-gray-500">{card.label}</p>
              <p className={`mt-1 text-lg font-bold ${card.color}`}>
                {card.value}
              </p>
            </div>
          ))}
        </div>
      )}

      {/* ── 목록 테이블 ───────────────────────────────────────────────── */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {/* 로딩 인디케이터 */}
        {(isLoading || isFetching) && (
          <div className="absolute inset-x-0 top-0 h-0.5 bg-blue-400 animate-pulse" />
        )}

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead>
              <tr className="bg-gray-50">
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  ID
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  수집 시각
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  소스 유형
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  상태
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  cust_no
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  원본 데이터 미리보기
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  상세
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="px-4 py-12 text-center">
                    <div className="flex justify-center">
                      <div className="animate-spin h-6 w-6 border-2 border-blue-600 border-t-transparent rounded-full" />
                    </div>
                  </td>
                </tr>
              ) : data?.data.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-12 text-center">
                    <InboxIcon className="h-10 w-10 text-gray-300 mx-auto mb-2" />
                    <p className="text-sm text-gray-500">
                      조회 결과가 없습니다.
                    </p>
                  </td>
                </tr>
              ) : (
                data?.data.map((record) => (
                  <tr
                    key={record.landingRecordId}
                    className="hover:bg-gray-50 transition-colors"
                  >
                    <td className="px-4 py-3 text-sm text-gray-600 font-mono">
                      {record.landingRecordId}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-700 whitespace-nowrap">
                      {formatDisplay(record.extractedAt)}
                    </td>
                    <td className="px-4 py-3">
                      <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-indigo-50 text-indigo-700">
                        {record.sourceType}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <IngestionBadge status={record.ingestionStatus} />
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600 font-mono">
                      {record.rawPayload?.cust_no != null
                        ? String(record.rawPayload.cust_no)
                        : "-"}
                    </td>
                    <td className="px-4 py-3 text-xs text-gray-500 max-w-xs truncate">
                      {jsonPreview(record.rawPayload)}
                    </td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => setSelectedRecord(record)}
                        className="text-xs text-blue-600 hover:text-blue-700 font-medium"
                      >
                        상세
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* 페이지네이션 */}
        {data && data.total > 0 && (
          <div className="px-4 py-3 border-t border-gray-100 flex items-center justify-between">
            <p className="text-xs text-gray-500">
              전체 {data.total.toLocaleString()}건 · {page + 1} /{" "}
              {totalPages} 페이지
            </p>
            <div className="flex items-center gap-1">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
                className="p-1.5 rounded text-gray-500 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                <ChevronLeftIcon className="h-4 w-4" />
              </button>
              <span className="text-xs text-gray-600 px-2">
                {page + 1} / {totalPages}
              </span>
              <button
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="p-1.5 rounded text-gray-500 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                <ChevronRightIcon className="h-4 w-4" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 상세 모달 */}
      {selectedRecord && (
        <DetailModal
          record={selectedRecord}
          onClose={() => setSelectedRecord(null)}
        />
      )}
    </div>
  );
}

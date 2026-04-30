"use client";

// [2026-04-22] 매핑 결과 조회 컴포넌트 — TODO-002
// mapped_storages 테이블: 기간/상태/JSONB 확장 필터 + 페이지네이션
// 상세 모달: 매핑 결과 탭 + 원본 비교 탭 (raw_payload ↔ row_data)
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
  fetchMappedStorages,
  fetchMappedStorageWithOrigin,
  MappedStorage,
  MappedStorageSearchParams,
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
  return entries.map(([k, v]) => `${String(v) !== "[object Object]" ? `${k}: ${String(v)}` : `${k}: {...}`}`).join("  ·  ") || "-";
}

// ── 상태 배지 ─────────────────────────────────────────────────────────────────

function ProcessingBadge({ status }: { status: string }) {
  const styles: Record<string, string> = {
    NEW: "bg-gray-100 text-gray-700",
    PROCESSING: "bg-blue-100 text-blue-700",
    COMPLETED: "bg-green-100 text-green-700",
    FAILED: "bg-red-100 text-red-700",
  };
  const labels: Record<string, string> = {
    NEW: "신규",
    PROCESSING: "처리중",
    COMPLETED: "완료",
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
  record: MappedStorage;
  onClose: () => void;
}) {
  const [activeTab, setActiveTab] = useState<"result" | "compare">("result");

  // 원본 비교 탭 선택 시 lazy 조회
  const { data: detail, isLoading: loadingOrigin } = useQuery({
    queryKey: ["mapped-storage-origin", record.mappedStorageId],
    queryFn: () => fetchMappedStorageWithOrigin(record.mappedStorageId),
    enabled: activeTab === "compare",
  });

  const copyJson = async (obj: Record<string, unknown>) => {
    await navigator.clipboard.writeText(JSON.stringify(obj, null, 2));
    toast.success("클립보드에 복사되었습니다.");
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl shadow-2xl w-full max-w-4xl mx-4 max-h-[88vh] flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 헤더 */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <div>
            <h3 className="text-lg font-semibold text-gray-900">
              매핑 결과 상세
            </h3>
            <p className="text-xs text-gray-500 mt-0.5">
              ID: {record.mappedStorageId} · {formatDisplay(record.regDt)}
              {record.transactionId && (
                <span className="ml-2 font-mono">{record.transactionId}</span>
              )}
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
            <dt className="text-gray-400">처리 상태</dt>
            <dd className="mt-0.5">
              <ProcessingBadge status={record.processingStatus} />
            </dd>
          </div>
          <div>
            <dt className="text-gray-400">Landing Record ID</dt>
            <dd className="mt-0.5 font-medium text-gray-700 font-mono">
              {record.landingRecordId}
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

        {/* 오류 메시지 */}
        {record.processingStatus === "FAILED" && record.errorMessage && (
          <div className="px-6 py-2 bg-red-50 border-b border-red-100">
            <p className="text-xs text-red-700">
              <span className="font-semibold">오류:</span> {record.errorMessage}
            </p>
          </div>
        )}

        {/* 탭 */}
        <div className="flex border-b border-gray-200 px-6">
          {(["result", "compare"] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`py-3 px-4 text-sm font-medium border-b-2 -mb-px ${
                activeTab === tab
                  ? "border-blue-500 text-blue-600"
                  : "border-transparent text-gray-500 hover:text-gray-700"
              }`}
            >
              {tab === "result" ? "매핑 결과" : "원본 비교"}
            </button>
          ))}
        </div>

        {/* 탭 콘텐츠 */}
        <div className="overflow-auto flex-1 px-6 py-4">
          {activeTab === "result" && (
            <div>
              <div className="flex justify-end mb-2">
                <button
                  onClick={() => copyJson(record.rowData)}
                  className="inline-flex items-center gap-1 text-xs text-gray-500 hover:text-blue-600"
                >
                  <ClipboardDocumentIcon className="h-4 w-4" />
                  복사
                </button>
              </div>
              {/* row_data 필드 테이블 */}
              <table className="min-w-full divide-y divide-gray-100 text-sm border border-gray-100 rounded-lg overflow-hidden">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 w-1/3">
                      필드명
                    </th>
                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">
                      값
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {Object.entries(record.rowData || {}).map(([key, val]) => (
                    <tr key={key} className="hover:bg-gray-50">
                      <td className="px-4 py-2 text-xs font-medium text-gray-600 font-mono">
                        {key}
                      </td>
                      <td className="px-4 py-2 text-xs text-gray-800 break-all font-mono">
                        {typeof val === "object"
                          ? JSON.stringify(val)
                          : String(val ?? "-")}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {activeTab === "compare" && (
            <div>
              {loadingOrigin ? (
                <div className="flex justify-center py-12">
                  <div className="animate-spin h-6 w-6 border-2 border-blue-600 border-t-transparent rounded-full" />
                </div>
              ) : (
                <div className="grid grid-cols-2 gap-4">
                  {/* 왼쪽: 원본 raw_payload */}
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-xs font-semibold text-gray-600">
                        수집 원본 (raw_payload)
                      </span>
                      {detail?.rawPayload && (
                        <button
                          onClick={() => copyJson(detail.rawPayload)}
                          className="inline-flex items-center gap-1 text-xs text-gray-400 hover:text-blue-600"
                        >
                          <ClipboardDocumentIcon className="h-3.5 w-3.5" />
                          복사
                        </button>
                      )}
                    </div>
                    <pre className="text-xs text-gray-700 font-mono whitespace-pre-wrap break-all bg-gray-50 rounded-lg p-3 border border-gray-100 h-64 overflow-auto">
                      {detail?.rawPayload
                        ? JSON.stringify(detail.rawPayload, null, 2)
                        : "-"}
                    </pre>
                  </div>
                  {/* 오른쪽: 매핑 결과 row_data */}
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-xs font-semibold text-gray-600">
                        매핑 결과 (row_data)
                      </span>
                      <button
                        onClick={() => copyJson(record.rowData)}
                        className="inline-flex items-center gap-1 text-xs text-gray-400 hover:text-blue-600"
                      >
                        <ClipboardDocumentIcon className="h-3.5 w-3.5" />
                        복사
                      </button>
                    </div>
                    <pre className="text-xs text-gray-700 font-mono whitespace-pre-wrap break-all bg-blue-50 rounded-lg p-3 border border-blue-100 h-64 overflow-auto">
                      {JSON.stringify(record.rowData, null, 2)}
                    </pre>
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

// ── 메인 컴포넌트 ─────────────────────────────────────────────────────────────

interface MappedStorageViewProps {
  dataSourceId: string;
}

export default function MappedStorageView({
  dataSourceId,
}: MappedStorageViewProps) {
  const now = new Date();
  const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);

  const [startDate, setStartDate] = useState(formatDatetimeLocal(yesterday));
  const [endDate, setEndDate] = useState(formatDatetimeLocal(now));
  const [statuses, setStatuses] = useState<string[]>([]);
  const [transactionId, setTransactionId] = useState("");
  const [custNo, setCustNo] = useState("");
  const [jsonFilters, setJsonFilters] = useState<JsonFilter[]>([]);

  const [appliedParams, setAppliedParams] =
    useState<MappedStorageSearchParams>({
      startDate: toIsoSec(formatDatetimeLocal(yesterday)),
      endDate: toIsoSec(formatDatetimeLocal(now)),
      page: 0,
      size: 20,
    });
  const [page, setPage] = useState(0);
  const [selectedRecord, setSelectedRecord] = useState<MappedStorage | null>(
    null
  );

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ["mapped-storages", dataSourceId, appliedParams, page],
    queryFn: () =>
      fetchMappedStorages(dataSourceId, { ...appliedParams, page }),
    placeholderData: (prev) => prev,
  });

  const handleSearch = () => {
    const params: MappedStorageSearchParams = {
      startDate: toIsoSec(startDate),
      endDate: toIsoSec(endDate),
      processingStatus: statuses.length > 0 ? statuses : undefined,
      transactionId: transactionId.trim() || undefined,
      custNo: custNo.trim() || undefined,
      jsonFilters: toJsonFilterStrings(jsonFilters),
      size: 20,
    };
    setAppliedParams(params);
    setPage(0);
  };

  const toggleStatus = (s: string) => {
    setStatuses((prev) =>
      prev.includes(s) ? prev.filter((x) => x !== s) : [...prev, s]
    );
  };

  const totalPages = data
    ? Math.ceil(data.total / (appliedParams.size ?? 20))
    : 0;
  const summary = data?.summary;

  return (
    <div className="space-y-5">
      {/* ── 필터 영역 ─────────────────────────────────────────────────── */}
      <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
        <h4 className="text-sm font-semibold text-gray-700">조회 조건</h4>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
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
              거래 ID
            </label>
            <input
              type="text"
              value={transactionId}
              onChange={(e) => setTransactionId(e.target.value)}
              placeholder="TXN-..."
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
            처리 상태
          </label>
          <div className="flex flex-wrap gap-2">
            {["NEW", "PROCESSING", "COMPLETED", "FAILED"].map((s) => (
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
              label: "처리 완료",
              value: summary.completedCount.toLocaleString(),
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
              label: "최근 매핑",
              value: formatDisplay(summary.lastRegDt),
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
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden relative">
        {(isLoading || isFetching) && (
          <div className="absolute inset-x-0 top-0 h-0.5 bg-blue-400 animate-pulse z-10" />
        )}

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead>
              <tr className="bg-gray-50">
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  ID
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  매핑 시각
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  거래 ID
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  상태
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  cust_no
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  데이터 미리보기
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
                    key={record.mappedStorageId}
                    className="hover:bg-gray-50 transition-colors"
                  >
                    <td className="px-4 py-3 text-sm text-gray-600 font-mono">
                      {record.mappedStorageId}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-700 whitespace-nowrap">
                      {formatDisplay(record.regDt)}
                    </td>
                    <td className="px-4 py-3 text-xs text-gray-600 font-mono max-w-[140px] truncate">
                      {record.transactionId ?? "-"}
                    </td>
                    <td className="px-4 py-3">
                      <ProcessingBadge status={record.processingStatus} />
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600 font-mono">
                      {record.rowData?.cust_no != null
                        ? String(record.rowData.cust_no)
                        : "-"}
                    </td>
                    <td className="px-4 py-3 text-xs text-gray-500 max-w-xs truncate">
                      {jsonPreview(record.rowData)}
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
              전체 {data.total.toLocaleString()}건 · {page + 1} / {totalPages}{" "}
              페이지
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

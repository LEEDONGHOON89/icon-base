"use client";

import { useState, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  ClockIcon,
  FunnelIcon,
  MagnifyingGlassIcon,
  ChevronDownIcon,
  ChevronUpIcon,
  ArrowPathIcon,
  CalendarIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
} from "@heroicons/react/24/outline";
import {
  searchAuditHistory,
  fetchAuditHistoryByTarget,
  AuditSummary,
  AuditDetail,
  ConfigAuditTargetType,
  ConfigAuditAction,
  PageResponse,
  getActionLabel,
  getTargetTypeLabel,
  getActionColorClass,
  getTargetTypeColorClass,
} from "../api";

// 날짜 포맷 함수
const formatDate = (dateString: string): string => {
  const date = new Date(dateString);
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).format(date);
};

// 상대 시간 포맷 함수
const formatRelativeTime = (dateString: string): string => {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return "방금 전";
  if (diffMins < 60) return `${diffMins}분 전`;
  if (diffHours < 24) return `${diffHours}시간 전`;
  if (diffDays < 7) return `${diffDays}일 전`;
  return formatDate(dateString);
};

// JSON 포맷 함수
const formatJson = (jsonString?: string): string => {
  if (!jsonString) return "-";
  try {
    const parsed = JSON.parse(jsonString);
    return JSON.stringify(parsed, null, 2);
  } catch {
    return jsonString;
  }
};

// 날짜를 ISO-8601 형식으로 변환 (LocalDateTime 호환)
const toISODateTime = (dateStr: string, isEndOfDay: boolean = false): string => {
  if (!dateStr) return "";
  const date = new Date(dateStr);
  if (isEndOfDay) {
    date.setHours(23, 59, 59, 999);
  } else {
    date.setHours(0, 0, 0, 0);
  }
  return date.toISOString();
};

// 오늘 날짜 (YYYY-MM-DD 형식)
const getToday = (): string => {
  return new Date().toISOString().split("T")[0];
};

// n일 전 날짜 (YYYY-MM-DD 형식)
const getDaysAgo = (days: number): string => {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return date.toISOString().split("T")[0];
};

// 페이지네이션 컴포넌트
function Pagination({
  currentPage,
  totalPages,
  onPageChange,
}: {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}) {
  const pages = useMemo(() => {
    const result: (number | string)[] = [];
    const maxVisible = 5;

    if (totalPages <= maxVisible + 2) {
      // 전체 페이지가 적으면 모두 표시
      for (let i = 0; i < totalPages; i++) result.push(i);
    } else {
      // 처음 페이지
      result.push(0);

      // 중간 페이지들
      let start = Math.max(1, currentPage - 1);
      let end = Math.min(totalPages - 2, currentPage + 1);

      if (currentPage < 3) {
        end = Math.min(3, totalPages - 2);
      } else if (currentPage > totalPages - 4) {
        start = Math.max(1, totalPages - 4);
      }

      if (start > 1) result.push("...");
      for (let i = start; i <= end; i++) result.push(i);
      if (end < totalPages - 2) result.push("...");

      // 마지막 페이지
      result.push(totalPages - 1);
    }

    return result;
  }, [currentPage, totalPages]);

  if (totalPages <= 1) return null;

  return (
    <div className="flex items-center justify-center gap-1">
      <button
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 0}
        className="p-2 rounded-lg hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        <ChevronLeftIcon className="h-5 w-5" />
      </button>

      {pages.map((page, idx) => (
        typeof page === "number" ? (
          <button
            key={idx}
            onClick={() => onPageChange(page)}
            className={`min-w-[40px] h-10 rounded-lg font-medium transition-colors ${
              page === currentPage
                ? "bg-purple-600 text-white"
                : "hover:bg-gray-100 text-gray-700"
            }`}
          >
            {page + 1}
          </button>
        ) : (
          <span key={idx} className="px-2 text-gray-400">...</span>
        )
      ))}

      <button
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage >= totalPages - 1}
        className="p-2 rounded-lg hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        <ChevronRightIcon className="h-5 w-5" />
      </button>
    </div>
  );
}

// 상세 정보 모달 컴포넌트
function AuditDetailModal({
  audit,
  detail,
  isLoading,
  onClose,
}: {
  audit: AuditSummary;
  detail: AuditDetail | null;
  isLoading: boolean;
  onClose: () => void;
}) {
  const [showBefore, setShowBefore] = useState(false);
  const [showAfter, setShowAfter] = useState(false);

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex min-h-full items-center justify-center p-4">
        <div
          className="fixed inset-0 bg-gray-500/75 transition-opacity"
          onClick={onClose}
        />
        <div className="relative bg-white rounded-xl shadow-xl max-w-3xl w-full max-h-[90vh] overflow-hidden">
          {/* Header */}
          <div className="bg-gradient-to-r from-purple-600 to-indigo-600 px-6 py-4">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-semibold text-white">변경 상세 정보</h3>
              <button
                onClick={onClose}
                className="text-white/80 hover:text-white"
              >
                <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          </div>

          {/* Content */}
          <div className="p-6 overflow-y-auto max-h-[calc(90vh-120px)]">
            {isLoading ? (
              <div className="flex items-center justify-center py-12">
                <ArrowPathIcon className="h-8 w-8 animate-spin text-purple-600" />
              </div>
            ) : detail ? (
              <div className="space-y-6">
                {/* 기본 정보 */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-sm font-medium text-gray-500">대상 타입</label>
                    <p className="mt-1">
                      <span className={`inline-flex px-2.5 py-1 rounded-full text-xs font-medium ${getTargetTypeColorClass(detail.targetType)}`}>
                        {getTargetTypeLabel(detail.targetType)}
                      </span>
                    </p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-500">액션</label>
                    <p className="mt-1">
                      <span className={`inline-flex px-2.5 py-1 rounded-full text-xs font-medium ${getActionColorClass(detail.action)}`}>
                        {getActionLabel(detail.action)}
                      </span>
                    </p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-500">대상 ID</label>
                    <p className="mt-1 text-sm text-gray-900 font-mono">{detail.targetId}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-500">대상 이름</label>
                    <p className="mt-1 text-sm text-gray-900">{detail.targetName || "-"}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-500">변경자</label>
                    <p className="mt-1 text-sm text-gray-900">{detail.changedBy}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-500">변경 시각</label>
                    <p className="mt-1 text-sm text-gray-900">{formatDate(detail.changedAt)}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-gray-500">IP 주소</label>
                    <p className="mt-1 text-sm text-gray-900 font-mono">{detail.ipAddress || "-"}</p>
                  </div>
                </div>

                {/* 변경 필드 */}
                {detail.changedFields && (
                  <div>
                    <label className="text-sm font-medium text-gray-500">변경된 필드</label>
                    <div className="mt-1 flex flex-wrap gap-2">
                      {JSON.parse(detail.changedFields).map((field: string) => (
                        <span key={field} className="px-2 py-1 bg-yellow-100 text-yellow-800 rounded text-xs font-medium">
                          {field}
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                {/* 변경 전 상태 */}
                {detail.beforeSnapshot && (
                  <div>
                    <button
                      onClick={() => setShowBefore(!showBefore)}
                      className="flex items-center gap-2 text-sm font-medium text-gray-700 hover:text-gray-900"
                    >
                      {showBefore ? <ChevronUpIcon className="h-4 w-4" /> : <ChevronDownIcon className="h-4 w-4" />}
                      변경 전 상태
                    </button>
                    {showBefore && (
                      <pre className="mt-2 p-4 bg-red-50 border border-red-200 rounded-lg text-xs text-gray-700 overflow-x-auto max-h-64">
                        {formatJson(detail.beforeSnapshot)}
                      </pre>
                    )}
                  </div>
                )}

                {/* 변경 후 상태 */}
                {detail.afterSnapshot && (
                  <div>
                    <button
                      onClick={() => setShowAfter(!showAfter)}
                      className="flex items-center gap-2 text-sm font-medium text-gray-700 hover:text-gray-900"
                    >
                      {showAfter ? <ChevronUpIcon className="h-4 w-4" /> : <ChevronDownIcon className="h-4 w-4" />}
                      변경 후 상태
                    </button>
                    {showAfter && (
                      <pre className="mt-2 p-4 bg-green-50 border border-green-200 rounded-lg text-xs text-gray-700 overflow-x-auto max-h-64">
                        {formatJson(detail.afterSnapshot)}
                      </pre>
                    )}
                  </div>
                )}
              </div>
            ) : (
              <p className="text-center text-gray-500 py-12">상세 정보를 불러올 수 없습니다.</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default function ChangeHistoryPage() {
  // 필터 상태
  const [selectedType, setSelectedType] = useState<ConfigAuditTargetType | "">("");
  const [selectedAction, setSelectedAction] = useState<ConfigAuditAction | "">("");
  const [searchTerm, setSearchTerm] = useState("");
  const [startDate, setStartDate] = useState(getDaysAgo(7)); // 기본 7일 전
  const [endDate, setEndDate] = useState(getToday());

  // 페이징 상태
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 20;

  // 모달 상태
  const [selectedAudit, setSelectedAudit] = useState<AuditSummary | null>(null);
  const [auditDetail, setAuditDetail] = useState<AuditDetail | null>(null);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);

  // 검색 쿼리 파라미터
  const searchParams = useMemo(() => ({
    targetType: selectedType || undefined,
    action: selectedAction || undefined,
    startDate: startDate ? toISODateTime(startDate, false) : undefined,
    endDate: endDate ? toISODateTime(endDate, true) : undefined,
    search: searchTerm || undefined,
    page: currentPage,
    size: pageSize,
  }), [selectedType, selectedAction, startDate, endDate, searchTerm, currentPage]);

  // 통합 검색 쿼리
  const { data: searchResult, isLoading, refetch } = useQuery<PageResponse<AuditSummary>>({
    queryKey: ["auditHistory", searchParams],
    queryFn: () => searchAuditHistory(searchParams),
    refetchInterval: 30000,
  });

  // 필터 변경 시 페이지 초기화
  const handleFilterChange = <T,>(setter: (v: T) => void, value: T) => {
    setter(value);
    setCurrentPage(0);
  };

  // 상세 정보 조회
  const handleViewDetail = async (audit: AuditSummary) => {
    setSelectedAudit(audit);
    setIsLoadingDetail(true);
    try {
      const details = await fetchAuditHistoryByTarget(audit.targetType, audit.targetId);
      const detail = details.find(d => d.auditId === audit.auditId);
      setAuditDetail(detail || null);
    } catch (error) {
      console.error("Failed to fetch audit detail:", error);
      setAuditDetail(null);
    } finally {
      setIsLoadingDetail(false);
    }
  };

  // 빠른 날짜 필터
  const setQuickDateFilter = (days: number) => {
    setStartDate(getDaysAgo(days));
    setEndDate(getToday());
    setCurrentPage(0);
  };

  return (
    <div className="p-6 space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">변경이력</h1>
          <p className="text-sm text-gray-500 mt-1">센서, 룰, 시나리오의 변경 이력을 조회합니다</p>
        </div>
        <button
          onClick={() => refetch()}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors"
        >
          <ArrowPathIcon className={`h-5 w-5 ${isLoading ? 'animate-spin' : ''}`} />
          <span>새로고침</span>
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4 space-y-4">
        {/* 첫 번째 줄: 검색, 타입, 액션 */}
        <div className="flex flex-col md:flex-row gap-4">
          {/* 검색 */}
          <div className="flex-1 relative">
            <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              type="text"
              placeholder="ID, 이름, 변경자로 검색..."
              value={searchTerm}
              onChange={(e) => handleFilterChange(setSearchTerm, e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
            />
          </div>

          {/* 타입 필터 */}
          <div className="flex items-center gap-2">
            <FunnelIcon className="h-5 w-5 text-gray-400" />
            <select
              value={selectedType}
              onChange={(e) => handleFilterChange(setSelectedType, e.target.value as ConfigAuditTargetType | "")}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
            >
              <option value="">전체 타입</option>
              <option value="SENSOR">센서</option>
              <option value="RULE">룰</option>
              <option value="SCENARIO">시나리오</option>
            </select>
          </div>

          {/* 액션 필터 */}
          <div>
            <select
              value={selectedAction}
              onChange={(e) => handleFilterChange(setSelectedAction, e.target.value as ConfigAuditAction | "")}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
            >
              <option value="">전체 액션</option>
              <option value="CREATE">생성</option>
              <option value="UPDATE">수정</option>
              <option value="DELETE">삭제</option>
            </select>
          </div>
        </div>

        {/* 두 번째 줄: 날짜 필터 */}
        <div className="flex flex-col md:flex-row gap-4 items-center">
          <div className="flex items-center gap-2">
            <CalendarIcon className="h-5 w-5 text-gray-400" />
            <span className="text-sm text-gray-600">기간</span>
          </div>

          {/* 빠른 날짜 버튼 */}
          <div className="flex gap-2">
            <button
              onClick={() => setQuickDateFilter(1)}
              className="px-3 py-1.5 text-sm bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
            >
              오늘
            </button>
            <button
              onClick={() => setQuickDateFilter(7)}
              className="px-3 py-1.5 text-sm bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
            >
              7일
            </button>
            <button
              onClick={() => setQuickDateFilter(30)}
              className="px-3 py-1.5 text-sm bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
            >
              30일
            </button>
            <button
              onClick={() => setQuickDateFilter(90)}
              className="px-3 py-1.5 text-sm bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
            >
              90일
            </button>
          </div>

          {/* 날짜 입력 */}
          <div className="flex items-center gap-2">
            <input
              type="date"
              value={startDate}
              onChange={(e) => handleFilterChange(setStartDate, e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
            />
            <span className="text-gray-500">~</span>
            <input
              type="date"
              value={endDate}
              onChange={(e) => handleFilterChange(setEndDate, e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
            />
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        {isLoading ? (
          <div className="flex items-center justify-center py-12">
            <ArrowPathIcon className="h-8 w-8 animate-spin text-purple-600" />
            <span className="ml-2 text-gray-600">로딩 중...</span>
          </div>
        ) : searchResult && searchResult.content.length > 0 ? (
          <>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      시간
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      타입
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      액션
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      대상
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      변경자
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                      상세
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {searchResult.content.map((audit) => (
                    <tr key={audit.auditId} className="hover:bg-gray-50 transition-colors">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-900">{formatRelativeTime(audit.changedAt)}</div>
                        <div className="text-xs text-gray-500">{formatDate(audit.changedAt)}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`inline-flex px-2.5 py-1 rounded-full text-xs font-medium ${getTargetTypeColorClass(audit.targetType)}`}>
                          {getTargetTypeLabel(audit.targetType)}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`inline-flex px-2.5 py-1 rounded-full text-xs font-medium ${getActionColorClass(audit.action)}`}>
                          {getActionLabel(audit.action)}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <div className="text-sm font-medium text-gray-900">{audit.targetName || "-"}</div>
                        <div className="text-xs text-gray-500 font-mono">{audit.targetId}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-900">{audit.changedBy}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right">
                        <button
                          onClick={() => handleViewDetail(audit)}
                          className="text-purple-600 hover:text-purple-900 font-medium text-sm"
                        >
                          상세보기
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* 페이지네이션 */}
            <div className="px-6 py-4 border-t border-gray-200 flex items-center justify-between">
              <div className="text-sm text-gray-500">
                전체 {searchResult.totalElements}건 중 {currentPage * pageSize + 1} - {Math.min((currentPage + 1) * pageSize, searchResult.totalElements)}건
              </div>
              <Pagination
                currentPage={currentPage}
                totalPages={searchResult.totalPages}
                onPageChange={setCurrentPage}
              />
            </div>
          </>
        ) : (
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
              <ClockIcon className="h-8 w-8 text-gray-400" />
            </div>
            <h3 className="text-lg font-medium text-gray-900 mb-1">변경이력이 없습니다</h3>
            <p className="text-gray-500">검색 조건을 변경하거나, 센서/룰/시나리오를 생성/수정/삭제하면 이력이 기록됩니다.</p>
          </div>
        )}
      </div>

      {/* 통계 카드 */}
      {searchResult && searchResult.totalElements > 0 && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
            <div className="text-2xl font-bold text-gray-900">{searchResult.totalElements}</div>
            <div className="text-sm text-gray-500">검색 결과</div>
          </div>
          <div className="bg-green-50 rounded-xl border border-green-200 p-4">
            <div className="text-2xl font-bold text-green-700">
              {searchResult.content.filter(a => a.action === 'CREATE').length}
            </div>
            <div className="text-sm text-green-600">생성 (현재 페이지)</div>
          </div>
          <div className="bg-blue-50 rounded-xl border border-blue-200 p-4">
            <div className="text-2xl font-bold text-blue-700">
              {searchResult.content.filter(a => a.action === 'UPDATE').length}
            </div>
            <div className="text-sm text-blue-600">수정 (현재 페이지)</div>
          </div>
          <div className="bg-red-50 rounded-xl border border-red-200 p-4">
            <div className="text-2xl font-bold text-red-700">
              {searchResult.content.filter(a => a.action === 'DELETE').length}
            </div>
            <div className="text-sm text-red-600">삭제 (현재 페이지)</div>
          </div>
        </div>
      )}

      {/* Detail Modal */}
      {selectedAudit && (
        <AuditDetailModal
          audit={selectedAudit}
          detail={auditDetail}
          isLoading={isLoadingDetail}
          onClose={() => {
            setSelectedAudit(null);
            setAuditDetail(null);
          }}
        />
      )}
    </div>
  );
}

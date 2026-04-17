"use client";

import { useState, useEffect, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { detectActionApi, DetectAction, DetectActionSearchParams, UpdateDetectActionRequest } from "./api";
import {
  ShieldExclamationIcon,
  CheckCircleIcon,
  XCircleIcon,
  ClockIcon,
  XMarkIcon,
  MagnifyingGlassIcon,
  FunnelIcon,
  CalendarIcon,
} from "@heroicons/react/24/outline";
import { CheckIcon } from "@heroicons/react/20/solid";
import dayjs from "dayjs";
import utc from "dayjs/plugin/utc";
import timezone from "dayjs/plugin/timezone";

// dayjs 플러그인 설정
dayjs.extend(utc);
dayjs.extend(timezone);

// 한국 시간 기준 날짜 문자열 반환 (YYYY-MM-DD)
const getKoreanDate = (date?: Date): string => {
  return dayjs(date).tz("Asia/Seoul").format("YYYY-MM-DD");
};

function DetectActionsPageContent() {
  const urlParams = useSearchParams();
  const [actions, setActions] = useState<DetectAction[]>([]);
  const [loading, setLoading] = useState(true);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // 모달 상태
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedAction, setSelectedAction] = useState<DetectAction | null>(null);
  const [formData, setFormData] = useState<UpdateDetectActionRequest>({});
  const [saving, setSaving] = useState(false);

  // 검색 파라미터 - URL에서 riskLevel 가져오기
  const [searchParams, setSearchParams] = useState<DetectActionSearchParams>({
    startDate: getKoreanDate(),
    endDate: getKoreanDate(),
    riskLevel: urlParams.get("riskLevel") || undefined,
    page: 0,
    size: 20,
  });

  // 조치 상태 다중 선택
  const [selectedStatuses, setSelectedStatuses] = useState<string[]>([]);

  // 조치 상태 토글
  const toggleStatus = (status: string) => {
    setSelectedStatuses(prev => {
      const newStatuses = prev.includes(status)
        ? prev.filter(s => s !== status)
        : [...prev, status];

      // searchParams 업데이트
      setSearchParams({
        ...searchParams,
        actionStatus: newStatuses.length > 0 ? newStatuses.join(',') : undefined,
        page: 0, // 필터 변경 시 첫 페이지로
      });

      return newStatuses;
    });
  };

  const fetchActions = async () => {
    try {
      setLoading(true);
      const data = await detectActionApi.getDetectActions(searchParams);
      setActions(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (error) {
      console.error("Failed to fetch detect actions:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchActions();
  }, [searchParams]);

  const handlePageChange = (newPage: number) => {
    setSearchParams({ ...searchParams, page: newPage });
  };

  // 모달 열기
  const handleOpenModal = (action: DetectAction) => {
    setSelectedAction(action);
    setFormData({
      actionMemo: action.actionMemo || "",
      actionReason: action.actionReason || "",
      actionStatus: action.actionStatus,
    });
    setIsModalOpen(true);
  };

  // 모달 닫기
  const handleCloseModal = () => {
    setIsModalOpen(false);
    setSelectedAction(null);
    setFormData({});
  };

  // 저장
  const handleSave = async () => {
    if (!selectedAction) return;

    try {
      setSaving(true);
      await detectActionApi.updateDetectAction(selectedAction.detectActionId, formData);

      // 목록 새로고침
      await fetchActions();
      handleCloseModal();
    } catch (error) {
      console.error("Failed to update detect action:", error);
      alert("조치 업데이트에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  // 위험수준 뱃지 스타일
  const getRiskLevelBadge = (riskLevel: string) => {
    const styles = {
      BLOCK: "bg-red-100 text-red-700 border-red-200",
      REVIEW: "bg-orange-100 text-orange-700 border-orange-200",
      INTENSIVE: "bg-yellow-100 text-yellow-700 border-yellow-200",
      MONITOR: "bg-green-100 text-green-700 border-green-200",
    };
    const labels = {
      BLOCK: "차단",
      REVIEW: "심사",
      INTENSIVE: "집중모니터링",
      MONITOR: "모니터링",
    };
    return (
      <span className={`px-2 py-1 text-xs font-medium rounded-full border ${styles[riskLevel as keyof typeof styles] || "bg-gray-100 text-gray-700"}`}>
        {labels[riskLevel as keyof typeof labels] || riskLevel}
      </span>
    );
  };

  // 조치 상태 아이콘 및 텍스트
  const getActionStatusBadge = (status: string) => {
    const config = {
      PENDING: { icon: ClockIcon, label: "미처리", color: "text-gray-600 bg-gray-100" },
      APPROVED: { icon: CheckCircleIcon, label: "승인", color: "text-blue-600 bg-blue-100" },
      REJECTED: { icon: XCircleIcon, label: "거절", color: "text-red-600 bg-red-100" },
      COMPLETED: { icon: CheckCircleIcon, label: "완료", color: "text-green-600 bg-green-100" },
    };

    const { icon: Icon, label, color } = config[status as keyof typeof config] || config.PENDING;

    return (
      <div className={`flex items-center gap-1 px-2 py-1 rounded-full ${color}`}>
        <Icon className="h-4 w-4" />
        <span className="text-xs font-medium">{label}</span>
      </div>
    );
  };

  return (
    <div className="p-6 space-y-6">
      {/* 헤더 */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">탐지 조치 관리</h1>
        <p className="text-sm text-gray-500 mt-1">차단 및 심사 탐지 건에 대한 조치를 관리합니다</p>
      </div>

      {/* 검색 필터 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4 space-y-4">
        {/* 첫 번째 줄: 위험수준, 조치 상태 */}
        <div className="flex flex-col md:flex-row gap-4">
          {/* 위험수준 필터 */}
          <div className="flex items-center gap-2">
            <FunnelIcon className="h-5 w-5 text-gray-400" />
            <select
              value={searchParams.riskLevel || ""}
              onChange={(e) => setSearchParams({ ...searchParams, riskLevel: e.target.value || undefined })}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="">전체 위험수준</option>
              <option value="BLOCK">차단</option>
              <option value="REVIEW">심사</option>
              <option value="INTENSIVE">집중모니터링</option>
              <option value="MONITOR">모니터링</option>
            </select>
          </div>

          {/* 조치 상태 필터 - Chips */}
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-sm text-gray-600">조치상태:</span>
            {[
              { value: 'PENDING', label: '미처리', bg: 'bg-gray-100', text: 'text-gray-700', border: 'border-gray-400' },
              { value: 'APPROVED', label: '승인', bg: 'bg-blue-100', text: 'text-blue-700', border: 'border-blue-500' },
              { value: 'REJECTED', label: '거절', bg: 'bg-red-100', text: 'text-red-700', border: 'border-red-500' },
              { value: 'COMPLETED', label: '완료', bg: 'bg-green-100', text: 'text-green-700', border: 'border-green-500' },
            ].map(status => (
              <button
                key={status.value}
                onClick={() => toggleStatus(status.value)}
                className={`
                  px-3 py-1.5 rounded-lg text-sm font-medium transition-all border
                  ${status.border}
                  ${selectedStatuses.includes(status.value)
                    ? `${status.bg} ${status.text}`
                    : 'bg-white text-gray-600 hover:bg-gray-50'
                  }
                `}
              >
                {status.label}
              </button>
            ))}
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
              onClick={() => {
                const today = getKoreanDate();
                setSearchParams({ ...searchParams, startDate: today, endDate: today });
              }}
              className="px-3 py-1.5 text-sm bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
            >
              오늘
            </button>
            <button
              onClick={() => {
                const today = dayjs().tz("Asia/Seoul");
                const weekAgo = today.subtract(7, 'day');
                setSearchParams({
                  ...searchParams,
                  startDate: weekAgo.format("YYYY-MM-DD"),
                  endDate: today.format("YYYY-MM-DD")
                });
              }}
              className="px-3 py-1.5 text-sm bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
            >
              7일
            </button>
            <button
              onClick={() => {
                const today = dayjs().tz("Asia/Seoul");
                const monthAgo = today.subtract(30, 'day');
                setSearchParams({
                  ...searchParams,
                  startDate: monthAgo.format("YYYY-MM-DD"),
                  endDate: today.format("YYYY-MM-DD")
                });
              }}
              className="px-3 py-1.5 text-sm bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
            >
              30일
            </button>
          </div>

          {/* 날짜 입력 */}
          <div className="flex items-center gap-2">
            <input
              type="date"
              value={searchParams.startDate || ""}
              onChange={(e) => setSearchParams({ ...searchParams, startDate: e.target.value })}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            <span className="text-gray-500">~</span>
            <input
              type="date"
              value={searchParams.endDate || ""}
              onChange={(e) => setSearchParams({ ...searchParams, endDate: e.target.value })}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
        </div>
      </div>

      {/* 목록 헤더 */}
      <div className="flex items-center justify-between">
        <div className="text-sm text-gray-600">
          총 <span className="font-semibold text-gray-900">{totalElements}</span>건
        </div>
      </div>

      {/* 테이블 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  ID
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  시나리오
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  그룹키
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  위험수준
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  조치 상태
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  요청자
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  요청일시
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {loading ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-gray-500">
                    로딩 중...
                  </td>
                </tr>
              ) : actions.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-gray-500">
                    조회된 데이터가 없습니다.
                  </td>
                </tr>
              ) : (
                actions.map((action) => (
                  <tr key={action.detectActionId} className="hover:bg-gray-50 transition-colors">
                    <td className="px-6 py-4">
                      <button
                        onClick={() => handleOpenModal(action)}
                        className="text-sm font-semibold text-blue-600 hover:text-blue-800 hover:underline"
                      >
                        {action.detectActionId}
                      </button>
                    </td>
                    <td className="px-6 py-4">
                      <div className="text-sm font-medium text-gray-900">{action.scenarioName}</div>
                      <div className="text-xs text-gray-500">{action.scenarioId}</div>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-900">{action.groupKey}</td>
                    <td className="px-6 py-4">{getRiskLevelBadge(action.riskLevel)}</td>
                    <td className="px-6 py-4">{getActionStatusBadge(action.actionStatus)}</td>
                    <td className="px-6 py-4 text-sm text-gray-900">{action.requestedBy}</td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                      {new Date(action.requestedAt).toLocaleString("ko-KR")}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* 페이지네이션 */}
        {totalPages > 1 && (
          <div className="bg-gray-50 px-6 py-4 flex items-center justify-between border-t border-gray-200">
            <button
              onClick={() => handlePageChange(searchParams.page! - 1)}
              disabled={searchParams.page === 0}
              className="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              이전
            </button>
            <span className="text-sm text-gray-700">
              {searchParams.page! + 1} / {totalPages}
            </span>
            <button
              onClick={() => handlePageChange(searchParams.page! + 1)}
              disabled={searchParams.page! + 1 >= totalPages}
              className="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              다음
            </button>
          </div>
        )}
      </div>

      {/* 조치 처리 모달 */}
      {isModalOpen && selectedAction && (
        <div className="fixed inset-0 z-50 overflow-y-auto">
          {/* 배경 오버레이 */}
          <div
            className="fixed inset-0 bg-black/10 transition-opacity"
            onClick={handleCloseModal}
          ></div>

          {/* 모달 컨텐츠 */}
          <div className="flex min-h-full items-center justify-center p-4">
            <div className="relative bg-white rounded-xl shadow-xl max-w-2xl w-full p-6">
              {/* 헤더 */}
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-xl font-bold text-gray-900">
                  탐지 조치 처리 #{selectedAction.detectActionId}
                </h2>
                <button
                  onClick={handleCloseModal}
                  className="text-gray-400 hover:text-gray-600"
                >
                  <XMarkIcon className="h-6 w-6" />
                </button>
              </div>

              {/* 기본 정보 */}
              <div className="mb-6 p-4 bg-gray-50 rounded-lg">
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="text-gray-500">시나리오:</span>
                    <span className="ml-2 font-medium text-gray-900">{selectedAction.scenarioName}</span>
                  </div>
                  <div>
                    <span className="text-gray-500">그룹키:</span>
                    <span className="ml-2 font-medium text-gray-900">{selectedAction.groupKey}</span>
                  </div>
                  <div>
                    <span className="text-gray-500">위험수준:</span>
                    <span className="ml-2">{getRiskLevelBadge(selectedAction.riskLevel)}</span>
                  </div>
                  <div>
                    <span className="text-gray-500">요청자:</span>
                    <span className="ml-2 font-medium text-gray-900">{selectedAction.requestedBy}</span>
                  </div>
                </div>
              </div>

              {/* 조치 입력 폼 */}
              <div className="space-y-4">
                {/* 조치 상태 */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    조치 상태 <span className="text-red-500">*</span>
                  </label>
                  <select
                    value={formData.actionStatus || ""}
                    onChange={(e) => setFormData({ ...formData, actionStatus: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  >
                    <option value="PENDING">미처리</option>
                    <option value="APPROVED">승인</option>
                    <option value="REJECTED">거절</option>
                    <option value="COMPLETED">완료</option>
                  </select>
                </div>

                {/* 조치 사유 */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    조치 사유
                  </label>
                  <textarea
                    value={formData.actionReason || ""}
                    onChange={(e) => setFormData({ ...formData, actionReason: e.target.value })}
                    rows={3}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    placeholder="조치 사유를 입력하세요"
                  />
                </div>

                {/* 조치 메모 */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    조치 메모
                  </label>
                  <textarea
                    value={formData.actionMemo || ""}
                    onChange={(e) => setFormData({ ...formData, actionMemo: e.target.value })}
                    rows={3}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    placeholder="조치 메모를 입력하세요"
                  />
                </div>
              </div>

              {/* 버튼 */}
              <div className="flex gap-3 mt-6 justify-end">
                <button
                  onClick={handleCloseModal}
                  className="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
                  disabled={saving}
                >
                  취소
                </button>
                <button
                  onClick={handleSave}
                  disabled={saving}
                  className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {saving ? "저장 중..." : "저장"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default function DetectActionsPage() {
  return (
    <Suspense fallback={<div className="p-6">불러오는 중…</div>}>
      <DetectActionsPageContent />
    </Suspense>
  );
}

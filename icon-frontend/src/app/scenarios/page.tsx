"use client";

import LoadingButton from "@/components/common/LoadingButton";
import { useErrorHandling } from "@/hooks/useErrorHandling";
import {
  PencilIcon,
  PlusIcon,
  Bars3Icon,
  Squares2X2Icon,
} from "@heroicons/react/24/outline";
import { useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useState, useEffect } from "react";
import toast from "react-hot-toast";
import { activateScenario, deactivateScenario, fetchScenariosPaged, type ScenarioWithRules, type ResponseList } from "./api";

export default function ScenariosPage() {
  const router = useRouter();
  const { handleError } = useErrorHandling();
  const [activatingId, setActivatingId] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<"list" | "card">("list");

  // 시나리오 목록 조회
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [sort, setSort] = useState<"name" | "regDt">("name");
  const [dir, setDir] = useState<"asc" | "desc">("asc");
  const [activeFilter, setActiveFilter] = useState<boolean | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");

  // 검색어 디바운싱 (300ms)
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch((prev) => {
        if (prev !== searchQuery) {
          setPage(1);
        }
        return searchQuery;
      });
    }, 300);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  // localStorage에서 뷰 모드 복원
  useEffect(() => {
    const savedViewMode = localStorage.getItem("scenarios-view-mode");
    if (savedViewMode === "list" || savedViewMode === "card") {
      setViewMode(savedViewMode);
    }
  }, []);

  const handleViewModeChange = (mode: "list" | "card") => {
    setViewMode(mode);
    localStorage.setItem("scenarios-view-mode", mode);
  };

  const { data: paged, isLoading, isFetching, refetch } = useQuery({
    queryKey: ["scenarios", page, size, sort, dir, activeFilter, debouncedSearch],
    queryFn: () => fetchScenariosPaged({
      activeOnly: activeFilter ?? undefined,
      page,
      size,
      sort,
      dir,
      search: debouncedSearch || undefined,
    }),
    placeholderData: (prev) => prev, // 이전 데이터 유지
  });
  const scenarios = (paged?.data || []) as ScenarioWithRules[];
  const total = Number(paged?.total || 0);

  // 시나리오 활성화/비활성화
  const handleToggleActive = async (scenario: ScenarioWithRules) => {
    setActivatingId(scenario.scenarioId);
    try {
      if (scenario.isActive) {
        await deactivateScenario(scenario.scenarioId);
        toast.success("시나리오가 비활성화되었습니다.");
      } else {
        await activateScenario(scenario.scenarioId);
        toast.success("시나리오가 활성화되었습니다.");
      }
      refetch();
    } catch (error) {
      handleError(error);
    } finally {
      setActivatingId(null);
    }
  };

  if (isLoading) {
    return (
      <div className="p-6 space-y-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200">
          <div className="flex justify-center items-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">시나리오 관리</h1>
          <p className="text-sm text-gray-500 mt-1">
            복수의 집계를 조합하여 복잡한 탐지 시나리오를 구성합니다.
          </p>
        </div>
        <LoadingButton
          onClick={() => router.push("/scenarios/new")}
          type="button"
          size="small"
          className="inline-flex items-center gap-2 bg-blue-600 hover:bg-blue-700"
        >
          <PlusIcon className="h-5 w-5" />
          새 시나리오 생성
        </LoadingButton>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
          <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">이름 또는 ID 검색</label>
              <input
                type="text"
                placeholder="시나리오 이름 또는 ID..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">활성 여부</label>
              <select
                value={activeFilter === null ? "all" : activeFilter ? "active" : "inactive"}
                onChange={(e) => {
                  const val = e.target.value;
                  setActiveFilter(val === "all" ? null : val === "active");
                  setPage(1);
                }}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="all">전체</option>
                <option value="active">활성만</option>
                <option value="inactive">비활성만</option>
              </select>
            </div>
          </div>
          <div className="mt-4 flex items-center justify-between">
            <div className="text-sm text-gray-600 flex items-center gap-2">
              총 {total}건
              {isFetching && (
                <span className="inline-block w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
              )}
            </div>
            <div className="flex items-center gap-3">
              <label className="text-sm text-gray-700">정렬</label>
              <select
                value={sort}
                onChange={(e) => {
                  setSort(e.target.value as any);
                  setPage(1);
                }}
                className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="name">이름</option>
                <option value="regDt">생성일</option>
              </select>
              <button
                onClick={() => {
                  setDir(d => d === "asc" ? "desc" : "asc");
                  setPage(1);
                }}
                className="px-3 py-2 border border-gray-300 rounded-lg hover:bg-gray-100 text-sm"
                title="오름/내림차순"
              >
                {dir === "asc" ? "오름차순" : "내림차순"}
              </button>
              <button
                onClick={() => {
                  setSearchQuery("");
                  setDebouncedSearch("");
                  setActiveFilter(null);
                  setSort("name");
                  setDir("asc");
                  setSize(20);
                  setPage(1);
                }}
                className="px-3 py-2 border border-gray-300 rounded-lg hover:bg-gray-100 text-sm"
                title="필터 초기화"
              >
                초기화
              </button>

              {/* 뷰 모드 토글 */}
              <div className="flex items-center gap-2 bg-white rounded-lg border border-gray-300 p-1">
                <button
                  onClick={() => handleViewModeChange("list")}
                  className={`p-2 rounded transition-colors ${
                    viewMode === "list"
                      ? "bg-blue-100 text-blue-600"
                      : "text-gray-600 hover:bg-gray-100"
                  }`}
                  title="리스트 보기"
                >
                  <Bars3Icon className="h-5 w-5" />
                </button>
                <button
                  onClick={() => handleViewModeChange("card")}
                  className={`p-2 rounded transition-colors ${
                    viewMode === "card"
                      ? "bg-blue-100 text-blue-600"
                      : "text-gray-600 hover:bg-gray-100"
                  }`}
                  title="카드 보기"
                >
                  <Squares2X2Icon className="h-5 w-5" />
                </button>
              </div>
            </div>
          </div>
      </div>

      {/* Scenario List */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          {isLoading ? (
            <div className="text-center py-12 text-gray-500">로딩 중…</div>
          ) : scenarios.length === 0 ? (
            <div className="text-center py-12">
              <div className="mx-auto h-12 w-12 text-gray-400">
                <svg className="w-full h-full" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                </svg>
              </div>
              <h3 className="mt-2 text-sm font-medium text-gray-900">
                시나리오가 없습니다
              </h3>
              <p className="mt-1 text-sm text-gray-500">
                새 시나리오를 생성하여 시작하세요.
              </p>
            </div>
          ) : viewMode === "card" ? (
            /* 카드 뷰 */
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {scenarios.map((scenario) => (
                <div
                  key={scenario.scenarioId}
                  className="bg-white rounded-xl border border-gray-200 p-6 hover:shadow-lg hover:border-blue-300 transition-all duration-200"
                >
                  {/* 카드 헤더 */}
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex-1">
                      {/* Scenario ID */}
                      <div className="text-xs font-mono text-gray-500 mb-2">
                        {scenario.scenarioId}
                      </div>
                      <h3 className="text-lg font-bold text-gray-900 mb-1">{scenario.scenarioName}</h3>
                      {scenario.description && (
                        <p className="text-sm text-gray-600">{scenario.description}</p>
                      )}
                    </div>
                    <div className="ml-2">
                      <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ${
                        scenario.isActive ? "bg-green-100 text-green-800" : "bg-gray-100 text-gray-800"
                      }`}>
                        {scenario.isActive ? "활성" : "비활성"}
                      </span>
                    </div>
                  </div>

                  {/* 집계 개수 */}
                  <div className="mb-4">
                    <div className="text-xs font-medium text-gray-500 mb-1">집계 수</div>
                    <div className="inline-flex items-center px-3 py-1.5 rounded-lg bg-purple-100 text-purple-800 text-sm font-medium">
                      {scenario.rules?.length || 0}개
                    </div>
                  </div>

                  {/* 생성일 */}
                  {scenario.regDt && (
                    <div className="mb-4">
                      <div className="text-xs font-medium text-gray-500 mb-1">생성일</div>
                      <div className="text-sm text-gray-700">
                        {new Date(scenario.regDt).toLocaleDateString()}
                      </div>
                    </div>
                  )}

                  {/* 집계 목록 미리보기 */}
                  {scenario.rules && scenario.rules.length > 0 && (
                    <div className="mb-4">
                      <div className="text-xs font-medium text-gray-500 mb-2">집계 구성</div>
                      <div className="bg-gray-100 rounded-lg p-3 border border-gray-200 max-h-32 overflow-y-auto">
                        <div className="flex flex-wrap gap-1.5">
                          {scenario.rules
                            .sort((a, b) => a.orderNo - b.orderNo)
                            .map((scenarioRule, index) => (
                              <div key={scenarioRule.ruleId} className="flex items-center gap-1">
                                {index > 0 && (
                                  <span className={`text-xs font-bold px-1.5 py-0.5 rounded ${
                                    (scenarioRule as any).operator === "AND" || (scenarioRule as any).scenarioOperator === "AND"
                                      ? "bg-blue-200 text-blue-900"
                                      : "bg-orange-200 text-orange-900"
                                  }`}>
                                    {(scenarioRule as any).operator ?? (scenarioRule as any).scenarioOperator}
                                  </span>
                                )}
                                <span className="px-2 py-0.5 bg-white text-gray-700 rounded text-xs border border-gray-300">
                                  {scenarioRule.ruleName}
                                </span>
                              </div>
                            ))}
                        </div>
                      </div>
                    </div>
                  )}

                  {/* 편집 버튼 */}
                  <a
                    href={`/scenarios/${scenario.scenarioId}/edit`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium mt-4"
                  >
                    <PencilIcon className="h-4 w-4" />
                    편집
                  </a>
                </div>
              ))}
            </div>
          ) : (
            /* 리스트 뷰 */
            <div className="space-y-4">
              {scenarios.map((scenario) => (
                <div
                  key={scenario.scenarioId}
                  className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 hover:shadow-lg transition-shadow duration-200"
                >
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      {/* 이름 (ID) - 1줄 */}
                      <h3 className="text-lg font-semibold text-gray-900">
                        {scenario.scenarioName} <span className="text-sm font-mono text-gray-500">({scenario.scenarioId})</span>
                      </h3>

                      {/* 생성일 + 집계 목록 */}
                      {scenario.rules && scenario.rules.length > 0 && (
                        <div className="mt-3 flex items-center gap-4">
                          {/* 생성일 */}
                          {scenario.regDt && (
                            <span className="text-sm text-gray-500 whitespace-nowrap">
                              {new Date(scenario.regDt).toLocaleDateString()}
                            </span>
                          )}

                          {/* 집계 목록 */}
                          <div className="flex flex-wrap gap-2 items-center">
                            {scenario.rules
                              .sort((a, b) => a.orderNo - b.orderNo)
                              .map((scenarioRule, index) => (
                                <div
                                  key={scenarioRule.ruleId}
                                  className="flex items-center gap-2"
                                >
                                  {index > 0 && (
                                    <span
                                      className={`text-xs font-medium px-2 py-0.5 rounded ${
                                        (scenarioRule as any).operator === "AND" || (scenarioRule as any).scenarioOperator === "AND"
                                          ? "bg-blue-100 text-blue-700"
                                          : "bg-orange-100 text-orange-700"
                                      }`}
                                    >
                                      {(scenarioRule as any).operator ?? (scenarioRule as any).scenarioOperator}
                                    </span>
                                  )}
                                  <span className="px-2.5 py-0.5 bg-gray-100 text-gray-700 rounded-full text-xs">
                                    {scenarioRule.ruleName}
                                  </span>
                                </div>
                              ))}
                          </div>
                        </div>
                      )}
                    </div>

                    <div className="flex items-center space-x-3 ml-4">
                      {/* 활성화 토글 스위치 */}
                      <div className="flex items-center space-x-2">
                        <span
                          className={`text-xs font-medium ${
                            scenario.isActive
                              ? "text-green-600"
                              : "text-gray-500"
                          }`}
                        >
                          {scenario.isActive ? "활성" : "비활성"}
                        </span>
                        <button
                          onClick={() => handleToggleActive(scenario)}
                          disabled={activatingId === scenario.scenarioId}
                          className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 ${
                            scenario.isActive
                              ? "bg-green-500"
                              : "bg-gray-300"
                          } ${
                            activatingId === scenario.scenarioId
                              ? "opacity-50 cursor-not-allowed"
                              : "cursor-pointer"
                          }`}
                          title={
                            scenario.isActive ? "비활성화" : "활성화"
                          }
                        >
                          <span
                            className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                              scenario.isActive
                                ? "translate-x-6"
                                : "translate-x-1"
                            }`}
                          />
                        </button>
                      </div>

                      <a
                        href={`/scenarios/${scenario.scenarioId}/edit`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                        title="새 창에서 수정"
                      >
                        <PencilIcon className="h-5 w-5" />
                      </a>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

        {/* Pager */}
        <div className="mt-6 flex items-center justify-between">
          <div className="text-sm text-gray-600">총 {total}건</div>
          <div className="flex items-center gap-2">
            <button disabled={page <= 1} onClick={() => { setPage(p => Math.max(1, p-1)); }} className="px-3 py-1 border rounded disabled:opacity-50">이전</button>
            <span className="text-sm">{page} / {Math.max(1, Math.ceil(total / (size || 1)))}</span>
            <button disabled={page >= Math.ceil(total / (size || 1))} onClick={() => { setPage(p => p+1); }} className="px-3 py-1 border rounded disabled:opacity-50">다음</button>
          </div>
        </div>
      </div>
    </div>
  );
}

"use client";

import LoadingButton from "@/components/common/LoadingButton";
import {
  BeakerIcon,
  ChartBarIcon,
  CogIcon,
  MagnifyingGlassIcon,
  PencilIcon,
  PlusIcon,
  ShieldCheckIcon,
  TagIcon,
  Bars3Icon,
  Squares2X2Icon,
  ArrowPathIcon
} from "@heroicons/react/24/outline";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import {
  createRule,
  fetchRuleById,
  fetchRules,
  Rule,
  RuleCategory,
  RuleCreateRequest,
  RuleUpdateRequest,
  updateRule,
} from "./api";
// import { fetchRuleCategories } from "../metadata/api";
import Alert from "@/components/common/Alert";
import { useErrorHandling } from "@/hooks/useErrorHandling";
import { useQueryWithErrorHandling } from "@/hooks/useQueryWithErrorHandling";
import { useEffect } from "react";
import Modal from "../users/Modal";
import type { RuleFormValues } from "./RuleForm";
import RuleForm from "./RuleForm";

export default function RulesPage() {
  const queryClient = useQueryClient();
  const [searchKeyword, setSearchKeyword] = useState("");
  const [searchCategory, setSearchCategory] = useState<RuleCategory | "">("");
  const [activatingId, setActivatingId] = useState<string | null>(null);

  // 뷰 모드 상태 (localStorage에서 초기화)
  const [viewMode, setViewMode] = useState<"list" | "card">(() => {
    if (typeof window !== "undefined") {
      const saved = localStorage.getItem("rulesViewMode");
      return (saved === "list" || saved === "card") ? saved : "list";
    }
    return "list";
  });

  // 뷰 모드 변경 시 localStorage에 저장
  const handleViewModeChange = (mode: "list" | "card") => {
    setViewMode(mode);
    if (typeof window !== "undefined") {
      localStorage.setItem("rulesViewMode", mode);
    }
  };

  // 전체 룰 조회
  const {
    data: rulesResponse,
    isLoading,
    errorQuery: queryError,
  } = useQueryWithErrorHandling({
    queryKey: ["rules", "all"],
    queryFn: () => fetchRules(),
  });

  // v4: 카테고리 메타데이터 사용 안 함

  const allRules = rulesResponse?.data || [];

  // 클라이언트에서 필터링 (AGG_ 프리픽스만)
  const filteredRules = allRules.filter((rule) => {
    // AGG_ 프리픽스 체크 (룰만)
    const isRule = rule.ruleId.startsWith('AGG_');
    const keywordMatch =
      !searchKeyword ||
      rule.ruleId.toLowerCase().includes(searchKeyword.toLowerCase()) ||
      rule.name.toLowerCase().includes(searchKeyword.toLowerCase()) ||
      (rule.description && rule.description.toLowerCase().includes(searchKeyword.toLowerCase()));
    return isRule && keywordMatch;
  });

  const [editingRule, setEditingRule] = useState<Rule | null>(null);
  const [editingRuleId, setEditingRuleId] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const { error: formError, handleError, clearError } = useErrorHandling();

  // 수정용 상세 Rule 조회
  const { data: ruleDetail, isLoading: isLoadingRuleDetail } =
    useQueryWithErrorHandling({
      queryKey: ["rule", editingRuleId],
      queryFn: () => fetchRuleById(editingRuleId!),
      enabled: !!editingRuleId,
    });

  // ruleDetail이 로드되면 editingRule 상태 업데이트
  useEffect(() => {
    if (ruleDetail) {
      setEditingRule(ruleDetail);
    }
  }, [ruleDetail]);

  // 생성
  const createMutation = useMutation({
    mutationFn: (data: RuleCreateRequest) => createRule(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["rules"] });
      setShowForm(false);
    },
    onError: handleError,
  });

  // 수정
  const updateMutation = useMutation({
    mutationFn: ({
      ruleId,
      data,
    }: {
      ruleId: string;
      data: RuleUpdateRequest;
    }) => updateRule(ruleId, data),
    onSuccess: (res: Rule) => {
      queryClient.invalidateQueries({ queryKey: ["rules"] });
      queryClient.setQueryData(["rule", editingRuleId], res);
      setEditingRule(null);
      setEditingRuleId(null);
      setShowForm(false);
    },
    onError: handleError,
  });

  // 폼 제출 핸들러
  const handleSubmit = (values: RuleFormValues) => {
    clearError();

    // groupByFields를 배열로 변환
    const groupByFields = typeof values.groupByFields === 'string'
      ? (values.groupByFields as string).split(',').map(f => f.trim()).filter(Boolean)
      : values.groupByFields;

    if (editingRule) {
      const updateData: RuleUpdateRequest = {
        name: values.name,
        description: values.description,
        operator: values.operator,
        predicateSensorId: values.predicateSensorId || undefined,
        prevSensorId: values.prevSensorId || undefined,
        nextSensorId: values.nextSensorId || undefined,
        anchorSensorId: values.anchorSensorId || undefined,
        windowMinutes: values.windowMinutes,
        thresholdCount: values.thresholdCount,
        thresholdAmount: values.thresholdAmount,
        dedupMinutes: values.dedupMinutes,
        groupByFields: groupByFields,
        aggregationField: values.aggregationField || undefined,
        entityType: values.entityType,
        whereJson: values.whereJson,
        evaluationMode: values.evaluationMode,
      };
      updateMutation.mutate({
        ruleId: editingRule.ruleId,
        data: updateData,
      });
    } else {
      const createData: RuleCreateRequest = {
        ruleId: (values as any).ruleId,
        name: values.name,
        description: values.description,
        operator: values.operator || "COUNT_WITHIN",
        predicateSensorId: values.predicateSensorId || undefined,
        prevSensorId: values.prevSensorId || undefined,
        nextSensorId: values.nextSensorId || undefined,
        anchorSensorId: values.anchorSensorId || undefined,
        windowMinutes: values.windowMinutes,
        thresholdCount: values.thresholdCount,
        thresholdAmount: values.thresholdAmount,
        dedupMinutes: values.dedupMinutes,
        groupByFields: groupByFields,
        aggregationField: values.aggregationField || undefined,
        entityType: values.entityType || "ACCOUNT",
        whereJson: values.whereJson,
        evaluationMode: values.evaluationMode,
      };
      createMutation.mutate(createData);
    }
  };

  // 수정 버튼 클릭
  const handleEdit = (rule: Rule) => {
    setEditingRuleId(rule.ruleId);
    setShowForm(true);
    clearError();
  };

  // 새 룰 등록 버튼 클릭
  const handleNew = () => {
    setEditingRule(null);
    setEditingRuleId(null);
    setShowForm(true);
    clearError();
  };

  // 룰 활성화/비활성화 토글
  const handleToggleActive = async (rule: Rule) => {
    setActivatingId(rule.ruleId);
    try {
      await updateMutation.mutateAsync({
        ruleId: rule.ruleId,
        data: { isActive: !rule.isActive },
      });
    } finally {
      setActivatingId(null);
    }
  };

  // description에서 "Composite: " 접두사 제거
  const cleanDescription = (description: string | undefined) => {
    if (!description) return "";
    return description.replace(/^Composite:\s*/i, "");
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
          <h1 className="text-2xl font-bold text-gray-900">룰 관리</h1>
          <p className="text-sm text-gray-500 mt-1">
            데이터 검증 및 처리를 위한 비즈니스 룰을 관리합니다.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => queryClient.invalidateQueries({ queryKey: ["rules"] })}
            className="inline-flex items-center gap-2 px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-lg transition-colors"
            title="새로고침"
          >
            <ArrowPathIcon className="h-5 w-5" />
            새로고침
          </button>
          <LoadingButton
            onClick={handleNew}
            type="button"
            size="small"
            className="inline-flex items-center gap-2 bg-blue-600 hover:bg-blue-700"
          >
            <PlusIcon className="h-5 w-5" />
            새 룰 등록
          </LoadingButton>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
        <div className="flex items-center justify-between gap-4">
          {/* Search */}
          <div className="relative flex-1 max-w-md">
            <MagnifyingGlassIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              type="text"
              placeholder="룰 ID, 룰명 또는 설명으로 검색..."
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

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

      {/* 에러 표시 */}
      {(queryError || formError) && (
        <Alert message={`${(queryError || formError)?.message}`} />
      )}

      {/* Rule List */}
      {!isLoading && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            {filteredRules.length === 0 ? (
              <div className="text-center py-12">
                <CogIcon className="mx-auto h-12 w-12 text-gray-400" />
                <h3 className="mt-2 text-sm font-medium text-gray-900">
                  룰이 없습니다
                </h3>
                <p className="mt-1 text-sm text-gray-500">
                  {searchKeyword || searchCategory ? "검색 결과가 없습니다." : "새 룰을 생성하여 시작하세요."}
                </p>
              </div>
            ) : viewMode === "card" ? (
              /* 카드 뷰 */
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {filteredRules.map((rule) => (
                    <div
                      key={rule.ruleId}
                      className="bg-white rounded-xl border border-gray-200 p-6 hover:shadow-lg hover:border-blue-300 transition-all duration-200"
                    >
                      {/* 카드 헤더 */}
                      <div className="flex items-start justify-between mb-4">
                        <div></div>
                        <div className="flex items-center gap-2">
                          {/* 수정 버튼 */}
                          <button
                            onClick={() => handleEdit(rule)}
                            className="text-sm text-blue-600 hover:text-blue-700 font-medium"
                          >
                            수정
                          </button>
                          {/* 활성화 토글 */}
                          <div className="flex flex-col items-end">
                            <span
                              className={`text-xs font-medium mb-1 ${
                                rule.isActive ? "text-green-600" : "text-gray-500"
                              }`}
                            >
                              {rule.isActive ? "활성" : "비활성"}
                            </span>
                            <button
                              onClick={() => handleToggleActive(rule)}
                              disabled={activatingId === rule.ruleId}
                              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 ${
                                rule.isActive ? "bg-green-500" : "bg-gray-300"
                              } ${
                                activatingId === rule.ruleId
                                  ? "opacity-50 cursor-not-allowed"
                                  : "cursor-pointer"
                              }`}
                            >
                              <span
                                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                                  rule.isActive ? "translate-x-6" : "translate-x-1"
                                }`}
                              />
                            </button>
                          </div>
                        </div>
                      </div>

                      {/* 룰 이름과 Rule ID */}
                      <div className="mb-2">
                        <h3 className="text-lg font-bold text-gray-900 line-clamp-1">
                          {rule.name} ({rule.ruleId})
                        </h3>
                      </div>

                      {/* 설명 */}
                      {rule.description && (
                        <p className="text-sm text-gray-600 mb-4 line-clamp-1">
                          {cleanDescription(rule.description)}
                        </p>
                      )}

                      {/* whereJson 미리보기 */}
                      {rule.whereJson && (
                        <div>
                          <div className="bg-gray-100 rounded-lg p-3 border border-gray-200">
                            <pre className="text-xs text-gray-700 overflow-x-auto whitespace-pre">
                              {JSON.stringify(JSON.parse(rule.whereJson), null, 2)}
                            </pre>
                          </div>
                        </div>
                      )}
                    </div>
                ))}
              </div>
            ) : (
              /* 리스트 뷰 (기존) */
              <div className="grid gap-4">
                {filteredRules.map((rule) => (
                    <div
                      key={rule.ruleId}
                      className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 hover:shadow-lg transition-shadow duration-200"
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex items-start space-x-4">
                          <div className="flex-1">
                            <h3 className="text-lg font-semibold text-gray-900">
                              {rule.name} ({rule.ruleId})
                            </h3>
                            {rule.description && (
                              <p className="mt-2 text-sm text-gray-600">
                                {cleanDescription(rule.description)}
                              </p>
                            )}
                          </div>
                        </div>
                        <div className="flex items-center space-x-3 ml-4">
                          {/* 활성화 토글 스위치 */}
                          <div className="flex items-center space-x-2">
                            <span
                              className={`text-xs font-medium ${rule.isActive
                                ? "text-green-600"
                                : "text-gray-500"
                                }`}
                            >
                              {rule.isActive ? "활성" : "비활성"}
                            </span>
                            <button
                              onClick={() => handleToggleActive(rule)}
                              disabled={activatingId === rule.ruleId}
                              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 ${rule.isActive
                                ? "bg-green-500"
                                : "bg-gray-300"
                                } ${activatingId === rule.ruleId
                                  ? "opacity-50 cursor-not-allowed"
                                  : "cursor-pointer"
                                }`}
                              title={
                                rule.isActive ? "비활성화" : "활성화"
                              }
                            >
                              <span
                                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${rule.isActive
                                  ? "translate-x-6"
                                  : "translate-x-1"
                                  }`}
                              />
                            </button>
                          </div>

                          <button
                            onClick={() => handleEdit(rule)}
                            className="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
                            title="수정"
                          >
                            <PencilIcon className="h-5 w-5" />
                          </button>
                        </div>
                      </div>
                    </div>
                ))}
              </div>
            )}
        </div>
      )}

      {/* 모달 */}
      <Modal
        open={showForm}
        onClose={() => {
          setShowForm(false);
          setEditingRule(null);
          setEditingRuleId(null);
          clearError();
        }}
        title={editingRule ? "룰 수정" : "새 룰 등록"}
        size="2xl"
        tall
      >
        {isLoadingRuleDetail && editingRuleId ? (
          <div className="py-8 text-center text-gray-500">
            룰 정보를 불러오는 중...
          </div>
        ) : editingRuleId && !editingRule ? (
          <div className="py-8">
            <div className="mt-4 text-center">
              <LoadingButton
                onClick={() => {
                  setShowForm(false);
                  setEditingRuleId(null);
                  setEditingRule(null);
                }}
                type="button"
                variant="secondary"
              >
                닫기
              </LoadingButton>
            </div>
          </div>
        ) : (
          <RuleForm
            onSubmit={handleSubmit}
            defaultValues={
              editingRule
                ? {
                  ruleId: editingRule.ruleId,
                  name: editingRule.name,
                  description: editingRule.description || "",
                  operator: editingRule.operator || "",
                  entityType: editingRule.entityType || "",
                  evaluationMode: editingRule.evaluationMode || "WINDOW",
                  predicateSensorId: editingRule.predicateSensorId || "",
                  windowMinutes: editingRule.windowMinutes,
                  thresholdCount: editingRule.thresholdCount,
                  thresholdAmount: editingRule.thresholdAmount,
                  dedupMinutes: editingRule.dedupMinutes,
                  groupByFields: editingRule.groupByFields || [],
                  aggregationField: editingRule.aggregationField || "",
                  prevSensorId: editingRule.prevSensorId || "",
                  nextSensorId: editingRule.nextSensorId || "",
                  anchorSensorId: editingRule.anchorSensorId || "",
                  whereJson: editingRule.whereJson || "",
                }
                : {}
            }
            isEdit={!!editingRule}
            isPending={createMutation.isPending || updateMutation.isPending}
            error={formError?.message}
          />
        )}
      </Modal>
    </div>
  );
}

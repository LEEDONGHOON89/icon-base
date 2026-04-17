"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  PlusIcon,
  PencilIcon,
  TrashIcon,
  MagnifyingGlassIcon,
  ArrowsRightLeftIcon,
  CheckCircleIcon,
  XCircleIcon,
} from "@heroicons/react/24/outline";
import {
  fetchRelationRules,
  createRelationRule,
  updateRelationRule,
  deleteRelationRule,
  activateRelationRule,
  deactivateRelationRule,
  RelationRule,
  CreateRelationRuleRequest,
  UpdateRelationRuleRequest,
} from "./api";
import { fetchDataSources } from "../data-sources/api";
import RelationRuleForm, { RelationRuleFormValues } from "./RelationRuleForm";
import Alert from "@/components/common/Alert";
import { useErrorHandling } from "@/hooks/useErrorHandling";
import Modal from "../users/Modal";
import LoadingButton from "@/components/common/LoadingButton";
import toast from "react-hot-toast";

export default function RelationRulesPage() {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState("");
  const [filterDataSource, setFilterDataSource] = useState("");
  const [editingRule, setEditingRule] = useState<RelationRule | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [togglingId, setTogglingId] = useState<number | null>(null);
  const { error: formError, handleError, clearError } = useErrorHandling();

  // 규칙 목록 조회
  const {
    data: rules,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["relation-rules", filterDataSource],
    queryFn: () => fetchRelationRules(filterDataSource || undefined),
  });

  // 데이터소스 목록 조회
  const { data: dataSources = [] } = useQuery({
    queryKey: ["dataSources"],
    queryFn: fetchDataSources,
  });

  // 검색 필터링
  const filteredRules = (rules || []).filter((rule) => {
    const searchLower = searchTerm.toLowerCase();
    return (
      rule.fromEntityType.toLowerCase().includes(searchLower) ||
      rule.toEntityType.toLowerCase().includes(searchLower) ||
      rule.relationType.toLowerCase().includes(searchLower) ||
      (rule.description && rule.description.toLowerCase().includes(searchLower))
    );
  });

  // 생성
  const createMutation = useMutation({
    mutationFn: (data: CreateRelationRuleRequest) => createRelationRule(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["relation-rules"] });
      setShowForm(false);
      toast.success("엔티티 관계 규칙이 생성되었습니다.");
    },
    onError: handleError,
  });

  // 수정
  const updateMutation = useMutation({
    mutationFn: ({
      ruleId,
      data,
    }: {
      ruleId: number;
      data: UpdateRelationRuleRequest;
    }) => updateRelationRule(ruleId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["relation-rules"] });
      setEditingRule(null);
      setShowForm(false);
      toast.success("엔티티 관계 규칙이 수정되었습니다.");
    },
    onError: handleError,
  });

  // 삭제
  const deleteMutation = useMutation({
    mutationFn: deleteRelationRule,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["relation-rules"] });
      toast.success("엔티티 관계 규칙이 삭제되었습니다.");
      setDeletingId(null);
    },
    onError: (error) => {
      handleError(error);
      setDeletingId(null);
    },
  });

  // 활성화/비활성화
  const toggleActiveMutation = useMutation({
    mutationFn: ({ ruleId, isActive }: { ruleId: number; isActive: boolean }) =>
      isActive ? deactivateRelationRule(ruleId) : activateRelationRule(ruleId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["relation-rules"] });
      toast.success("상태가 변경되었습니다.");
      setTogglingId(null);
    },
    onError: (error) => {
      handleError(error);
      setTogglingId(null);
    },
  });

  // 폼 제출 핸들러
  const handleSubmit = (values: RelationRuleFormValues) => {
    clearError();
    if (editingRule) {
      updateMutation.mutate({
        ruleId: editingRule.ruleId,
        data: {
          ...values,
          isActive: editingRule.isActive,
        } as UpdateRelationRuleRequest,
      });
    } else {
      createMutation.mutate(values as CreateRelationRuleRequest);
    }
  };

  // 수정 버튼 클릭
  const handleEdit = (rule: RelationRule) => {
    setEditingRule(rule);
    setShowForm(true);
    clearError();
  };

  // 새 규칙 등록 버튼 클릭
  const handleNew = () => {
    setEditingRule(null);
    setShowForm(true);
    clearError();
  };

  // 삭제 버튼 클릭
  const handleDelete = (rule: RelationRule) => {
    if (
      confirm(
        `"${rule.fromEntityType} -${rule.relationType}-> ${rule.toEntityType}" 규칙을 삭제하시겠습니까?`
      )
    ) {
      setDeletingId(rule.ruleId);
      deleteMutation.mutate(rule.ruleId);
    }
  };

  // 활성화/비활성화 토글
  const handleToggleActive = (rule: RelationRule) => {
    setTogglingId(rule.ruleId);
    toggleActiveMutation.mutate({
      ruleId: rule.ruleId,
      isActive: rule.isActive,
    });
  };

  // 고유 데이터소스 목록 (ID와 이름 매핑)
  const uniqueDataSourceIds = Array.from(
    new Set((rules || []).map((r) => r.dataSourceId))
  ).sort();

  const dataSourceMap = new Map(
    dataSources.map((ds) => [ds.dataSourceId, ds.name])
  );

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
          <h1 className="text-2xl font-bold text-gray-900">
            엔티티 관계 설정
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            엔티티 간 관계를 정의하고 관리합니다.
          </p>
        </div>
        <LoadingButton
          onClick={handleNew}
          type="button"
          size="small"
          className="inline-flex items-center gap-2"
        >
          <PlusIcon className="h-5 w-5" />
          새 관계 규칙 등록
        </LoadingButton>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
          <div className="flex items-center gap-3">
            {/* Search */}
            <div className="relative flex-1 max-w-md">
              <MagnifyingGlassIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
              <input
                type="text"
                placeholder="엔티티 타입, 관계 타입, 설명으로 검색..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            {/* DataSource Filter */}
            <select
              value={filterDataSource}
              onChange={(e) => setFilterDataSource(e.target.value)}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="">전체 데이터소스</option>
              {uniqueDataSourceIds.map((dsId) => {
                const dsName = dataSourceMap.get(dsId);
                return (
                  <option key={dsId} value={dsId}>
                    {dsName ? `${dsName} (${dsId})` : dsId}
                  </option>
                );
              })}
            </select>
          </div>
      </div>

      {/* 에러 표시 */}
      {error && (
        <Alert message="규칙 목록을 불러오지 못했습니다." />
      )}

      {/* Rule List */}
      {!isLoading && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            {filteredRules.length === 0 ? (
              <div className="text-center py-12">
                <ArrowsRightLeftIcon className="mx-auto h-12 w-12 text-gray-400" />
                <h3 className="mt-2 text-sm font-medium text-gray-900">
                  엔티티 관계 규칙이 없습니다
                </h3>
                <p className="mt-1 text-sm text-gray-500">
                  {searchTerm || filterDataSource
                    ? "검색 결과가 없습니다."
                    : "새 관계 규칙을 생성하여 시작하세요."}
                </p>
              </div>
            ) : (
              <div className="grid gap-4">
                {filteredRules.map((rule) => {
                  return (
                    <div
                      key={rule.ruleId}
                      className={`bg-white rounded-xl border p-6 hover:shadow-lg transition-shadow duration-200 ${
                        rule.isActive
                          ? "border-gray-200"
                          : "border-gray-300 bg-gray-50 opacity-75"
                      }`}
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          {/* 관계 표시 */}
                          <div className="flex items-center gap-3 mb-3">
                            <span className="inline-flex items-center px-3 py-1 rounded-lg bg-blue-100 text-blue-700 font-semibold">
                              {rule.fromEntityType}
                            </span>
                            <div className="flex items-center gap-2">
                              <div className="h-0.5 w-8 bg-gray-400"></div>
                              <span className="inline-flex items-center px-2 py-0.5 rounded bg-purple-100 text-purple-700 text-sm font-medium">
                                {rule.relationType}
                              </span>
                              <div className="h-0.5 w-8 bg-gray-400"></div>
                              <ArrowsRightLeftIcon className="h-4 w-4 text-gray-400" />
                            </div>
                            <span className="inline-flex items-center px-3 py-1 rounded-lg bg-green-100 text-green-700 font-semibold">
                              {rule.toEntityType}
                            </span>
                          </div>

                          {/* 필드 정보 */}
                          <div className="text-sm text-gray-600 space-y-1 mb-3">
                            <div>
                              <span className="font-medium">From 필드:</span>{" "}
                              {rule.fromIdField}
                            </div>
                            <div>
                              <span className="font-medium">To 필드:</span>{" "}
                              {rule.toIdField}
                            </div>
                            <div>
                              <span className="font-medium">데이터소스:</span>{" "}
                              {rule.dataSourceId}
                            </div>
                          </div>

                          {/* 설명 */}
                          {rule.description && (
                            <p className="text-sm text-gray-600 mb-3">
                              {rule.description}
                            </p>
                          )}

                          {/* 상태 및 날짜 */}
                          <div className="flex items-center gap-4 text-xs text-gray-500">
                            <button
                              onClick={() => handleToggleActive(rule)}
                              disabled={togglingId === rule.ruleId}
                              className={`inline-flex items-center gap-1 px-2.5 py-1.5 rounded-full font-medium ${
                                rule.isActive
                                  ? "bg-green-100 text-green-800 hover:bg-green-200"
                                  : "bg-gray-100 text-gray-800 hover:bg-gray-200"
                              } transition-colors disabled:opacity-50`}
                            >
                              {togglingId === rule.ruleId ? (
                                <div className="h-3 w-3 animate-spin rounded-full border-2 border-current border-t-transparent"></div>
                              ) : rule.isActive ? (
                                <>
                                  <CheckCircleIcon className="h-4 w-4" />
                                  활성
                                </>
                              ) : (
                                <>
                                  <XCircleIcon className="h-4 w-4" />
                                  비활성
                                </>
                              )}
                            </button>
                            <span>
                              생성일:{" "}
                              {new Date(rule.createdAt).toLocaleDateString()}
                            </span>
                          </div>
                        </div>

                        {/* 액션 버튼 */}
                        <div className="flex items-center space-x-2 ml-4">
                          {/* 수정 */}
                          <button
                            onClick={() => handleEdit(rule)}
                            className="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
                            title="수정"
                          >
                            <PencilIcon className="h-5 w-5" />
                          </button>

                          {/* 삭제 */}
                          <button
                            onClick={() => handleDelete(rule)}
                            disabled={deletingId === rule.ruleId}
                            className={`p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors ${
                              deletingId === rule.ruleId
                                ? "opacity-50 cursor-not-allowed"
                                : ""
                            }`}
                            title="삭제"
                          >
                            {deletingId === rule.ruleId ? (
                              <div className="h-5 w-5 animate-spin rounded-full border-2 border-gray-600 border-t-transparent"></div>
                            ) : (
                              <TrashIcon className="h-5 w-5" />
                            )}
                          </button>
                        </div>
                      </div>
                    </div>
                  );
                })}
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
          clearError();
        }}
        title={editingRule ? "엔티티 관계 수정" : "새 엔티티 관계 등록"}
      >
        <RelationRuleForm
          onSubmit={handleSubmit}
          defaultValues={
            editingRule
              ? {
                  dataSourceId: editingRule.dataSourceId,
                  fromEntityType: editingRule.fromEntityType,
                  fromIdField: editingRule.fromIdField,
                  relationType: editingRule.relationType,
                  toEntityType: editingRule.toEntityType,
                  toIdField: editingRule.toIdField,
                  description: editingRule.description,
                }
              : {}
          }
          isEdit={!!editingRule}
          isPending={createMutation.isPending || updateMutation.isPending}
          error={formError?.message}
        />
      </Modal>
    </div>
  );
}

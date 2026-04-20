"use client";

// [2026-04-20] 표준 필드 추가/수정/삭제 기능 추가 (읽기전용 → CRUD)
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  fetchStandardFields,
  createStandardField,
  updateStandardField,
  deleteStandardField,
} from "../api";
import type { StandardField, CreateStandardFieldRequest, UpdateStandardFieldRequest } from "../api";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "react-hot-toast";
import { MagnifyingGlassIcon, PlusIcon, PencilIcon, TrashIcon } from "@heroicons/react/24/outline";
import { Modal } from "@/components/ui/Modal";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";

const DATA_TYPES = [
  "STRING", "NUMBER", "DECIMAL", "BOOLEAN",
  "DATE", "DATETIME", "TIME", "JSON", "ARRAY", "OBJECT",
];

const CATEGORIES = [
  "TRANSACTION", "CUSTOMER", "ACCOUNT", "AUTH", "SECURITY",
  "DATETIME", "TEMPORAL", "SYSTEM", "IDENTIFIER", "FRAUD",
  "ECOMMERCE", "RISK", "NETWORK", "ACTIVITY", "LOCATION",
  "ACCESS", "DEVICE", "CERTIFICATE", "LOAN", "OPEN_BANKING",
  "ATM", "BLACKLIST", "TRANSFER_APPROVAL", "TRANSFER_LIMIT", "EMPLOYEE",
];

const getDataTypeColor = (dataType: string) => {
  const colorMap: Record<string, string> = {
    STRING: "bg-blue-100 text-blue-700",
    NUMBER: "bg-green-100 text-green-700",
    DECIMAL: "bg-teal-100 text-teal-700",
    BOOLEAN: "bg-purple-100 text-purple-700",
    DATE: "bg-orange-100 text-orange-700",
    DATETIME: "bg-pink-100 text-pink-700",
    TIME: "bg-cyan-100 text-cyan-700",
    JSON: "bg-indigo-100 text-indigo-700",
    ARRAY: "bg-yellow-100 text-yellow-700",
    OBJECT: "bg-red-100 text-red-700",
  };
  return colorMap[dataType] || "bg-gray-100 text-gray-700";
};

type FormMode = "create" | "edit";

interface FieldFormValues {
  fieldName: string;
  displayName: string;
  dataType: string;
  category: string;
  description: string;
  isActive: boolean;
}

export default function StandardFieldsPage() {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState("");
  const [formMode, setFormMode] = useState<FormMode>("create");
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingField, setEditingField] = useState<StandardField | null>(null);
  const [deletingField, setDeletingField] = useState<StandardField | null>(null);

  const { data: standardFields = [], isLoading } = useQuery({
    queryKey: ["standardFields"],
    queryFn: fetchStandardFields,
  });

  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } =
    useForm<FieldFormValues>();

  const sortedFields = [...standardFields]
    .filter((f) => f && f.fieldId)
    .filter((f) => {
      if (!searchTerm) return true;
      const s = searchTerm.toLowerCase();
      return (
        f.fieldId.toLowerCase().includes(s) ||
        f.displayName.toLowerCase().includes(s)
      );
    })
    .sort((a, b) => a.fieldId.localeCompare(b.fieldId));

  const createMutation = useMutation({
    mutationFn: (data: CreateStandardFieldRequest) => createStandardField(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["standardFields"] });
      toast.success("표준 필드가 생성되었습니다");
      setIsFormOpen(false);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || "생성에 실패했습니다");
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ fieldId, data }: { fieldId: string; data: UpdateStandardFieldRequest }) =>
      updateStandardField(fieldId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["standardFields"] });
      toast.success("표준 필드가 수정되었습니다");
      setIsFormOpen(false);
      setEditingField(null);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || "수정에 실패했습니다");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (fieldId: string) => deleteStandardField(fieldId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["standardFields"] });
      toast.success("표준 필드가 삭제되었습니다");
      setDeletingField(null);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || "삭제에 실패했습니다");
    },
  });

  const openCreate = () => {
    setFormMode("create");
    setEditingField(null);
    reset({
      fieldName: "",
      displayName: "",
      dataType: "STRING",
      category: "",
      description: "",
      isActive: true,
    });
    setIsFormOpen(true);
  };

  const openEdit = (field: StandardField) => {
    setFormMode("edit");
    setEditingField(field);
    reset({
      fieldName: field.fieldId,
      displayName: field.displayName,
      dataType: field.dataType,
      category: field.category ?? "",
      description: field.description ?? "",
      isActive: field.active,
    });
    setIsFormOpen(true);
  };

  const onSubmit = async (values: FieldFormValues) => {
    if (formMode === "create") {
      await createMutation.mutateAsync({
        fieldName: values.fieldName,
        displayName: values.displayName,
        dataType: values.dataType,
        category: values.category || undefined,
        description: values.description || undefined,
      });
    } else if (editingField) {
      await updateMutation.mutateAsync({
        fieldId: editingField.fieldId,
        data: {
          displayName: values.displayName,
          dataType: values.dataType,
          category: values.category || undefined,
          description: values.description || undefined,
          isActive: values.isActive,
        },
      });
    }
  };

  if (isLoading) {
    return (
      <div className="p-6 space-y-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 flex justify-center items-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600" />
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">표준 필드</h1>
          <p className="text-sm text-gray-500 mt-1">
            시스템에서 사용하는 표준 필드를 관리합니다. (총 {sortedFields.length}개)
          </p>
        </div>
        <button
          onClick={openCreate}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors"
        >
          <PlusIcon className="h-4 w-4" />
          필드 추가
        </button>
      </div>

      {/* 검색창 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
        <div className="relative">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <MagnifyingGlassIcon className="h-5 w-5 text-gray-400" />
          </div>
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="필드 ID 또는 표시명으로 검색..."
            className="block w-full pl-10 pr-10 py-2 border border-gray-300 rounded-lg bg-white text-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm("")}
              className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600"
            >
              <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          )}
        </div>
      </div>

      {/* 테이블 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="px-4 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider w-40">필드 ID</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider w-56">표시명</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider w-32">데이터 타입</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider w-32">카테고리</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider w-24">활성</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">설명</th>
                <th className="px-6 py-4 text-right text-xs font-semibold text-gray-600 uppercase tracking-wider w-24">작업</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {sortedFields.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-gray-500">
                    표준 필드가 없습니다.
                  </td>
                </tr>
              ) : (
                sortedFields.map((field) => (
                  <tr key={field.fieldId} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-4 text-sm font-mono text-gray-900 w-40">{field.fieldId}</td>
                    <td className="px-6 py-4 text-sm text-gray-900 w-56">{field.displayName}</td>
                    <td className="px-6 py-4 text-sm w-32">
                      <span className={`px-3 py-1.5 rounded text-xs font-semibold whitespace-nowrap ${getDataTypeColor(field.dataType)}`}>
                        {field.dataType}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-sm w-32">
                      {field.category ? (
                        <span className="px-3 py-1.5 bg-purple-100 text-purple-700 rounded text-xs font-semibold whitespace-nowrap inline-block">
                          {field.category}
                        </span>
                      ) : (
                        <span className="text-gray-400">-</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-sm w-24">
                      {field.active ? (
                        <span className="px-3 py-1.5 bg-green-100 text-green-700 rounded text-xs font-semibold whitespace-nowrap inline-block">활성</span>
                      ) : (
                        <span className="px-3 py-1.5 bg-gray-100 text-gray-600 rounded text-xs font-semibold whitespace-nowrap inline-block">비활성</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600">{field.description || "-"}</td>
                    <td className="px-6 py-4 text-right w-24">
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => openEdit(field)}
                          className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded transition-colors"
                          title="수정"
                        >
                          <PencilIcon className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => setDeletingField(field)}
                          className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
                          title="삭제"
                        >
                          <TrashIcon className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* 생성/수정 모달 */}
      <Modal
        isOpen={isFormOpen}
        onClose={() => { setIsFormOpen(false); setEditingField(null); }}
        title={formMode === "create" ? "표준 필드 추가" : "표준 필드 수정"}
        size="lg"
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          {/* 필드 ID (생성 시만) */}
          {formMode === "create" && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">필드 ID *</label>
              <input
                {...register("fieldName", {
                  required: "필드 ID는 필수입니다",
                  pattern: {
                    value: /^[a-zA-Z][a-zA-Z0-9_]*$/,
                    message: "영문자로 시작하고 영문자, 숫자, 언더스코어만 허용됩니다",
                  },
                })}
                type="text"
                placeholder="예: transaction_amount"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
              />
              {errors.fieldName && <p className="mt-1 text-xs text-red-600">{errors.fieldName.message}</p>}
              <p className="mt-1 text-xs text-gray-500">한번 생성하면 변경할 수 없습니다</p>
            </div>
          )}

          {/* 표시명 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">표시명 *</label>
            <input
              {...register("displayName", { required: "표시명은 필수입니다" })}
              type="text"
              placeholder="예: 거래 금액"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
            />
            {errors.displayName && <p className="mt-1 text-xs text-red-600">{errors.displayName.message}</p>}
          </div>

          <div className="grid grid-cols-2 gap-4">
            {/* 데이터 타입 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">데이터 타입 *</label>
              <select
                {...register("dataType", { required: "데이터 타입은 필수입니다" })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
              >
                {DATA_TYPES.map((t) => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
              {errors.dataType && <p className="mt-1 text-xs text-red-600">{errors.dataType.message}</p>}
            </div>

            {/* 카테고리 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">카테고리</label>
              <select
                {...register("category")}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
              >
                <option value="">선택 안함</option>
                {CATEGORIES.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>
          </div>

          {/* 설명 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">설명</label>
            <textarea
              {...register("description")}
              placeholder="필드에 대한 설명을 입력하세요"
              rows={3}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm resize-none"
            />
          </div>

          {/* 활성 상태 (수정 시만) */}
          {formMode === "edit" && (
            <div className="flex items-center gap-3">
              <input
                {...register("isActive")}
                type="checkbox"
                id="isActive"
                className="h-4 w-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
              />
              <label htmlFor="isActive" className="text-sm font-medium text-gray-700">활성</label>
            </div>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => { setIsFormOpen(false); setEditingField(null); }}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              {isSubmitting ? "저장 중..." : formMode === "create" ? "생성" : "수정"}
            </button>
          </div>
        </form>
      </Modal>

      {/* 삭제 확인 다이얼로그 */}
      {deletingField && (
        <ConfirmDialog
          isOpen={!!deletingField}
          title="표준 필드 삭제"
          message={`"${deletingField.displayName}" (${deletingField.fieldId}) 필드를 삭제하시겠습니까? 이 필드를 참조하는 센서 조건이 있을 경우 오류가 발생할 수 있습니다.`}
          confirmText="삭제"
          confirmColor="red"
          onConfirm={() => deleteMutation.mutate(deletingField.fieldId)}
          onClose={() => setDeletingField(null)}
        />
      )}
    </div>
  );
}

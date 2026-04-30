// [2026-04-24] 엔티티 필드 관리 화면 — 조회 전용에서 등록/수정/삭제 CRUD 관리 화면으로 확장
"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  fetchAllEntityFields,
  createEntityField,
  updateEntityField,
  deleteEntityField,
  DATA_TYPES,
  type EntityField,
  type CreateEntityFieldRequest,
  type UpdateEntityFieldRequest,
} from "@/app/entity-fields/api";
import toast from "react-hot-toast";
import {
  MagnifyingGlassIcon,
  PlusIcon,
  PencilSquareIcon,
  TrashIcon,
  XMarkIcon,
  CheckIcon,
} from "@heroicons/react/24/outline";

// 데이터 타입별 색상 매핑
const getDataTypeColor = (dataType: string) => {
  const colorMap: Record<string, string> = {
    STRING:    "bg-blue-100 text-blue-700",
    NUMBER:    "bg-green-100 text-green-700",
    BOOLEAN:   "bg-purple-100 text-purple-700",
    DATE:      "bg-orange-100 text-orange-700",
    DATETIME:  "bg-pink-100 text-pink-700",
    TIMESTAMP: "bg-indigo-100 text-indigo-700",
    ARRAY:     "bg-yellow-100 text-yellow-700",
    OBJECT:    "bg-red-100 text-red-700",
  };
  return colorMap[dataType] || "bg-gray-100 text-gray-700";
};

// 초기 폼 상태
const EMPTY_CREATE: CreateEntityFieldRequest = {
  entityFieldId: "",
  displayName: "",
  dataType: "STRING",
  description: "",
};

export default function EntityFieldsPage() {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState("");

  // 모달 상태
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editTarget, setEditTarget] = useState<EntityField | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<EntityField | null>(null);

  // 폼 상태
  const [createForm, setCreateForm] = useState<CreateEntityFieldRequest>(EMPTY_CREATE);
  const [editForm, setEditForm] = useState<UpdateEntityFieldRequest>({
    displayName: "", dataType: "STRING", description: "", isActive: true,
  });

  // ── 데이터 조회 ────────────────────────────────────────────
  const { data: entityFields = [], isLoading } = useQuery({
    queryKey: ["entityFields", "all"],
    queryFn: fetchAllEntityFields,
  });

  const filtered = [...entityFields]
    .filter((f) => {
      if (!searchTerm) return true;
      const s = searchTerm.toLowerCase();
      return (
        f.entityFieldId.toLowerCase().includes(s) ||
        f.displayName.toLowerCase().includes(s) ||
        (f.description || "").toLowerCase().includes(s)
      );
    })
    .sort((a, b) => a.entityFieldId.localeCompare(b.entityFieldId));

  // ── Mutations ─────────────────────────────────────────────
  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["entityFields"] });
  };

  const createMutation = useMutation({
    mutationFn: createEntityField,
    onSuccess: () => {
      toast.success("엔티티 필드가 등록되었습니다.");
      setShowCreateModal(false);
      setCreateForm(EMPTY_CREATE);
      invalidate();
    },
    onError: (e: any) => toast.error(e?.response?.data?.message || "등록에 실패했습니다."),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateEntityFieldRequest }) =>
      updateEntityField(id, data),
    onSuccess: () => {
      toast.success("엔티티 필드가 수정되었습니다.");
      setEditTarget(null);
      invalidate();
    },
    onError: (e: any) => toast.error(e?.response?.data?.message || "수정에 실패했습니다."),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteEntityField,
    onSuccess: () => {
      toast.success("엔티티 필드가 삭제되었습니다.");
      setDeleteTarget(null);
      invalidate();
    },
    onError: (e: any) => toast.error(e?.response?.data?.message || "삭제에 실패했습니다."),
  });

  // ── 핸들러 ────────────────────────────────────────────────
  const handleEditOpen = (field: EntityField) => {
    setEditTarget(field);
    setEditForm({
      displayName: field.displayName,
      dataType: field.dataType,
      description: field.description || "",
      isActive: field.isActive,
    });
  };

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!createForm.entityFieldId.trim()) { toast.error("필드 ID를 입력하세요."); return; }
    if (!createForm.displayName.trim()) { toast.error("표시명을 입력하세요."); return; }
    createMutation.mutate(createForm);
  };

  const handleEditSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editTarget) return;
    if (!editForm.displayName.trim()) { toast.error("표시명을 입력하세요."); return; }
    updateMutation.mutate({ id: editTarget.entityFieldId, data: editForm });
  };

  // ── 공통 폼 필드 렌더러 ───────────────────────────────────
  const renderDataTypeSelect = (
    value: string,
    onChange: (v: string) => void,
    id: string
  ) => (
    <select
      id={id}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white"
      required
    >
      {DATA_TYPES.map((t) => (
        <option key={t.value} value={t.value}>{t.label}</option>
      ))}
    </select>
  );

  return (
    <div className="p-6 space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">엔티티 필드 관리</h1>
          <p className="text-sm text-gray-500 mt-1">
            시나리오 엔티티 필터에서 사용할 필드를 관리합니다. (총 {filtered.length}개)
          </p>
        </div>
        <button
          onClick={() => { setCreateForm(EMPTY_CREATE); setShowCreateModal(true); }}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-xl text-sm font-medium hover:bg-blue-700 transition-colors shadow-sm"
        >
          <PlusIcon className="h-4 w-4" />
          필드 추가
        </button>
      </div>

      {/* 검색 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
        <div className="relative">
          <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="필드 ID, 표시명, 설명으로 검색..."
            className="block w-full pl-10 pr-10 py-2 border border-gray-300 rounded-lg bg-white text-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm("")}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            >
              <XMarkIcon className="h-5 w-5" />
            </button>
          )}
        </div>
      </div>

      {/* 테이블 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        {isLoading ? (
          <div className="flex justify-center items-center h-48">
            <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600" />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider w-44">필드 ID</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider w-36">표시명</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider w-32">데이터 타입</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider w-20">활성</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">설명</th>
                  <th className="px-4 py-3 text-center text-xs font-semibold text-gray-600 uppercase tracking-wider w-24">관리</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filtered.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-6 py-12 text-center text-gray-500 text-sm">
                      {searchTerm ? "검색 결과가 없습니다." : "등록된 엔티티 필드가 없습니다."}
                    </td>
                  </tr>
                ) : (
                  filtered.map((field) => (
                    <tr key={field.entityFieldId} className={`hover:bg-gray-50 transition-colors ${!field.isActive ? "opacity-50" : ""}`}>
                      <td className="px-4 py-3 text-sm font-mono text-gray-900">{field.entityFieldId}</td>
                      <td className="px-4 py-3 text-sm text-gray-900">{field.displayName}</td>
                      <td className="px-4 py-3 text-sm">
                        <span className={`px-2 py-1 rounded text-xs font-semibold ${getDataTypeColor(field.dataType)}`}>
                          {field.dataType}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm">
                        {field.isActive ? (
                          <span className="px-2 py-1 bg-green-100 text-green-700 rounded text-xs font-semibold">활성</span>
                        ) : (
                          <span className="px-2 py-1 bg-gray-100 text-gray-500 rounded text-xs font-semibold">비활성</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-500">{field.description || "-"}</td>
                      <td className="px-4 py-3 text-center">
                        <div className="flex items-center justify-center gap-2">
                          <button
                            onClick={() => handleEditOpen(field)}
                            className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                            title="수정"
                          >
                            <PencilSquareIcon className="h-4 w-4" />
                          </button>
                          <button
                            onClick={() => setDeleteTarget(field)}
                            className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
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
        )}
      </div>

      {/* ── 등록 모달 ─────────────────────────────────────────── */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md mx-4">
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
              <h2 className="text-lg font-semibold text-gray-900">엔티티 필드 추가</h2>
              <button onClick={() => setShowCreateModal(false)} className="text-gray-400 hover:text-gray-600">
                <XMarkIcon className="h-5 w-5" />
              </button>
            </div>
            <form onSubmit={handleCreateSubmit} className="px-6 py-5 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1" htmlFor="c-id">
                  필드 ID <span className="text-red-500">*</span>
                </label>
                <input
                  id="c-id"
                  type="text"
                  value={createForm.entityFieldId}
                  onChange={(e) => setCreateForm({ ...createForm, entityFieldId: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm font-mono focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="예: customer_age"
                  required
                />
                <p className="text-xs text-gray-400 mt-1">소문자, 숫자, 언더스코어만 허용</p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1" htmlFor="c-name">
                  표시명 <span className="text-red-500">*</span>
                </label>
                <input
                  id="c-name"
                  type="text"
                  value={createForm.displayName}
                  onChange={(e) => setCreateForm({ ...createForm, displayName: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="예: 고객 나이"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1" htmlFor="c-type">
                  데이터 타입 <span className="text-red-500">*</span>
                </label>
                {renderDataTypeSelect(createForm.dataType, (v) => setCreateForm({ ...createForm, dataType: v }), "c-type")}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1" htmlFor="c-desc">
                  설명 (선택)
                </label>
                <input
                  id="c-desc"
                  type="text"
                  value={createForm.description || ""}
                  onChange={(e) => setCreateForm({ ...createForm, description: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="필드에 대한 설명"
                />
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
                >
                  취소
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="flex items-center gap-1.5 px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
                >
                  <CheckIcon className="h-4 w-4" />
                  {createMutation.isPending ? "저장 중..." : "저장"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── 수정 모달 ─────────────────────────────────────────── */}
      {editTarget && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md mx-4">
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
              <div>
                <h2 className="text-lg font-semibold text-gray-900">엔티티 필드 수정</h2>
                <p className="text-xs text-gray-400 font-mono mt-0.5">{editTarget.entityFieldId}</p>
              </div>
              <button onClick={() => setEditTarget(null)} className="text-gray-400 hover:text-gray-600">
                <XMarkIcon className="h-5 w-5" />
              </button>
            </div>
            <form onSubmit={handleEditSubmit} className="px-6 py-5 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1" htmlFor="e-name">
                  표시명 <span className="text-red-500">*</span>
                </label>
                <input
                  id="e-name"
                  type="text"
                  value={editForm.displayName}
                  onChange={(e) => setEditForm({ ...editForm, displayName: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1" htmlFor="e-type">
                  데이터 타입 <span className="text-red-500">*</span>
                </label>
                {renderDataTypeSelect(editForm.dataType, (v) => setEditForm({ ...editForm, dataType: v }), "e-type")}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1" htmlFor="e-desc">
                  설명 (선택)
                </label>
                <input
                  id="e-desc"
                  type="text"
                  value={editForm.description || ""}
                  onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
              <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div>
                  <p className="text-sm font-medium text-gray-700">활성화</p>
                  <p className="text-xs text-gray-400">비활성화 시 필터에서 사용 불가</p>
                </div>
                <button
                  type="button"
                  onClick={() => setEditForm({ ...editForm, isActive: !editForm.isActive })}
                  className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${editForm.isActive ? "bg-blue-600" : "bg-gray-300"}`}
                >
                  <span className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${editForm.isActive ? "translate-x-6" : "translate-x-1"}`} />
                </button>
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setEditTarget(null)}
                  className="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
                >
                  취소
                </button>
                <button
                  type="submit"
                  disabled={updateMutation.isPending}
                  className="flex items-center gap-1.5 px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
                >
                  <CheckIcon className="h-4 w-4" />
                  {updateMutation.isPending ? "저장 중..." : "저장"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── 삭제 확인 모달 ────────────────────────────────────── */}
      {deleteTarget && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm mx-4 p-6">
            <div className="flex items-start gap-3 mb-4">
              <div className="p-2 bg-red-100 rounded-xl flex-shrink-0">
                <TrashIcon className="h-5 w-5 text-red-600" />
              </div>
              <div>
                <h2 className="text-base font-semibold text-gray-900">엔티티 필드 삭제</h2>
                <p className="text-sm text-gray-500 mt-1">
                  <span className="font-mono font-medium text-gray-800">{deleteTarget.entityFieldId}</span> ({deleteTarget.displayName})을 삭제하시겠습니까?
                </p>
                <p className="text-xs text-red-500 mt-2">이 필드를 사용 중인 시나리오 필터에서 오류가 발생할 수 있습니다.</p>
              </div>
            </div>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setDeleteTarget(null)}
                className="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
              >
                취소
              </button>
              <button
                onClick={() => deleteMutation.mutate(deleteTarget.entityFieldId)}
                disabled={deleteMutation.isPending}
                className="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
              >
                {deleteMutation.isPending ? "삭제 중..." : "삭제"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

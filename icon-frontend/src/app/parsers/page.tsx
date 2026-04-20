"use client";

// [2026-04-20] 파서 관리 페이지 - 파서 CRUD (목록/생성/수정/삭제)
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  fetchParsers,
  createParser,
  updateParser,
  deleteParser,
  PARSER_TYPE_LABELS,
  DEFAULT_CONFIG_JSON,
} from "./api";
import type {
  ParserSummary,
  ParserDetail,
  ParserType,
  ParserRuleItem,
  CreateParserRequest,
  UpdateParserRequest,
} from "./api";
import { fetchParser } from "./api";
import { useState, useEffect } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { toast } from "react-hot-toast";
import {
  PlusIcon,
  PencilIcon,
  TrashIcon,
  MagnifyingGlassIcon,
  ScissorsIcon,
  ChevronDownIcon,
  ChevronUpIcon,
} from "@heroicons/react/24/outline";
import { Modal } from "@/components/ui/Modal";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";

// ─── 타입 ───────────────────────────────────────────────────────────────────
type FormMode = "create" | "edit";

interface RuleFormItem {
  ruleOrder: number;
  configJson: string;
  targetStandardFieldId: string;
  targetFieldName: string;
}

interface ParserFormValues {
  parserName: string;
  parserType: ParserType;
  description: string;
  isActive: boolean;
  rules: RuleFormItem[];
}

// ─── 파서 타입 배지 색상 ──────────────────────────────────────────────────────
const PARSER_TYPE_COLORS: Record<ParserType, string> = {
  DELIMITER: "bg-blue-100 text-blue-700",
  FIXED_WIDTH: "bg-green-100 text-green-700",
  REGEX: "bg-purple-100 text-purple-700",
};

// ─── 페이지 컴포넌트 ──────────────────────────────────────────────────────────
export default function ParsersPage() {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState("");
  const [formMode, setFormMode] = useState<FormMode>("create");
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingParser, setEditingParser] = useState<ParserSummary | null>(null);
  const [deletingParser, setDeletingParser] = useState<ParserSummary | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  // 파서 목록 조회
  const { data: parsers = [], isLoading } = useQuery({
    queryKey: ["parsers"],
    queryFn: fetchParsers,
  });

  // 파서 상세 조회 (편집 시)
  const { data: editDetail } = useQuery({
    queryKey: ["parser", editingParser?.parserId],
    queryFn: () => fetchParser(editingParser!.parserId),
    enabled: !!editingParser && formMode === "edit",
  });

  // 폼
  const {
    register,
    handleSubmit,
    reset,
    watch,
    control,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<ParserFormValues>({
    defaultValues: {
      parserName: "",
      parserType: "DELIMITER",
      description: "",
      isActive: true,
      rules: [{ ruleOrder: 0, configJson: DEFAULT_CONFIG_JSON.DELIMITER, targetStandardFieldId: "", targetFieldName: "" }],
    },
  });

  const { fields: ruleFields, append, remove } = useFieldArray({ control, name: "rules" });

  const watchedType = watch("parserType");

  // 파서 타입 변경 시 rules의 configJson 기본값 업데이트
  useEffect(() => {
    if (formMode === "create") {
      setValue("rules", [{ ruleOrder: 0, configJson: DEFAULT_CONFIG_JSON[watchedType], targetStandardFieldId: "", targetFieldName: "" }]);
    }
  }, [watchedType, formMode, setValue]);

  // 편집 상세 로드 시 폼 초기화
  useEffect(() => {
    if (formMode === "edit" && editDetail) {
      reset({
        parserName: editDetail.parserName,
        parserType: editDetail.parserType,
        description: editDetail.description ?? "",
        isActive: editDetail.isActive,
        rules: editDetail.rules.map((r) => ({
          ruleOrder: r.ruleOrder,
          configJson: r.configJson,
          targetStandardFieldId: r.targetStandardFieldId ?? "",
          targetFieldName: r.targetFieldName ?? "",
        })),
      });
    }
  }, [editDetail, formMode, reset]);

  // 생성
  const createMutation = useMutation({
    mutationFn: (data: CreateParserRequest) => createParser(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["parsers"] });
      toast.success("파서가 생성되었습니다.");
      setIsFormOpen(false);
    },
    onError: () => toast.error("파서 생성에 실패했습니다."),
  });

  // 수정
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateParserRequest }) =>
      updateParser(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["parsers"] });
      toast.success("파서가 수정되었습니다.");
      setIsFormOpen(false);
    },
    onError: () => toast.error("파서 수정에 실패했습니다."),
  });

  // 삭제
  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteParser(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["parsers"] });
      toast.success("파서가 삭제되었습니다.");
      setDeletingParser(null);
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || "파서 삭제에 실패했습니다.";
      toast.error(msg);
      setDeletingParser(null);
    },
  });

  // 검색 필터
  const filtered = parsers.filter((p) => {
    if (!searchTerm) return true;
    const s = searchTerm.toLowerCase();
    return (
      p.parserName.toLowerCase().includes(s) ||
      (p.description ?? "").toLowerCase().includes(s) ||
      PARSER_TYPE_LABELS[p.parserType].toLowerCase().includes(s)
    );
  });

  // 폼 열기 (생성)
  const openCreate = () => {
    setFormMode("create");
    reset({
      parserName: "",
      parserType: "DELIMITER",
      description: "",
      isActive: true,
      rules: [{ ruleOrder: 0, configJson: DEFAULT_CONFIG_JSON.DELIMITER, targetStandardFieldId: "", targetFieldName: "" }],
    });
    setEditingParser(null);
    setIsFormOpen(true);
  };

  // 폼 열기 (수정)
  const openEdit = (p: ParserSummary) => {
    setFormMode("edit");
    setEditingParser(p);
    setIsFormOpen(true);
  };

  // 규칙 추가
  const addRule = () => {
    append({
      ruleOrder: ruleFields.length,
      configJson: DEFAULT_CONFIG_JSON[watchedType],
      targetStandardFieldId: "",
      targetFieldName: "",
    });
  };

  // 폼 제출
  const onSubmit = (values: ParserFormValues) => {
    const rules: ParserRuleItem[] = values.rules.map((r, idx) => ({
      ruleOrder: idx,
      configJson: r.configJson,
      targetStandardFieldId: r.targetStandardFieldId || null,
      targetFieldName: r.targetFieldName || null,
    }));

    if (formMode === "create") {
      createMutation.mutate({
        parserName: values.parserName,
        parserType: values.parserType,
        description: values.description || undefined,
        rules,
      });
    } else if (editingParser) {
      updateMutation.mutate({
        id: editingParser.parserId,
        data: {
          parserName: values.parserName,
          description: values.description || undefined,
          isActive: values.isActive,
          rules,
        },
      });
    }
  };

  return (
    <div className="p-6 max-w-6xl mx-auto">
      {/* 헤더 */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-blue-100 rounded-lg">
            <ScissorsIcon className="h-6 w-6 text-blue-600" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">파서 관리</h1>
            <p className="text-sm text-gray-500 mt-0.5">
              텍스트 필드 값을 구분자·고정폭·정규식으로 분리하는 파서를 관리합니다.
            </p>
          </div>
        </div>
        <button
          onClick={openCreate}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm font-medium"
        >
          <PlusIcon className="h-4 w-4" />
          파서 추가
        </button>
      </div>

      {/* 검색 */}
      <div className="relative mb-4">
        <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
        <input
          type="text"
          placeholder="파서명, 설명, 타입으로 검색..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full pl-9 pr-4 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      {/* 통계 뱃지 */}
      <div className="flex gap-3 mb-4">
        {(["DELIMITER", "FIXED_WIDTH", "REGEX"] as ParserType[]).map((t) => {
          const cnt = parsers.filter((p) => p.parserType === t).length;
          return (
            <span key={t} className={`text-xs px-3 py-1 rounded-full font-medium ${PARSER_TYPE_COLORS[t]}`}>
              {PARSER_TYPE_LABELS[t]}: {cnt}
            </span>
          );
        })}
      </div>

      {/* 목록 */}
      {isLoading ? (
        <div className="text-center py-10 text-gray-400">로딩 중...</div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-10 text-gray-400">
          {searchTerm ? "검색 결과가 없습니다." : "등록된 파서가 없습니다."}
        </div>
      ) : (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="text-left px-4 py-3 font-semibold text-gray-600 w-10"></th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600">파서명</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600">타입</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-600">설명</th>
                <th className="text-center px-4 py-3 font-semibold text-gray-600">규칙 수</th>
                <th className="text-center px-4 py-3 font-semibold text-gray-600">상태</th>
                <th className="text-center px-4 py-3 font-semibold text-gray-600">작업</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((p) => (
                <>
                  <tr key={p.parserId} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                    {/* 펼치기 버튼 */}
                    <td className="px-4 py-3">
                      <button
                        onClick={() => setExpandedId(expandedId === p.parserId ? null : p.parserId)}
                        className="text-gray-400 hover:text-gray-600"
                      >
                        {expandedId === p.parserId
                          ? <ChevronUpIcon className="h-4 w-4" />
                          : <ChevronDownIcon className="h-4 w-4" />}
                      </button>
                    </td>
                    <td className="px-4 py-3 font-medium text-gray-900">{p.parserName}</td>
                    <td className="px-4 py-3">
                      <span className={`text-xs px-2 py-1 rounded-full font-medium ${PARSER_TYPE_COLORS[p.parserType]}`}>
                        {PARSER_TYPE_LABELS[p.parserType]}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-500 max-w-xs truncate">{p.description ?? "-"}</td>
                    <td className="px-4 py-3 text-center">
                      <span className="inline-flex items-center justify-center w-6 h-6 bg-gray-100 rounded-full text-xs font-semibold text-gray-700">
                        {p.ruleCount}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span className={`text-xs px-2 py-1 rounded-full font-medium ${p.isActive ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-500"}`}>
                        {p.isActive ? "활성" : "비활성"}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-center gap-2">
                        <button
                          onClick={() => openEdit(p)}
                          className="p-1.5 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                          title="수정"
                        >
                          <PencilIcon className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => setDeletingParser(p)}
                          className="p-1.5 text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                          title="삭제"
                        >
                          <TrashIcon className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                  {/* 상세 규칙 펼침 행 */}
                  {expandedId === p.parserId && (
                    <tr key={`${p.parserId}-detail`} className="bg-blue-50">
                      <td colSpan={7} className="px-6 py-3">
                        <ParserRulePreview parserId={p.parserId} />
                      </td>
                    </tr>
                  )}
                </>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* 생성/수정 모달 */}
      <Modal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        title={formMode === "create" ? "파서 추가" : "파서 수정"}
        size="lg"
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          {/* 파서명 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              파서명 <span className="text-red-500">*</span>
            </label>
            <input
              {...register("parserName", { required: "파서명은 필수입니다" })}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="예: 로그인 로그 파서"
            />
            {errors.parserName && (
              <p className="text-xs text-red-500 mt-1">{errors.parserName.message}</p>
            )}
          </div>

          {/* 파서 타입 (생성 시에만 변경 가능) */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              파서 타입 <span className="text-red-500">*</span>
            </label>
            <select
              {...register("parserType")}
              disabled={formMode === "edit"}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
            >
              <option value="DELIMITER">구분자 파서 (DELIMITER)</option>
              <option value="FIXED_WIDTH">고정폭 파서 (FIXED_WIDTH)</option>
              <option value="REGEX">정규식 파서 (REGEX)</option>
            </select>
            {formMode === "edit" && (
              <p className="text-xs text-gray-400 mt-1">파서 타입은 생성 후 변경할 수 없습니다.</p>
            )}
          </div>

          {/* 설명 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">설명</label>
            <textarea
              {...register("description")}
              rows={2}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
              placeholder="파서에 대한 설명을 입력하세요"
            />
          </div>

          {/* 활성화 여부 (수정 시만) */}
          {formMode === "edit" && (
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="isActive"
                {...register("isActive")}
                className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
              />
              <label htmlFor="isActive" className="text-sm font-medium text-gray-700">
                활성화
              </label>
            </div>
          )}

          {/* 파서 규칙 */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-sm font-medium text-gray-700">
                파서 규칙 <span className="text-red-500">*</span>
              </label>
              <button
                type="button"
                onClick={addRule}
                className="flex items-center gap-1 text-xs text-blue-600 hover:text-blue-700 font-medium"
              >
                <PlusIcon className="h-3.5 w-3.5" />
                규칙 추가
              </button>
            </div>

            <div className="space-y-3">
              {ruleFields.map((field, idx) => (
                <div key={field.id} className="p-3 border border-gray-200 rounded-lg bg-gray-50 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-gray-500">규칙 #{idx + 1}</span>
                    {ruleFields.length > 1 && (
                      <button
                        type="button"
                        onClick={() => remove(idx)}
                        className="text-xs text-red-500 hover:text-red-600"
                      >
                        삭제
                      </button>
                    )}
                  </div>

                  {/* config_json */}
                  <div>
                    <label className="text-xs text-gray-600 mb-1 block">
                      설정 JSON{" "}
                      <span className="text-gray-400">
                        {watchedType === "DELIMITER" && "예: {\"delimiter\":\"|\",\"index\":0}"}
                        {watchedType === "FIXED_WIDTH" && "예: {\"startByte\":0,\"byteLength\":10}"}
                        {watchedType === "REGEX" && "예: {\"pattern\":\"^(\\\\w+)\",\"group\":1}"}
                      </span>
                    </label>
                    <input
                      {...register(`rules.${idx}.configJson`, { required: "필수 입력" })}
                      className="w-full px-2 py-1.5 border border-gray-300 rounded text-xs font-mono focus:outline-none focus:ring-1 focus:ring-blue-500"
                      placeholder="설정 JSON"
                    />
                    {errors.rules?.[idx]?.configJson && (
                      <p className="text-xs text-red-500 mt-0.5">{errors.rules[idx]!.configJson!.message}</p>
                    )}
                  </div>

                  {/* 타겟 필드 */}
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="text-xs text-gray-600 mb-1 block">표준 필드 ID (선택)</label>
                      <input
                        {...register(`rules.${idx}.targetStandardFieldId`)}
                        className="w-full px-2 py-1.5 border border-gray-300 rounded text-xs focus:outline-none focus:ring-1 focus:ring-blue-500"
                        placeholder="예: login_id"
                      />
                    </div>
                    <div>
                      <label className="text-xs text-gray-600 mb-1 block">커스텀 필드명 (선택)</label>
                      <input
                        {...register(`rules.${idx}.targetFieldName`)}
                        className="w-full px-2 py-1.5 border border-gray-300 rounded text-xs focus:outline-none focus:ring-1 focus:ring-blue-500"
                        placeholder="예: extracted_field"
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* 버튼 */}
          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => setIsFormOpen(false)}
              className="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-4 py-2 text-sm text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              {formMode === "create" ? "추가" : "저장"}
            </button>
          </div>
        </form>
      </Modal>

      {/* 삭제 확인 다이얼로그 */}
      <ConfirmDialog
        isOpen={!!deletingParser}
        onClose={() => setDeletingParser(null)}
        onConfirm={() => deletingParser && deleteMutation.mutate(deletingParser.parserId)}
        title="파서 삭제"
        message={`"${deletingParser?.parserName}" 파서를 삭제하시겠습니까?\n이 파서를 사용하는 원본 필드의 파서 설정이 해제됩니다.`}
        confirmText="삭제"
        confirmColor="red"
      />
    </div>
  );
}

// ─── 규칙 미리보기 컴포넌트 ─────────────────────────────────────────────────────
function ParserRulePreview({ parserId }: { parserId: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ["parser", parserId],
    queryFn: () => fetchParser(parserId),
  });

  if (isLoading) return <div className="text-xs text-gray-400">로딩 중...</div>;
  if (!data?.rules?.length) return <div className="text-xs text-gray-400">규칙이 없습니다.</div>;

  return (
    <div className="space-y-1">
      <p className="text-xs font-semibold text-gray-500 mb-2">파서 규칙 목록</p>
      {data.rules.map((rule, idx) => (
        <div key={rule.parserRuleId} className="flex items-start gap-3 text-xs">
          <span className="text-gray-400 w-4">#{idx + 1}</span>
          <code className="bg-white border border-gray-200 px-2 py-0.5 rounded font-mono text-gray-700 flex-1 overflow-x-auto">
            {rule.configJson}
          </code>
          <span className="text-gray-500">
            →{" "}
            {rule.targetStandardFieldId || rule.targetFieldName || (
              <span className="text-gray-400 italic">필드 미지정</span>
            )}
          </span>
        </div>
      ))}
    </div>
  );
}

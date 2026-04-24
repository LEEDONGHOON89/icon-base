"use client";

// [2026-04-20] 파서 관리 페이지 - 재설계: sourceField + 타입별 UI
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  fetchParsers,
  fetchParser,
  createParser,
  updateParser,
  deleteParser,
  PARSER_TYPE_LABELS,
  PARSER_TYPE_COLORS,
  DEFAULT_RULE_CONFIG_JSON,
} from "./api";
import type {
  ParserSummary,
  ParserType,
  ParserRuleItem,
  CreateParserRequest,
  UpdateParserRequest,
} from "./api";
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
  InformationCircleIcon,
} from "@heroicons/react/24/outline";
import { Modal } from "@/components/ui/Modal";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";

// ─── 폼 타입 ────────────────────────────────────────────────────────────────

type FormMode = "create" | "edit";

interface RuleFormItem {
  // DELIMITER: ruleOrder가 split 인덱스 → 입력 불필요 (자동)
  // FIXED_WIDTH: byteLength 입력
  // REGEX: pattern, group 입력
  byteLength: string;       // FIXED_WIDTH 전용
  regexPattern: string;     // REGEX 전용
  regexGroup: string;       // REGEX 전용
  targetFieldName: string;  // 출력 필드명 (필수)
}

interface ParserFormValues {
  parserName: string;
  parserType: ParserType;
  sourceField: string;      // 파싱 대상 원본 필드명 (예: line)
  delimiter: string;        // DELIMITER 전용 (파서 레벨)
  description: string;
  isActive: boolean;
  rules: RuleFormItem[];
}

// ─── 헬퍼 ──────────────────────────────────────────────────────────────────

/** RuleItem 빌드: 폼값 → API 전송 형식 */
function buildRuleItems(values: ParserFormValues): ParserRuleItem[] {
  return values.rules.map((r, idx) => {
    let configJson: string | null = null;
    if (values.parserType === "FIXED_WIDTH") {
      const bl = parseInt(r.byteLength, 10);
      configJson = JSON.stringify({ byteLength: isNaN(bl) ? 1 : bl });
    } else if (values.parserType === "REGEX") {
      const grp = parseInt(r.regexGroup, 10);
      configJson = JSON.stringify({
        pattern: r.regexPattern,
        group: isNaN(grp) ? 1 : grp,
      });
    }
    return {
      ruleOrder: idx,
      configJson: configJson ?? null,
      targetFieldName: r.targetFieldName,
    };
  });
}

/** 빈 폼 규칙 초기값 */
function defaultRule(type: ParserType): RuleFormItem {
  return {
    byteLength: type === "FIXED_WIDTH" ? "4" : "",
    regexPattern: type === "REGEX" ? "^(\\w+)" : "",
    regexGroup: type === "REGEX" ? "1" : "",
    targetFieldName: "",
  };
}

// ─── 안내 박스 ──────────────────────────────────────────────────────────────

function TypeHint({ type }: { type: ParserType }) {
  const hints: Record<ParserType, { sample: string; desc: string }> = {
    DELIMITER: {
      sample: 'line 값: "A|B|C|D|E|F|G"  →  구분자 "|" 적용',
      desc: "파서에 구분자를 지정하고, 규칙마다 출력 필드명을 입력하면 split 순서대로 매핑됩니다.",
    },
    FIXED_WIDTH: {
      sample: 'line 값: "AAAABBBBCCCCC"  →  바이트 길이 4, 4, 5 적용',
      desc: "규칙마다 바이트 길이를 입력하세요. startByte는 엔진이 자동 계산합니다.",
    },
    REGEX: {
      sample: 'line 값: "KR|20240115"  →  패턴 "^(\\w+)\\|(\\d+)", group=1 → "KR"',
      desc: "규칙마다 정규식 패턴과 캡처 그룹 번호를 입력하세요.",
    },
  };
  const h = hints[type];
  return (
    <div className="flex gap-2 p-3 bg-blue-50 border border-blue-200 rounded-lg text-xs text-blue-700">
      <InformationCircleIcon className="h-4 w-4 shrink-0 mt-0.5" />
      <div>
        <p className="font-semibold mb-0.5">{h.sample}</p>
        <p className="text-blue-600">{h.desc}</p>
      </div>
    </div>
  );
}

// ─── 페이지 ─────────────────────────────────────────────────────────────────

export default function ParsersPage() {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState("");
  const [formMode, setFormMode] = useState<FormMode>("create");
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingParser, setEditingParser] = useState<ParserSummary | null>(null);
  const [deletingParser, setDeletingParser] = useState<ParserSummary | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  // 파서 목록
  const { data: parsers = [], isLoading } = useQuery({
    queryKey: ["parsers"],
    queryFn: fetchParsers,
  });

  // 편집 상세 (편집 모달 열릴 때 로드)
  const { data: editDetail } = useQuery({
    queryKey: ["parser", editingParser?.parserId],
    queryFn: () => fetchParser(editingParser!.parserId),
    enabled: !!editingParser && formMode === "edit",
  });

  // ─── 폼 ──────────────────────────────────────────────────────────────────

  const {
    register,
    handleSubmit,
    reset,
    watch,
    control,
    formState: { errors, isSubmitting },
  } = useForm<ParserFormValues>({
    defaultValues: {
      parserName: "",
      parserType: "DELIMITER",
      sourceField: "line",
      delimiter: "|",
      description: "",
      isActive: true,
      rules: [defaultRule("DELIMITER")],
    },
  });

  const { fields: ruleFields, append, remove } = useFieldArray({ control, name: "rules" });
  const watchedType = watch("parserType");

  // 생성 모드에서 타입 변경 시 rules 초기화
  useEffect(() => {
    if (formMode === "create") {
      reset((prev) => ({ ...prev, rules: [defaultRule(watchedType)] }));
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [watchedType]);

  // 편집 시 폼 초기화
  useEffect(() => {
    if (formMode === "edit" && editDetail) {
      const rules: RuleFormItem[] = editDetail.rules.map((r) => {
        let byteLength = "";
        let regexPattern = "";
        let regexGroup = "";
        if (r.configJson) {
          try {
            const cfg = JSON.parse(r.configJson);
            byteLength = cfg.byteLength != null ? String(cfg.byteLength) : "";
            regexPattern = cfg.pattern ?? "";
            regexGroup = cfg.group != null ? String(cfg.group) : "";
          } catch (_) {/* ignore */}
        }
        return {
          byteLength,
          regexPattern,
          regexGroup,
          targetFieldName: r.targetFieldName ?? "",
        };
      });

      // DELIMITER: 구분자는 parser 레벨 configJson에서 추출
      let delimiter = "|";
      if (editDetail.parserType === "DELIMITER" && editDetail.configJson) {
        try { delimiter = JSON.parse(editDetail.configJson).delimiter ?? "|"; } catch (_) {/* */}
      }

      reset({
        parserName: editDetail.parserName,
        parserType: editDetail.parserType,
        sourceField: editDetail.sourceField ?? "",
        delimiter,
        description: editDetail.description ?? "",
        isActive: editDetail.isActive,
        rules: rules.length > 0 ? rules : [defaultRule(editDetail.parserType)],
      });
    }
  }, [editDetail, formMode, reset]);

  // ─── Mutations ──────────────────────────────────────────────────────────

  const createMutation = useMutation({
    mutationFn: (data: CreateParserRequest) => createParser(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["parsers"] });
      toast.success("파서가 생성되었습니다.");
      setIsFormOpen(false);
    },
    onError: () => toast.error("파서 생성에 실패했습니다."),
  });

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

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteParser(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["parsers"] });
      toast.success("파서가 삭제되었습니다.");
      setDeletingParser(null);
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || "파서 삭제에 실패했습니다.");
      setDeletingParser(null);
    },
  });

  // ─── 핸들러 ─────────────────────────────────────────────────────────────

  const filtered = parsers.filter((p) => {
    if (!searchTerm) return true;
    const s = searchTerm.toLowerCase();
    return (
      p.parserName.toLowerCase().includes(s) ||
      (p.description ?? "").toLowerCase().includes(s) ||
      PARSER_TYPE_LABELS[p.parserType].toLowerCase().includes(s)
    );
  });

  const openCreate = () => {
    setFormMode("create");
    setEditingParser(null);
    reset({
      parserName: "",
      parserType: "DELIMITER",
      sourceField: "line",
      delimiter: "|",
      description: "",
      isActive: true,
      rules: [defaultRule("DELIMITER")],
    });
    setIsFormOpen(true);
  };

  const openEdit = (p: ParserSummary) => {
    setFormMode("edit");
    setEditingParser(p);
    setIsFormOpen(true);
  };

  const onSubmit = (values: ParserFormValues) => {
    const rules = buildRuleItems(values);
    // DELIMITER는 파서 레벨 configJson에 구분자 저장
    const configJson =
      values.parserType === "DELIMITER"
        ? JSON.stringify({ delimiter: values.delimiter })
        : null;

    if (formMode === "create") {
      createMutation.mutate({
        parserName: values.parserName,
        parserType: values.parserType,
        sourceField: values.sourceField,
        configJson,
        description: values.description || undefined,
        rules,
      });
    } else if (editingParser) {
      updateMutation.mutate({
        id: editingParser.parserId,
        data: {
          parserName: values.parserName,
          sourceField: values.sourceField,
          configJson,
          description: values.description || undefined,
          isActive: values.isActive,
          rules,
        },
      });
    }
  };

  // ─── JSX ────────────────────────────────────────────────────────────────

  // [2026-04-24] 레이아웃을 표준 필드 화면 기준으로 통일
  return (
    <div className="p-6 space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">파서 관리</h1>
          <p className="text-sm text-gray-500 mt-1">
            텍스트 필드를 구분자·고정폭·정규식으로 분리하는 파서를 관리합니다.
            파서를 데이터소스에 연결하면 수집 시 자동으로 필드를 추출합니다. (총 {parsers.length}개)
          </p>
        </div>
        <button
          onClick={openCreate}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm font-medium"
        >
          <PlusIcon className="h-4 w-4" />
          파서 추가
        </button>
      </div>

      {/* 검색 + 타입 통계 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4 space-y-3">
        <div className="relative">
          <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            type="text"
            placeholder="파서명, 설명, 타입으로 검색..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-10 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all"
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
        {/* 타입별 통계 배지 */}
        <div className="flex gap-2">
          {(["DELIMITER", "FIXED_WIDTH", "REGEX"] as ParserType[]).map((t) => (
            <span key={t} className={`text-xs px-3 py-1 rounded-full font-medium ${PARSER_TYPE_COLORS[t]}`}>
              {PARSER_TYPE_LABELS[t]}: {parsers.filter((p) => p.parserType === t).length}
            </span>
          ))}
        </div>
      </div>

      {/* 목록 */}
      {isLoading ? (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 flex justify-center items-center h-48">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 py-12 text-center text-gray-400">
          <ScissorsIcon className="h-10 w-10 mx-auto mb-3 opacity-30" />
          <p className="text-sm">{searchTerm ? "검색 결과가 없습니다." : "등록된 파서가 없습니다."}</p>
        </div>
      ) : (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="px-3 py-4 w-8"></th>
                <th className="text-left px-4 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">파서명</th>
                <th className="text-left px-4 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">타입</th>
                <th className="text-left px-4 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">파싱 대상 필드</th>
                <th className="text-left px-4 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">설명</th>
                <th className="text-center px-4 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">규칙</th>
                <th className="text-center px-4 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">상태</th>
                <th className="text-right px-4 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider w-24">작업</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filtered.map((p) => (
                <>
                  <tr key={p.parserId} className="hover:bg-gray-50 transition-colors">
                    <td className="px-3 py-3">
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
                    <td className="px-4 py-3">
                      {p.sourceField
                        ? <code className="text-xs bg-gray-100 px-1.5 py-0.5 rounded font-mono">{p.sourceField}</code>
                        : <span className="text-gray-400 text-xs">-</span>}
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
                  {/* 규칙 상세 펼침 */}
                  {expandedId === p.parserId && (
                    <tr key={`${p.parserId}-detail`} className="bg-blue-50/50">
                      <td colSpan={8} className="px-6 py-3">
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
            {errors.parserName && <p className="text-xs text-red-500 mt-1">{errors.parserName.message}</p>}
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

          {/* 타입별 안내 */}
          <TypeHint type={watchedType} />

          {/* 파싱 대상 원본 필드명 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              파싱 대상 필드명 <span className="text-red-500">*</span>
            </label>
            <input
              {...register("sourceField", { required: "파싱 대상 필드명은 필수입니다" })}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="예: line"
            />
            <p className="text-xs text-gray-400 mt-1">수집된 데이터에서 파싱할 원본 필드명 (예: line, raw_data)</p>
            {errors.sourceField && <p className="text-xs text-red-500 mt-1">{errors.sourceField.message}</p>}
          </div>

          {/* DELIMITER 전용: 구분자 입력 */}
          {watchedType === "DELIMITER" && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                구분자 <span className="text-red-500">*</span>
              </label>
              <input
                {...register("delimiter", { required: "구분자는 필수입니다" })}
                className="w-40 px-3 py-2 border border-gray-300 rounded-lg text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="예: |"
              />
              {errors.delimiter && <p className="text-xs text-red-500 mt-1">{errors.delimiter.message}</p>}
            </div>
          )}

          {/* 설명 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">설명</label>
            <textarea
              {...register("description")}
              rows={2}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
              placeholder="파서에 대한 설명"
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
              <label htmlFor="isActive" className="text-sm font-medium text-gray-700">활성화</label>
            </div>
          )}

          {/* 파서 규칙 */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-sm font-medium text-gray-700">
                출력 필드 목록 <span className="text-red-500">*</span>
                <span className="ml-1 text-xs text-gray-400 font-normal">
                  ({watchedType === "DELIMITER" ? "분리된 값이 순서대로 아래 필드에 저장됩니다" :
                    watchedType === "FIXED_WIDTH" ? "각 규칙의 바이트 길이만큼 순서대로 추출합니다" :
                    "각 규칙의 정규식으로 독립적으로 추출합니다"})
                </span>
              </label>
              <button
                type="button"
                onClick={() => append(defaultRule(watchedType))}
                className="flex items-center gap-1 text-xs text-blue-600 hover:text-blue-700 font-medium"
              >
                <PlusIcon className="h-3.5 w-3.5" />
                필드 추가
              </button>
            </div>

            <div className="space-y-2">
              {ruleFields.map((field, idx) => (
                <div key={field.id} className="flex items-start gap-2 p-3 border border-gray-200 rounded-lg bg-gray-50">
                  {/* 인덱스 뱃지 */}
                  <span className="mt-1.5 flex-none w-6 h-6 flex items-center justify-center bg-blue-100 text-blue-700 rounded-full text-xs font-bold">
                    {idx}
                  </span>

                  {/* FIXED_WIDTH: byteLength */}
                  {watchedType === "FIXED_WIDTH" && (
                    <div className="flex-none w-28">
                      <label className="text-xs text-gray-500 mb-1 block">바이트 길이</label>
                      <input
                        {...register(`rules.${idx}.byteLength`, {
                          required: "필수",
                          pattern: { value: /^\d+$/, message: "숫자만" },
                        })}
                        type="number"
                        min={1}
                        className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                        placeholder="예: 4"
                      />
                      {errors.rules?.[idx]?.byteLength && (
                        <p className="text-xs text-red-500 mt-0.5">{errors.rules[idx]!.byteLength!.message}</p>
                      )}
                    </div>
                  )}

                  {/* REGEX: pattern + group */}
                  {watchedType === "REGEX" && (
                    <>
                      <div className="flex-1">
                        <label className="text-xs text-gray-500 mb-1 block">정규식 패턴</label>
                        <input
                          {...register(`rules.${idx}.regexPattern`, { required: "필수" })}
                          className="w-full px-2 py-1.5 border border-gray-300 rounded text-xs font-mono focus:outline-none focus:ring-1 focus:ring-blue-500"
                          placeholder="예: ^(\w+)"
                        />
                        {errors.rules?.[idx]?.regexPattern && (
                          <p className="text-xs text-red-500 mt-0.5">{errors.rules[idx]!.regexPattern!.message}</p>
                        )}
                      </div>
                      <div className="flex-none w-20">
                        <label className="text-xs text-gray-500 mb-1 block">캡처 그룹</label>
                        <input
                          {...register(`rules.${idx}.regexGroup`)}
                          type="number"
                          min={0}
                          className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                          placeholder="1"
                        />
                      </div>
                    </>
                  )}

                  {/* 출력 필드명 (공통 필수) */}
                  <div className="flex-1">
                    <label className="text-xs text-gray-500 mb-1 block">출력 필드명 <span className="text-red-400">*</span></label>
                    <input
                      {...register(`rules.${idx}.targetFieldName`, { required: "출력 필드명은 필수입니다" })}
                      className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                      placeholder="예: COLUMN1"
                    />
                    {errors.rules?.[idx]?.targetFieldName && (
                      <p className="text-xs text-red-500 mt-0.5">{errors.rules[idx]!.targetFieldName!.message}</p>
                    )}
                  </div>

                  {/* 삭제 */}
                  {ruleFields.length > 1 && (
                    <button
                      type="button"
                      onClick={() => remove(idx)}
                      className="mt-6 text-red-400 hover:text-red-600"
                    >
                      <TrashIcon className="h-4 w-4" />
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* 버튼 */}
          <div className="flex justify-end gap-3 pt-2 border-t border-gray-100">
            <button
              type="button"
              onClick={() => setIsFormOpen(false)}
              className="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={isSubmitting || createMutation.isPending || updateMutation.isPending}
              className="px-4 py-2 text-sm text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              {formMode === "create" ? "추가" : "저장"}
            </button>
          </div>
        </form>
      </Modal>

      {/* 삭제 확인 */}
      <ConfirmDialog
        isOpen={!!deletingParser}
        onClose={() => setDeletingParser(null)}
        onConfirm={() => deletingParser && deleteMutation.mutate(deletingParser.parserId)}
        title="파서 삭제"
        message={`"${deletingParser?.parserName}" 파서를 삭제하시겠습니까?\n이 파서를 사용하는 데이터소스 연결도 함께 삭제됩니다.`}
        confirmText="삭제"
        confirmColor="red"
      />
    </div>
  );
}

// ─── 규칙 미리보기 ─────────────────────────────────────────────────────────

function ParserRulePreview({ parserId }: { parserId: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ["parser", parserId],
    queryFn: () => fetchParser(parserId),
  });

  if (isLoading) return <div className="text-xs text-gray-400">로딩 중...</div>;
  if (!data) return null;

  return (
    <div className="space-y-2">
      <div className="flex items-center gap-4 text-xs text-gray-500 mb-2">
        <span>파싱 대상: <code className="bg-white border border-gray-200 px-1.5 py-0.5 rounded font-mono">{data.sourceField ?? "-"}</code></span>
        {data.parserType === "DELIMITER" && data.configJson && (
          <span>
            구분자: <code className="bg-white border border-gray-200 px-1.5 py-0.5 rounded font-mono">
              {(() => { try { return JSON.parse(data.configJson).delimiter; } catch { return "?"; } })()}
            </code>
          </span>
        )}
      </div>
      {!data.rules?.length ? (
        <p className="text-xs text-gray-400">규칙이 없습니다.</p>
      ) : (
        <div className="grid grid-cols-1 gap-1">
          {data.rules.map((rule, idx) => (
            <div key={rule.parserRuleId} className="flex items-center gap-3 text-xs bg-white border border-gray-100 rounded px-3 py-1.5">
              <span className="w-5 h-5 flex items-center justify-center bg-blue-100 text-blue-600 rounded-full font-bold text-xs">{idx}</span>
              {rule.configJson && (
                <code className="text-gray-500 font-mono bg-gray-50 px-1.5 rounded">{rule.configJson}</code>
              )}
              <span className="text-gray-400">→</span>
              <span className="font-medium text-gray-700">
                {rule.targetFieldName || <span className="italic text-gray-400">미지정</span>}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

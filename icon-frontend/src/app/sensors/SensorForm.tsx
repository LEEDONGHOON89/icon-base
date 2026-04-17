"use client";

import Alert from "@/components/common/Alert";
import Input from "@/components/common/Input";
import LoadingButton from "@/components/common/LoadingButton";
import Autocomplete from "@/components/common/Autocomplete";
import { useEffect, useRef } from "react";
import { useForm, Controller, useFieldArray } from "react-hook-form";
import { useQuery } from "@tanstack/react-query";
import { SensorCreateRequest } from "./api";
import { fetchRuleFields } from "../metadata/api";
import { toast } from "react-hot-toast";

type ConditionRow = { fieldName?: string; operator?: string; value?: any };
export type SensorFormValues = SensorCreateRequest & { mode?: "builder" | "json"; conditions?: ConditionRow[] };

interface SensorFormProps {
  onSubmit: (values: SensorFormValues) => void;
  defaultValues?: Partial<SensorFormValues>;
  isEdit?: boolean;
  isPending?: boolean;
  error?: string | null;
}

function SensorForm({
  onSubmit,
  defaultValues = {},
  isEdit = false,
  isPending = false,
  error,
}: SensorFormProps) {
  // 이전 조건(JSON) 스냅샷 - 편집 참고용
  const prevWhereRef = useRef<string | null>(null);
  if (prevWhereRef.current === null) {
    const pretty = (s: string) => {
      try { return JSON.stringify(JSON.parse(s), null, 2); } catch { return s; }
    };
    const w = (defaultValues as any).whereJson as string | undefined;
    if (w && w.trim() !== "") {
      prevWhereRef.current = pretty(w);
    } else {
      const f = (defaultValues as any).fieldName;
      const o = (defaultValues as any).operator;
      const v = (defaultValues as any).value;
      if (f && o) {
        prevWhereRef.current = JSON.stringify({ fieldName: f, operator: o, value: v }, null, 2);
      }
    }
  }

  // 메타데이터 조회: Rule 필드 (센서도 동일한 필드 사용)
  const { data: fields, isLoading: fieldsLoading } = useQuery({
    queryKey: ["metadata", "rule-fields"],
    queryFn: fetchRuleFields,
  });

  const {
    register,
    handleSubmit,
    control,
    watch,
    reset,
    setValue,
    formState: { errors },
  } = useForm<SensorFormValues>({
    defaultValues: {
      ...defaultValues,
      mode: ((defaultValues as any).whereJson && (defaultValues as any).whereJson.trim() !== "") ? "json" : "builder",
      conditions: (() => {
        const w = (defaultValues as any).whereJson as string | undefined;
        if (w && w.trim() !== "") {
          try {
            const node = JSON.parse(w);
            if (Array.isArray(node)) {
              return node.map((n) => ({ fieldName: n.field || n.fieldName, operator: n.operator, value: n.value }));
            } else if (typeof node === "object") {
              return [{ fieldName: node.field || node.fieldName, operator: node.operator, value: node.value }];
            }
          } catch (e) { /* ignore parse errors */ }
        }
        return [{ fieldName: (defaultValues as any).fieldName ?? "", operator: (defaultValues as any).operator ?? "", value: (defaultValues as any).value ?? "" }];
      })(),
      whereJson: (() => {
        const w = (defaultValues as any).whereJson;
        if (!w || w.trim() === "") return "";
        try {
          const parsed = JSON.parse(w);
          return JSON.stringify(parsed, null, 2);
        } catch {
          return w;
        }
      })(),
    },
  });

  const { fields: condFields, append, remove, update, replace } = useFieldArray({
    control,
    name: "conditions" as never,
  });

  const watchedConditions = watch("conditions") as ConditionRow[] | undefined;
  const watchedMode = (watch("mode" as any) as string) ?? "builder";

  // 컴포넌트 마운트 시 한 번만 defaultValues 설정
  useEffect(() => {
    if (isEdit || Object.keys(defaultValues).length > 0) {
      const formValues: SensorFormValues = {
        sensorId: defaultValues.sensorId ?? "",
        sensorName: defaultValues.sensorName ?? "",
        mode: "builder",
        conditions: (() => {
          const w = (defaultValues as any).whereJson as string | undefined;
          if (w && w.trim() !== "") {
            try {
              const node = JSON.parse(w);
              if (Array.isArray(node)) {
                return node.map((n: any) => ({ fieldName: n.field || n.fieldName, operator: n.operator, value: n.value }));
              } else if (typeof node === "object") {
                return [{ fieldName: (node as any).field || (node as any).fieldName, operator: (node as any).operator, value: (node as any).value }];
              }
            } catch (e) { /* ignore */ }
          }
          return [{ fieldName: (defaultValues as any).fieldName ?? "", operator: (defaultValues as any).operator ?? "", value: (defaultValues as any).value ?? "" }];
        })(),
        description: defaultValues.description ?? "",
        whereJson: (() => {
          const w = (defaultValues as any).whereJson;
          if (!w || w.trim() === "") return "";
          try {
            const parsed = JSON.parse(w);
            return JSON.stringify(parsed, null, 2);
          } catch {
            return w;
          }
        })(),
      };

      reset(formValues);
    }
  }, [isEdit]);

  // 필드 선택 시: 기존 연산자/값을 최대한 보존
  const handleFieldChange = (rowIndex: number, fieldName: string) => {
    const prev = (watchedConditions?.[rowIndex] || {}) as ConditionRow;
    update(rowIndex, {
      ...(prev || {}),
      fieldName,
      operator: prev.operator,
      value: prev.value,
    } as any);
  };

  const whereJsonValue = watch("whereJson" as any) as string | undefined;

  // JSON 포맷 함수
  const handleJsonFormat = () => {
    try {
      if (!whereJsonValue || whereJsonValue.trim() === "") {
        return;
      }
      const parsed = JSON.parse(whereJsonValue);
      setValue("whereJson" as any, JSON.stringify(parsed, null, 2));
      toast.success("JSON 포맷이 적용되었습니다.");
    } catch (e) {
      toast.error("유효하지 않은 JSON입니다.");
    }
  };

  const handleJsonAddAnd = () => {
    try {
      if (!whereJsonValue || whereJsonValue.trim() === "") {
        setValue("whereJson" as any, "[\n  {}\n]");
        return;
      }
      const parsed = JSON.parse(whereJsonValue);
      if (Array.isArray(parsed)) {
        parsed.push({});
        setValue("whereJson" as any, JSON.stringify(parsed, null, 2));
      } else if (typeof parsed === "object") {
        setValue("whereJson" as any, JSON.stringify([parsed, {}], null, 2));
      } else {
        setValue("whereJson" as any, "[\n  {}\n]");
      }
    } catch (e) {
      setValue("whereJson" as any, "[\n  {}\n]");
    }
  };

  // JSON -> 빌더 전환 시 현재 JSON을 파싱해 conditions로 반영
  useEffect(() => {
    if (watchedMode === "builder") {
      const w = whereJsonValue;
      if (!w || w.trim() === "") return;
      try {
        const node = JSON.parse(w);
        const toRow = (n: any): ConditionRow => ({
          fieldName: n.fieldName ?? n.field ?? "",
          operator: n.operator ?? "",
          value: n.value ?? "",
        });
        const rows: ConditionRow[] = Array.isArray(node) ? node.map(toRow) : [toRow(node)];
        if (rows.length > 0) {
          replace(rows as any);
        }
      } catch (e) {
        // ignore parse error, keep user input
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [watchedMode]);

  // 메타데이터 로딩 중일 때
  if (fieldsLoading) {
    return (
      <div className="text-center py-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
        <p className="mt-2 text-gray-600">메타데이터를 불러오는 중...</p>
      </div>
    );
  }

  return (
    <div className="max-h-[85vh] overflow-y-auto">
      <form onSubmit={handleSubmit((vals) => {
        const mode = (vals as any).mode ?? "builder";
        if (mode === "builder") {
          const conds = (vals as any).conditions as ConditionRow[] | undefined;
          if (conds && conds.length > 0) {
            const builtList = conds
              .filter((c) => c.fieldName && c.operator)
              .map((c) => {
                const obj: any = { fieldName: c.fieldName, operator: c.operator };
                if (c.value !== undefined && c.value !== "") {
                  if (typeof c.value === "string" && c.value.includes(",")) {
                    obj.value = c.value.split(",").map((v: string) => v.trim()).filter(Boolean);
                  } else {
                    obj.value = c.value;
                  }
                }
                return obj;
              });
            if (builtList.length === 1) {
              (vals as any).whereJson = JSON.stringify(builtList[0]);
            } else if (builtList.length > 1) {
              (vals as any).whereJson = JSON.stringify(builtList);
            }
          }
        }
        onSubmit(vals);
      })}>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* 왼쪽: 기본 정보 */}
          <div className="space-y-6 px-1">
            <h3 className="text-lg font-semibold text-gray-900 border-b pb-2">
              기본 정보
            </h3>

            {isEdit ? (
              <Input
                label="센서 ID"
                value={(defaultValues as any).sensorId || ""}
                disabled
                className="bg-gray-50"
              />
            ) : (
              <Input
                label="센서 ID (S_ 프리픽스 권장)"
                {...register("sensorId", {
                  required: "센서 ID는 필수입니다.",
                  minLength: { value: 2, message: "센서 ID는 2자 이상" },
                  maxLength: { value: 100, message: "센서 ID는 100자 이하" },
                  pattern: {
                    value: /^[A-Z_][A-Z0-9_]*$/,
                    message: "센서 ID는 대문자, 숫자, 언더스코어만 사용 가능합니다"
                  }
                })}
                placeholder="예: S_LOGIN_FAIL"
                error={errors.sensorId?.message}
              />
            )}

            <Input
              label="센서명"
              {...register("sensorName", {
                required: "센서명은 필수입니다.",
                minLength: { value: 2, message: "센서명은 2자 이상" },
                maxLength: { value: 100, message: "센서명은 100자 이하" },
              })}
              placeholder="예: 로그인 실패 감지"
              error={errors.sensorName?.message}
            />

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                설명
              </label>
              <textarea
                {...register("description", {
                  maxLength: { value: 500, message: "설명은 500자 이하" },
                })}
                placeholder="센서에 대한 설명을 입력하세요"
                rows={3}
                className="w-full p-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
              />
              {errors.description?.message && (
                <p className="text-xs text-red-600 mt-1">
                  {errors.description.message}
                </p>
              )}
            </div>

            {/* 모드 선택: 간편(Builder) / JSON */}
            <div className="flex items-center gap-4">
              <label className="inline-flex items-center gap-2 text-sm">
                <input type="radio" value="builder" {...register("mode" as any)} /> 간편 조건
              </label>
              <label className="inline-flex items-center gap-2 text-sm">
                <input type="radio" value="json" {...register("mode" as any)} /> JSON 조건
              </label>
            </div>
          </div>

          {/* 오른쪽: 조건 설정 */}
          <div className="space-y-4 px-1 lg:col-span-2">
            <div className="border-b pb-2">
              <h3 className="text-lg font-semibold text-gray-900">조건 설정</h3>
            </div>

            {isEdit && prevWhereRef.current && (
              <div className="rounded-md border border-gray-200 bg-gray-50 p-3">
                <div className="text-xs text-gray-500 mb-1">이전 조건 (참고용)</div>
                <pre className="text-xs font-mono whitespace-pre-wrap">{prevWhereRef.current}</pre>
              </div>
            )}

            <div className={"h-100"}>
              {watchedMode === "builder" && (
                <div className="space-y-3">
                  {condFields.map((row, idx) => {
                    const current = watchedConditions?.[idx] || {};
                    const baseFieldOptions = (fields || []).map((f) => ({ value: f.name, label: `${f.label} (${f.name})`, category: f.category }));
                    const hasUnknownField = !!(current.fieldName && !(fields || []).some((f) => f.name === current.fieldName));
                    const fieldOptions = baseFieldOptions;

                    const selectedField = (fields || []).find((f) => f.name === current.fieldName);
                    const allOps = (() => {
                      const seen = new Set<string>();
                      const acc: any[] = [];
                      for (const f of fields || []) {
                        for (const op of f.availableOperators || []) {
                          if (!seen.has(op.value)) { seen.add(op.value); acc.push(op); }
                        }
                      }
                      return acc;
                    })();
                    const fieldOperators = selectedField?.availableOperators && (selectedField.availableOperators.length > 0) ? selectedField.availableOperators : allOps;
                    const hasUnknownOperator = !!(current.operator && !fieldOperators.some((op: any) => op.value === current.operator));
                    const mappedOps = fieldOperators.map((op: any) => ({ value: op.value, label: `${op.label} (${op.value})`, category: op.category }));
                    const operatorOptions = hasUnknownOperator
                      ? [{ value: current.operator as string, label: `${current.operator} (유효하지 않음)`, category: "기타" }, ...mappedOps]
                      : mappedOps;
                    return (
                      <div key={row.id} className="grid grid-cols-12 gap-2 items-end">
                        <div className="col-span-4">
                          <Controller
                            name={`conditions.${idx}.fieldName` as any}
                            control={control}
                            rules={{ required: "필드를 선택하세요" } as any}
                            render={({ field: { onChange, value } }) => (
                              <Autocomplete
                                label={idx === 0 ? "필드" : undefined}
                                options={fieldOptions}
                                value={(hasUnknownField ? "" : (value as any))}
                                onChange={(val) => {
                                  onChange(val);
                                  handleFieldChange(idx, val);
                                }}
                                placeholder={hasUnknownField ? "없음" : "필드"}
                                groupBy={(option) => option.category || "기타"}
                              />
                            )}
                          />
                        </div>
                        <div className="col-span-4">
                          <Controller
                            name={`conditions.${idx}.operator` as any}
                            control={control}
                            rules={{ required: "연산자" } as any}
                            render={({ field: { onChange, value } }) => (
                              <Autocomplete
                                label={idx === 0 ? "연산자" : undefined}
                                options={operatorOptions}
                                value={value as any}
                                onChange={onChange as any}
                                placeholder="연산자"
                                groupBy={(option) => option.category || "기타"}
                              />
                            )}
                          />
                        </div>
                        <div className="col-span-3">
                          <Controller
                            name={`conditions.${idx}.value` as any}
                            control={control}
                            render={({ field: { onChange, value } }) => (
                              <input
                                type="text"
                                value={(value as any) ?? ""}
                                onChange={(e) => onChange(e.target.value)}
                                placeholder="값"
                                className="w-full p-2 text-sm border border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500"
                              />
                            )}
                          />
                        </div>
                        <div className="col-span-1 flex gap-1 justify-end">
                          {condFields.length > 1 && (
                            <button type="button" onClick={() => remove(idx)} className="text-red-500 text-sm">삭제</button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                  <div className="flex justify-between">
                    <button type="button" onClick={() => append({} as any)} className="text-blue-600 text-sm">+ 조건 추가(AND)</button>
                  </div>

                  {/* JSON 미리보기 */}
                  {watchedConditions && watchedConditions.length > 0 && watchedConditions.some(c => c.fieldName || c.operator) && (
                    <div className="mt-4 p-3 bg-gray-50 rounded-md border border-gray-200">
                      <div className="flex items-center justify-between mb-2">
                        <label className="text-xs font-medium text-gray-700">생성될 JSON (미리보기)</label>
                        <span className="text-xs text-gray-500">저장 시 이 형식으로 전송됩니다</span>
                      </div>
                      <pre className="text-xs font-mono text-gray-800 bg-white p-2 rounded border border-gray-300 overflow-x-auto">
                        {(() => {
                          const builtList = watchedConditions
                            .filter((c) => c.fieldName && c.operator)
                            .map((c) => {
                              const obj: any = { fieldName: c.fieldName, operator: c.operator };
                              if (c.value !== undefined && c.value !== "") {
                                if (typeof c.value === "string" && c.value.includes(",")) {
                                  obj.value = c.value.split(",").map((v: string) => v.trim()).filter(Boolean);
                                } else {
                                  obj.value = c.value;
                                }
                              }
                              return obj;
                            });
                          if (builtList.length === 0) return "{}";
                          if (builtList.length === 1) return JSON.stringify(builtList[0], null, 2);
                          return JSON.stringify(builtList, null, 2);
                        })()}
                      </pre>
                    </div>
                  )}
                </div>
              )}

              {/* JSON 모드: where_json 직접 입력 */}
              {watchedMode === "json" && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    고급 조건(JSON) - where_json
                  </label>
                  <textarea
                    {...register("whereJson" as any)}
                    rows={8}
                    placeholder='예: {"fieldName":"login_fail_count","operator":"GREATER_THAN_OR_EQUALS","value":3}'
                    className="w-full p-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 font-mono text-sm"
                  />
                  <div className="mt-2 flex items-center justify-between">
                    <p className="text-xs text-gray-500">
                      {`객체 1개 또는 배열([ {...}, {...} ]) 형식의 AND 조건을 입력할 수 있습니다.`}
                    </p>
                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={handleJsonFormat}
                        className="px-3 py-1 text-xs bg-gray-100 hover:bg-gray-200 text-gray-700 rounded transition-colors"
                      >
                        포맷
                      </button>
                      <button
                        type="button"
                        onClick={handleJsonAddAnd}
                        className="px-3 py-1 text-xs bg-blue-600 hover:bg-blue-700 text-white rounded transition-colors"
                      >
                        + AND 추가
                      </button>
                    </div>
                  </div>
                </div>
              )}

            </div>
          </div>
        </div>

        {/* 하단 버튼 */}
        <div className="mt-8 pt-6 border-t">
          {error && <Alert message={error} />}
          <div className="flex justify-end">
            <LoadingButton
              loading={isPending}
              type="submit"
              size="small"
              className="w-auto"
            >
              {isEdit ? "수정하기" : "등록하기"}
            </LoadingButton>
          </div>
        </div>
      </form>
    </div>
  );
}

export default SensorForm;

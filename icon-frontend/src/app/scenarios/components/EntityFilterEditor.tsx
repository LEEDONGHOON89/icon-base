import { useState, useEffect, useRef } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchEntityFields, type EntityField } from "@/app/entity-fields/api";
import { PlusIcon, TrashIcon, CheckCircleIcon } from "@heroicons/react/24/outline";
import toast from "react-hot-toast";

// 엔티티 필터 조건 타입
export interface EntityFilterCondition {
  fieldName: string;
  operator: string;
  value: string | string[];
}

// 연산자 옵션
const OPERATOR_OPTIONS = [
  { value: "EQUALS", label: "같음 (=)" },
  { value: "NOT_EQUALS", label: "다름 (≠)" },
  { value: "GREATER_THAN_OR_EQUALS", label: "크거나 같음 (≥)" },
  { value: "LESS_THAN_OR_EQUALS", label: "작거나 같음 (≤)" },
  { value: "IN", label: "포함 (IN)" },
  { value: "NOT_IN", label: "불포함 (NOT IN)" },
  { value: "CONTAINS", label: "문자열 포함" },
];

interface EntityFilterEditorProps {
  value: EntityFilterCondition[];
  onChange: (filters: EntityFilterCondition[]) => void;
}

// [2026-04-24] 순환 상태 업데이트 구조 개선:
//   - isMountRef: 마운트 직후 onChange 호출 방지 (불필요한 부모 리렌더 억제)
//   - value sync effect: 부모가 시나리오 로드 후 entityFilters 를 변경할 때만 동기화
//     (사용자 편집 중에는 부모가 value 를 동일 참조로 내려주므로 동기화가 일어나지 않음)
export default function EntityFilterEditor({ value, onChange }: EntityFilterEditorProps) {
  const [entityFilters, setEntityFilters] = useState<EntityFilterCondition[]>(value || []);
  const [validationResult, setValidationResult] = useState<{ valid: boolean; message: string } | null>(null);
  // [2026-04-24] 마운트 직후 onChange 스킵 — 초기값을 부모에 역전파하면 불필요한 리렌더 발생
  const isMountedRef = useRef(false);

  // value prop이 변경될 때 내부 state 동기화 (부모의 초기화/외부 변경 반영)
  useEffect(() => {
    setEntityFilters(value || []);
  }, [value]);

  // 엔티티 필드 목록 조회
  const { data: entityFields = [], isLoading: entityFieldsLoading } = useQuery({
    queryKey: ["entityFields"],
    queryFn: fetchEntityFields,
  });

  // 부모 컴포넌트에 변경사항 전달 (마운트 직후 첫 실행은 건너뜀)
  useEffect(() => {
    if (!isMountedRef.current) {
      isMountedRef.current = true;
      return;
    }
    onChange(entityFilters);
  }, [entityFilters, onChange]);

  // 필터 검증 함수
  const validateFilters = () => {
    if (entityFilters.length === 0) {
      setValidationResult({ valid: true, message: "필터가 없습니다." });
      toast("필터가 없습니다.", { icon: "ℹ️" });
      return;
    }

    // 각 필터 검증
    for (let i = 0; i < entityFilters.length; i++) {
      const filter = entityFilters[i];
      const field = entityFields.find(f => f.entityFieldId === filter.fieldName);

      // 필드 존재 확인
      if (!field) {
        setValidationResult({ valid: false, message: `필터 ${i + 1}: 유효하지 않은 필드입니다.` });
        toast.error(`필터 ${i + 1}: 유효하지 않은 필드입니다.`);
        return;
      }

      // 연산자 확인
      if (!filter.operator) {
        setValidationResult({ valid: false, message: `필터 ${i + 1}: 연산자가 필요합니다.` });
        toast.error(`필터 ${i + 1}: 연산자가 필요합니다.`);
        return;
      }

      // 값 확인
      const isArrayOperator = filter.operator === "IN" || filter.operator === "NOT_IN";
      if (isArrayOperator) {
        if (!Array.isArray(filter.value) || filter.value.length === 0) {
          setValidationResult({ valid: false, message: `필터 ${i + 1}: 최소 1개 이상의 값이 필요합니다.` });
          toast.error(`필터 ${i + 1}: 최소 1개 이상의 값이 필요합니다.`);
          return;
        }
        // 빈 문자열 체크
        const hasEmpty = filter.value.some(v => !v || v.trim() === "");
        if (hasEmpty) {
          setValidationResult({ valid: false, message: `필터 ${i + 1}: 빈 값이 포함되어 있습니다.` });
          toast.error(`필터 ${i + 1}: 빈 값이 포함되어 있습니다.`);
          return;
        }
      } else {
        if (!filter.value || (typeof filter.value === "string" && filter.value.trim() === "")) {
          setValidationResult({ valid: false, message: `필터 ${i + 1}: 값이 필요합니다.` });
          toast.error(`필터 ${i + 1}: 값이 필요합니다.`);
          return;
        }
      }
    }

    setValidationResult({ valid: true, message: "모든 필터가 유효합니다." });
    toast.success("✅ 필터 설정이 유효합니다.");
  };

  // 엔티티 필터 추가
  const addEntityFilter = () => {
    const usedFieldIds = entityFilters.map(f => f.fieldName);
    const availableField = entityFields.find(field => !usedFieldIds.includes(field.entityFieldId));

    if (availableField) {
      setEntityFilters([
        ...entityFilters,
        {
          fieldName: availableField.entityFieldId,
          operator: "EQUALS",
          value: ""
        }
      ]);
    } else if (entityFields.length === 0) {
      toast.error("사용 가능한 엔티티 필드가 없습니다.");
    } else {
      toast.error("모든 필드가 이미 추가되었습니다.");
    }
  };

  // 엔티티 필터 제거
  const removeEntityFilter = (index: number) => {
    setEntityFilters(entityFilters.filter((_, i) => i !== index));
  };

  // 엔티티 필터 필드명 변경
  const updateFilterFieldName = (index: number, fieldName: string) => {
    const updated = [...entityFilters];
    updated[index].fieldName = fieldName;
    setEntityFilters(updated);
  };

  // 엔티티 필터 연산자 변경
  const updateFilterOperator = (index: number, operator: string) => {
    const updated = [...entityFilters];
    updated[index].operator = operator;

    // IN/NOT_IN 연산자로 변경하면 값을 배열로 변환
    if ((operator === "IN" || operator === "NOT_IN") && typeof updated[index].value === "string") {
      updated[index].value = updated[index].value ? [updated[index].value as string] : [];
    }
    // 다른 연산자로 변경하면 값을 문자열로 변환
    else if (operator !== "IN" && operator !== "NOT_IN" && Array.isArray(updated[index].value)) {
      updated[index].value = updated[index].value.length > 0 ? updated[index].value[0] : "";
    }

    setEntityFilters(updated);
  };

  // 엔티티 필터 값 변경
  const updateFilterValue = (index: number, value: string | string[]) => {
    const updated = [...entityFilters];
    updated[index].value = value;
    setEntityFilters(updated);
  };

  // 배열 값을 문자열로 변환 (화면 표시용)
  const getArrayValueAsString = (value: string | string[]): string => {
    if (Array.isArray(value)) {
      return value.join(", ");
    }
    return value || "";
  };

  // 문자열을 배열로 변환 (저장용)
  const parseStringToArray = (str: string): string[] => {
    return str
      .split(",")
      .map(v => v.trim())
      .filter(v => v.length > 0);
  };

  return (
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-3">
        엔티티 필터 (선택)
      </label>
      <div className="grid grid-cols-2 gap-4">
        {/* 왼쪽: 입력 폼 */}
        <div className="border border-gray-300 rounded-xl p-4 bg-gray-50">
          <div className="flex justify-between items-center mb-3">
            <h4 className="text-sm font-semibold text-gray-700">필터 설정</h4>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={validateFilters}
                className="px-3 py-1 text-xs bg-green-600 text-white rounded-lg hover:bg-green-700 flex items-center gap-1"
              >
                <CheckCircleIcon className="w-3 h-3" />
                검증
              </button>
              <button
                type="button"
                onClick={addEntityFilter}
                className="px-3 py-1 text-xs bg-blue-600 text-white rounded-lg hover:bg-blue-700 flex items-center gap-1"
              >
                <PlusIcon className="w-3 h-3" />
                추가
              </button>
            </div>
          </div>
          <div className="space-y-3 max-h-60 overflow-y-auto">
            {entityFieldsLoading ? (
              <p className="text-xs text-gray-500 text-center py-4">
                로딩 중...
              </p>
            ) : entityFilters.length === 0 ? (
              <p className="text-xs text-gray-500 text-center py-4">
                필터를 추가하세요
              </p>
            ) : (
              entityFilters.map((filter, index) => {
                const field = entityFields.find(f => f.entityFieldId === filter.fieldName);
                const isArrayValue = filter.operator === "IN" || filter.operator === "NOT_IN";

                return (
                  <div key={index} className="border border-gray-200 rounded-lg p-2 bg-white">
                    <div className="flex gap-2 items-start">
                      {/* 필드 선택 */}
                      <select
                        value={filter.fieldName}
                        onChange={(e) => updateFilterFieldName(index, e.target.value)}
                        className="flex-1 px-2 py-1.5 text-xs border border-gray-300 rounded focus:ring-1 focus:ring-blue-500"
                      >
                        {/* 현재 선택된 필드 */}
                        <option value={filter.fieldName}>
                          {field?.displayName || filter.fieldName}
                        </option>
                        {/* 아직 사용되지 않은 필드들 */}
                        {entityFields
                          .filter(f => f.entityFieldId !== filter.fieldName && !entityFilters.some(ef => ef.fieldName === f.entityFieldId))
                          .map(f => (
                            <option key={f.entityFieldId} value={f.entityFieldId}>
                              {f.displayName}
                            </option>
                          ))
                        }
                      </select>

                      {/* 연산자 선택 */}
                      <select
                        value={filter.operator}
                        onChange={(e) => updateFilterOperator(index, e.target.value)}
                        className="w-32 px-2 py-1.5 text-xs border border-gray-300 rounded focus:ring-1 focus:ring-blue-500"
                      >
                        {OPERATOR_OPTIONS.map(op => (
                          <option key={op.value} value={op.value}>
                            {op.label}
                          </option>
                        ))}
                      </select>

                      {/* 값 입력 */}
                      <div className="flex-1">
                        {isArrayValue ? (
                          <div>
                            <input
                              type="text"
                              value={getArrayValueAsString(filter.value)}
                              onChange={(e) => {
                                // 입력하는 동안은 문자열 그대로 저장
                                updateFilterValue(index, e.target.value);
                              }}
                              onBlur={(e) => {
                                // focus를 벗어날 때 배열로 변환
                                const arrayValue = parseStringToArray(e.target.value);
                                updateFilterValue(index, arrayValue);
                              }}
                              className="w-full px-2 py-1.5 text-xs border border-gray-300 rounded focus:ring-1 focus:ring-blue-500"
                              placeholder="쉼표(,)로 구분 (예: VIP, VVIP)"
                            />
                            {Array.isArray(filter.value) && filter.value.length > 0 && (
                              <p className="text-xs text-gray-500 mt-0.5">
                                {filter.value.length}개: {filter.value.join(", ")}
                              </p>
                            )}
                          </div>
                        ) : (
                          <input
                            type="text"
                            value={typeof filter.value === "string" ? filter.value : ""}
                            onChange={(e) => updateFilterValue(index, e.target.value)}
                            className="w-full px-2 py-1.5 text-xs border border-gray-300 rounded focus:ring-1 focus:ring-blue-500"
                            placeholder="값을 입력하세요"
                          />
                        )}
                      </div>

                      {/* 삭제 버튼 */}
                      <button
                        type="button"
                        onClick={() => removeEntityFilter(index)}
                        className="text-red-600 hover:text-red-700 mt-1"
                      >
                        <TrashIcon className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* 오른쪽: JSON 미리보기 */}
        <div className="border border-gray-300 rounded-xl p-4 bg-white">
          <h4 className="text-sm font-semibold text-gray-700 mb-3">JSON 미리보기</h4>
          <pre className="text-xs font-mono text-gray-800 bg-gray-50 p-3 rounded border border-gray-200 overflow-x-auto max-h-60">
            {entityFilters.length > 0
              ? JSON.stringify(entityFilters, null, 2)
              : "[]"}
          </pre>
        </div>
      </div>
      {validationResult && (
        <div className={`mt-3 p-3 rounded-lg text-sm ${
          validationResult.valid
            ? "bg-green-50 text-green-800 border border-green-200"
            : "bg-red-50 text-red-800 border border-red-200"
        }`}>
          <div className="flex items-center gap-2">
            {validationResult.valid ? (
              <CheckCircleIcon className="w-5 h-5 text-green-600" />
            ) : (
              <span className="text-red-600">⚠️</span>
            )}
            <span>{validationResult.message}</span>
          </div>
        </div>
      )}
      <p className="mt-2 text-xs text-gray-500">
        엔티티 필터는 이벤트 처리 시 조건으로 사용됩니다. 검증 버튼으로 저장 가능 여부를 확인하세요.
      </p>
    </div>
  );
}

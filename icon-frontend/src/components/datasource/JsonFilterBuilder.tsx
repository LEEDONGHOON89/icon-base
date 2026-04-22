"use client";

// [2026-04-22] JSONB 동적 필터 행 UI — TODO-001/002 공통 재사용 컴포넌트
import { PlusIcon, XMarkIcon } from "@heroicons/react/24/outline";

// [2026-04-22] crypto.randomUUID는 HTTPS(보안 컨텍스트)에서만 사용 가능
// HTTP 환경 호환을 위한 폴백 UUID 생성 함수
function generateId(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  // 폴백: Math.random 기반 UUID v4 형식
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

export interface JsonFilter {
  id: string;
  field: string;
  op: "eq" | "like" | "neq";
  value: string;
}

/** JsonFilter[] → API용 "key:op:value" 문자열 배열로 변환 */
export function toJsonFilterStrings(filters: JsonFilter[]): string[] {
  return filters
    .filter((f) => f.field.trim() && f.value.trim())
    .map((f) => `${f.field.trim()}:${f.op}:${f.value}`);
}

interface JsonFilterBuilderProps {
  filters: JsonFilter[];
  onChange: (filters: JsonFilter[]) => void;
  fieldHints?: string[];
  label?: string;
}

export default function JsonFilterBuilder({
  filters,
  onChange,
  fieldHints = ["cust_no", "account_id", "transaction_id"],
  label = "필드 필터",
}: JsonFilterBuilderProps) {
  const addFilter = () => {
    onChange([
      ...filters,
      { id: generateId(), field: "", op: "eq", value: "" },
    ]);
  };

  const updateFilter = (id: string, patch: Partial<JsonFilter>) => {
    onChange(filters.map((f) => (f.id === id ? { ...f, ...patch } : f)));
  };

  const removeFilter = (id: string) => {
    onChange(filters.filter((f) => f.id !== id));
  };

  return (
    <div className="border border-gray-200 rounded-lg p-3 bg-gray-50">
      <div className="flex items-center justify-between mb-2">
        <span className="text-sm font-medium text-gray-600">{label}</span>
        <button
          type="button"
          onClick={addFilter}
          className="inline-flex items-center gap-1 text-xs text-blue-600 hover:text-blue-700 font-medium"
        >
          <PlusIcon className="h-3.5 w-3.5" />
          필터 추가
        </button>
      </div>

      {filters.length === 0 && (
        <p className="text-xs text-gray-400 italic py-1">
          + 버튼으로 JSONB 내부 필드를 검색할 수 있습니다
        </p>
      )}

      <div className="space-y-2">
        {filters.map((f) => (
          <div key={f.id} className="flex items-center gap-2">
            {/* 필드명 입력 (datalist 자동완성) */}
            <input
              list={`fh-${f.id}`}
              value={f.field}
              onChange={(e) => updateFilter(f.id, { field: e.target.value })}
              placeholder="필드명"
              className="flex-1 min-w-0 text-xs border border-gray-300 rounded px-2 py-1.5 bg-white focus:ring-1 focus:ring-blue-400 focus:outline-none"
            />
            <datalist id={`fh-${f.id}`}>
              {fieldHints.map((h) => (
                <option key={h} value={h} />
              ))}
            </datalist>

            {/* 연산자 선택 */}
            <select
              value={f.op}
              onChange={(e) =>
                updateFilter(f.id, { op: e.target.value as JsonFilter["op"] })
              }
              className="text-xs border border-gray-300 rounded px-2 py-1.5 bg-white focus:ring-1 focus:ring-blue-400 focus:outline-none"
            >
              <option value="eq">= (일치)</option>
              <option value="like">LIKE (포함)</option>
              <option value="neq">≠ (불일치)</option>
            </select>

            {/* 값 입력 */}
            <input
              value={f.value}
              onChange={(e) => updateFilter(f.id, { value: e.target.value })}
              placeholder="검색값"
              className="flex-1 min-w-0 text-xs border border-gray-300 rounded px-2 py-1.5 bg-white focus:ring-1 focus:ring-blue-400 focus:outline-none"
            />

            {/* 삭제 버튼 */}
            <button
              type="button"
              onClick={() => removeFilter(f.id)}
              className="text-gray-400 hover:text-red-500 transition-colors"
            >
              <XMarkIcon className="h-4 w-4" />
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { addExcludedValue, deleteExcludedValue, fetchExcludedValues, reloadExcludedCache, updateExcludedValue, type ExcludedValue } from "../api";

export default function ExcludedValuesPage() {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({ queryKey: ["engine-settings", "excluded"], queryFn: () => fetchExcludedValues({}) });
  const rows = (data || []) as ExcludedValue[];

  const [newValue, setNewValue] = useState("");

  const onAdd = async () => {
    if (!newValue.trim()) return alert("값을 입력하세요.");
    await addExcludedValue(newValue.trim(), true);
    setNewValue("");
    await qc.invalidateQueries({ queryKey: ["engine-settings", "excluded"] });
  };

  const onToggle = async (row: ExcludedValue) => {
    await updateExcludedValue(row.id, { isActive: !row.isActive });
    await qc.invalidateQueries({ queryKey: ["engine-settings", "excluded"] });
  };

  const onDelete = async (row: ExcludedValue) => {
    if (!confirm(`삭제하시겠습니까? ${row.value}`)) return;
    await deleteExcludedValue(row.id);
    await qc.invalidateQueries({ queryKey: ["engine-settings", "excluded"] });
  };

  const onReload = async () => {
    await reloadExcludedCache();
    alert("엔진 캐시를 리로드했습니다.");
  };

  return (
    <div className="max-w-4xl mx-auto py-8">
      <div className="bg-white rounded-2xl shadow border">
        <div className="px-6 py-4 border-b bg-gray-50 rounded-t-2xl flex items-center justify-between">
          <div>
            <h1 className="text-xl font-semibold">제외값 관리</h1>
            <p className="text-gray-500 text-sm">그룹핑/파생 계산에서 제외할 문자열 목록</p>
          </div>
          <button onClick={onReload} className="px-3 py-1.5 bg-indigo-600 text-white rounded">엔진 캐시 리로드</button>
        </div>
        <div className="p-6">
          <div className="flex gap-2 mb-4">
            <input className="flex-1 px-3 py-2 border rounded" placeholder="예: 내계좌" value={newValue} onChange={e => setNewValue(e.target.value)} />
            <button onClick={onAdd} className="px-3 py-2 bg-blue-600 text-white rounded">추가</button>
          </div>
          {isLoading ? (
            <div className="text-gray-500">불러오는 중…</div>
          ) : rows.length === 0 ? (
            <div className="text-gray-500">등록된 제외값이 없습니다.</div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-500">
                  <th className="p-2">ID</th>
                  <th className="p-2">값</th>
                  <th className="p-2">활성</th>
                  <th className="p-2 text-right">액션</th>
                </tr>
              </thead>
              <tbody>
                {rows.map(r => (
                  <tr key={r.id} className="border-t">
                    <td className="p-2">{r.id}</td>
                    <td className="p-2">{r.value}</td>
                    <td className="p-2">{r.isActive ? "Y" : "N"}</td>
                    <td className="p-2 text-right space-x-2">
                      <button onClick={() => onToggle(r)} className="px-3 py-1.5 border rounded">{r.isActive ? "비활성" : "활성"}</button>
                      <button onClick={() => onDelete(r)} className="px-3 py-1.5 bg-red-600 text-white rounded">삭제</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

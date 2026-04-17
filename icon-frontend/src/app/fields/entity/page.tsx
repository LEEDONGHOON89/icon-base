"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchEntityFields } from "../api";
import type { EntityField } from "../api";
import { useState } from "react";
import { MagnifyingGlassIcon } from "@heroicons/react/24/outline";

// 데이터 타입별 색상 매핑
const getDataTypeColor = (dataType: string) => {
  const colorMap: Record<string, string> = {
    STRING: "bg-blue-100 text-blue-700",
    NUMBER: "bg-green-100 text-green-700",
    BOOLEAN: "bg-purple-100 text-purple-700",
    DATE: "bg-orange-100 text-orange-700",
    DATETIME: "bg-pink-100 text-pink-700",
    TIMESTAMP: "bg-indigo-100 text-indigo-700",
    ARRAY: "bg-yellow-100 text-yellow-700",
    OBJECT: "bg-red-100 text-red-700",
  };
  return colorMap[dataType] || "bg-gray-100 text-gray-700";
};

export default function EntityFieldsPage() {
  const [searchTerm, setSearchTerm] = useState("");

  // 엔티티 필드 조회
  const { data: entityFields = [], isLoading } = useQuery({
    queryKey: ["entityFields"],
    queryFn: fetchEntityFields,
  });

  // 컬럼명(entityFieldId)으로 정렬 및 검색 필터링
  const sortedFields = [...entityFields]
    .filter((field) => field && field.entityFieldId)
    .filter((field) => {
      if (!searchTerm) return true;
      const search = searchTerm.toLowerCase();
      return (
        field.entityFieldId.toLowerCase().includes(search) ||
        field.displayName.toLowerCase().includes(search)
      );
    })
    .sort((a, b) => a.entityFieldId.localeCompare(b.entityFieldId));

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
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">엔티티 필드</h1>
          <p className="text-sm text-gray-500 mt-1">
            시스템에서 사용하는 엔티티 필드를 조회합니다. (총 {sortedFields.length}개)
          </p>
        </div>
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
                  <th className="px-4 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider w-40">
                    필드 ID
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider w-64">
                    표시명
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider w-32">
                    데이터 타입
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider w-24">
                    활성
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                    설명
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {sortedFields.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-6 py-12 text-center text-gray-500">
                      엔티티 필드가 없습니다.
                    </td>
                  </tr>
                ) : (
                  sortedFields.map((field) => (
                    <tr
                      key={field.entityFieldId}
                      className="hover:bg-gray-50 transition-colors"
                    >
                      <td className="px-4 py-4 text-sm font-mono text-gray-900 w-40">
                        {field.entityFieldId}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-900 w-64">
                        {field.displayName}
                      </td>
                      <td className="px-6 py-4 text-sm w-32">
                        <span className={`px-3 py-1.5 rounded text-xs font-semibold whitespace-nowrap ${getDataTypeColor(field.dataType)}`}>
                          {field.dataType}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm w-24">
                        {field.isActive ? (
                          <span className="px-3 py-1.5 bg-green-100 text-green-700 rounded text-xs font-semibold whitespace-nowrap inline-block">
                            활성
                          </span>
                        ) : (
                          <span className="px-3 py-1.5 bg-gray-100 text-gray-600 rounded text-xs font-semibold whitespace-nowrap inline-block">
                            비활성
                          </span>
                        )}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-600">
                        {field.description || "-"}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
      </div>
    </div>
  );
}

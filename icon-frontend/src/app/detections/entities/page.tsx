"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { fetchDetectedScenarios } from "@/app/detections/api";
import { MagnifyingGlassIcon, UserCircleIcon } from "@heroicons/react/24/outline";

export default function EntitiesPage() {
  const router = useRouter();
  const [searchKey, setSearchKey] = useState("");

  // 최근 탐지된 엔티티 목록 (시나리오 결과에서 group_key 추출)
  const { data: recentScenarios } = useQuery({
    queryKey: ["recentEntities"],
    queryFn: () => fetchDetectedScenarios({ limit: 50 }),
  });

  // 중복 제거된 그룹키 목록
  const uniqueGroupKeys = recentScenarios
    ? Array.from(new Set(recentScenarios.map((s) => s.groupKey)))
        .slice(0, 20)
    : [];

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchKey.trim()) {
      router.push(`/detections/entity-history?entityId=${encodeURIComponent(searchKey.trim())}`);
    }
  };

  const handleEntityClick = (groupKey: string) => {
    router.push(`/detections/entity-history?entityId=${encodeURIComponent(groupKey)}`);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50 p-8">
      <div className="max-w-7xl mx-auto">
        {/* 헤더 */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-800 mb-2">엔티티 조회</h1>
          <p className="text-gray-600">
            group_key를 입력하여 엔티티 속성과 이벤트 이력을 확인하세요
          </p>
        </div>

        {/* 검색 폼 */}
        <div className="bg-white rounded-3xl shadow-xl p-8 mb-8">
          <h2 className="text-xl font-bold text-gray-800 mb-4">그룹키로 검색</h2>
          <form onSubmit={handleSearch} className="flex gap-4">
            <div className="flex-1">
              <input
                type="text"
                value={searchKey}
                onChange={(e) => setSearchKey(e.target.value)}
                placeholder="group_key 입력 (예: CUS001, ACC123)"
                className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
              />
            </div>
            <button
              type="submit"
              disabled={!searchKey.trim()}
              className="px-6 py-3 bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-xl hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-lg flex items-center gap-2"
            >
              <MagnifyingGlassIcon className="h-5 w-5" />
              조회
            </button>
          </form>
        </div>

        {/* 최근 탐지된 엔티티 목록 */}
        <div className="bg-white rounded-3xl shadow-xl p-8">
          <h2 className="text-xl font-bold text-gray-800 mb-6">
            최근 탐지된 엔티티
          </h2>

          {uniqueGroupKeys.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {uniqueGroupKeys.map((groupKey) => (
                <button
                  key={groupKey}
                  onClick={() => handleEntityClick(groupKey)}
                  className="p-4 border border-gray-200 rounded-xl hover:border-blue-500 hover:shadow-md transition-all duration-200 text-left group"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-lg flex items-center justify-center group-hover:scale-110 transition-transform">
                      <UserCircleIcon className="h-6 w-6 text-white" />
                    </div>
                    <div className="flex-1">
                      <div className="font-mono text-sm text-gray-900 font-medium">
                        {groupKey}
                      </div>
                      <div className="text-xs text-gray-500 mt-1">
                        클릭하여 상세 정보 보기
                      </div>
                    </div>
                  </div>
                </button>
              ))}
            </div>
          ) : (
            <div className="text-center py-12 text-gray-500">
              최근 탐지된 엔티티가 없습니다.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

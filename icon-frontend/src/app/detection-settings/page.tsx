"use client";

import Link from "next/link";
import { MapIcon } from "@heroicons/react/24/outline";
import { useQuery } from "@tanstack/react-query";
import { fetchAggregates, type AggregateDef, labelAggregateOperator } from "@/app/detections/api";

export default function DetectionSettingsPage() {
  const { data: aggregates, isLoading, error } = useQuery<AggregateDef[]>({
    queryKey: ["aggregates", "defs"],
    queryFn: fetchAggregates,
  });

  return (
    <div className="space-y-6">
      <div className="bg-gradient-to-r from-indigo-600 to-purple-600 rounded-2xl p-6 text-white shadow">
        <h1 className="text-2xl font-semibold">탐지설정</h1>
        <p className="text-indigo-100 mt-1">룰 관리를 통해 탐지 정책을 구성하세요.</p>
      </div>

      <div className="bg-white rounded-xl shadow p-5 border">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-lg bg-teal-50">
              <MapIcon className="h-5 w-5 text-teal-600" />
            </div>
            <h2 className="font-semibold">룰 목록</h2>
          </div>
        </div>
        <div className="overflow-auto">
          {isLoading && (
            <div className="p-4 text-gray-500">불러오는 중...</div>
          )}
          {error && (
            <div className="p-4 text-red-600">집계 목록을 불러오지 못했습니다. (네트워크/인증 확인)</div>
          )}
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-gray-500">
                <th className="p-2">ID</th>
                <th className="p-2">이름</th>
                <th className="p-2">연산자</th>
                <th className="p-2">윈도우(분)</th>
                <th className="p-2">활성</th>
              </tr>
            </thead>
            <tbody>
              {aggregates?.map((a) => (
                <tr key={a.ruleId} className="border-t">
                  <td className="p-2 font-mono">{a.ruleId}</td>
                  <td className="p-2">{a.name}</td>
                  <td className="p-2">{labelAggregateOperator(a.operator as any)}</td>
                  <td className="p-2">{a.windowMinutes ?? "-"}</td>
                  <td className="p-2">{a.isActive ? "Y" : "N"}</td>
                </tr>
              ))}
              {!aggregates?.length && (
                <tr>
                  <td className="p-4 text-gray-500" colSpan={5}>등록된 집계가 없습니다.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

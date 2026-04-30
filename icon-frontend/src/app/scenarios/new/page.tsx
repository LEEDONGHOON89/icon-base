"use client";

import { useRouter } from "next/navigation";
import { createScenario, CreateScenarioRequest, ScenarioOperator } from "../api";
import ScenarioForm, { ScenarioFormData } from "../ScenarioForm";
import toast from "react-hot-toast";
import { ChevronLeftIcon } from "@heroicons/react/24/outline";

export default function NewScenarioPage() {
  const router = useRouter();

  // 폼 제출 핸들러
  const handleSubmit = async (data: ScenarioFormData) => {
    // [2026-04-24] primaryEntityType 등 누락 필드 추가 — 미전달 시 DB NOT NULL 제약 위반 오류 발생
    const request: CreateScenarioRequest = {
      scenarioName: data.scenarioName,
      description: data.description,
      riskLevelId: data.riskLevelId || undefined,
      detectionAreaId: data.detectionAreaId || undefined,
      primaryEntityType: data.primaryEntityType || undefined,
      entityFilterJson: data.entityFilterJson || undefined,
      dedupMinutes: (data.dedupMinutes != null && !isNaN(data.dedupMinutes)) ? data.dedupMinutes : 0,
      rules: data.rules.map((rule) => ({
        ruleId: rule.ruleId,
        orderNo: rule.orderNo,
        operator: rule.operator as ScenarioOperator | undefined, // 첫 번째 집계는 operator가 undefined일 수 있음
      })),
    };

    const result = await createScenario(request);
    toast.success("시나리오가 생성되었습니다.");
    router.push(`/scenarios/${result.scenarioId}/edit`);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50 p-8">
      <div className="max-w-6xl mx-auto">
        {/* 헤더 */}
        <div className="mb-8">
          <button
            onClick={() => router.push("/scenarios")}
            className="flex items-center gap-2 text-gray-600 hover:text-gray-800 transition-colors mb-4"
          >
            <ChevronLeftIcon className="h-5 w-5" />
            시나리오 목록으로
          </button>
          <h1 className="text-3xl font-bold text-gray-800">새 시나리오 생성</h1>
          <p className="text-gray-600 mt-2">복수의 집계를 조합해 탐지 시나리오를 만들어보세요.</p>
        </div>

        {/* 시나리오 폼 */}
        <ScenarioForm
          onSubmit={handleSubmit}
          submitLabel="시나리오 생성"
        />
      </div>
    </div>
  );
}

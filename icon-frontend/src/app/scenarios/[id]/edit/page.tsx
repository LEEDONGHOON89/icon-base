"use client";

import { useRouter, useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { useState, useRef, useEffect } from "react";
import {
  fetchScenario,
  updateScenario,
  UpdateScenarioRequest,
  ScenarioOperator,
} from "../../api";
import ScenarioForm, { ScenarioFormData } from "../../ScenarioForm";
import toast from "react-hot-toast";
import { ChevronLeftIcon } from "@heroicons/react/24/outline";
import {
  DocumentTextIcon,
  FunnelIcon,
  Cog6ToothIcon,
  PuzzlePieceIcon,
  ChartBarIcon,
} from "@heroicons/react/24/outline";

// 목차 항목 인터페이스
interface TOCItem {
  id: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
}

// 목차 항목 정의
const tocItems: TOCItem[] = [
  { id: "basic-info", label: "기본 정보", icon: DocumentTextIcon },
  { id: "entity-filter", label: "엔티티 필터", icon: FunnelIcon },
  { id: "rule-config", label: "룰 구성", icon: PuzzlePieceIcon },
  { id: "visualization", label: "룰 구성 시각화", icon: ChartBarIcon },
];

export default function EditScenarioPage() {
  const router = useRouter();
  const params = useParams();
  const scenarioId = params.id as string;
  const [activeSection, setActiveSection] = useState("basic-info");

  // 섹션 ref
  const sectionRefs = useRef<{ [key: string]: HTMLDivElement | null }>({});

  // 시나리오 상세 정보 조회
  const { data: scenario, isLoading: scenarioLoading } = useQuery({
    queryKey: ["scenarios", scenarioId],
    queryFn: async () => {
      if (!scenarioId) throw new Error("시나리오 ID가 없습니다.");
      const result = await fetchScenario(scenarioId);
      console.log("📊 [SCENARIO EDIT] 시나리오 조회 결과:", result);
      console.log("📊 [SCENARIO EDIT] 룰 구성 (rules):", result.rules);
      return result;
    },
    enabled: !!scenarioId,
  });

  // 폼 제출 핸들러
  // [2026-04-24] riskLevelId / detectionAreaId / primaryEntityType / isActive / dedupMinutes 누락 필드 추가
  const handleSubmit = async (data: ScenarioFormData) => {
    if (!scenarioId) {
      toast.error("시나리오 ID가 없습니다.");
      return;
    }

    const request: UpdateScenarioRequest = {
      scenarioName: data.scenarioName,
      description: data.description,
      riskLevelId: data.riskLevelId || undefined,
      detectionAreaId: data.detectionAreaId || undefined,
      primaryEntityType: data.primaryEntityType || undefined,
      entityFilterJson: data.entityFilterJson,
      isActive: data.isActive,
      dedupMinutes: (data.dedupMinutes != null && !isNaN(data.dedupMinutes)) ? data.dedupMinutes : 0,
      rules: data.rules.map((rule) => ({
        ruleId: rule.ruleId,
        orderNo: rule.orderNo,
        operator: rule.operator,
      })),
    };

    await updateScenario(scenarioId, request);
    toast.success("시나리오가 수정되었습니다.");
  };

  // 목차 클릭 핸들러
  const handleTOCClick = (sectionId: string) => {
    setActiveSection(sectionId);
    const element = sectionRefs.current[sectionId];
    const main = document.querySelector('main');

    if (element && main) {
      const rect = element.getBoundingClientRect();
      const mainRect = main.getBoundingClientRect();
      const currentScroll = main.scrollTop;
      const relativeTop = rect.top - mainRect.top;

      main.scrollTo({
        top: currentScroll + relativeTop - 20,
        behavior: "smooth",
      });
    }
  };

  // 스크롤 이벤트로 active section 업데이트
  useEffect(() => {
    const main = document.querySelector('main');
    if (!main) return;

    const handleScroll = () => {
      for (const item of tocItems) {
        const element = sectionRefs.current[item.id];
        if (element) {
          const rect = element.getBoundingClientRect();
          const mainRect = main.getBoundingClientRect();

          // main 영역 내에서의 상대 위치
          const relativeTop = rect.top - mainRect.top;

          if (relativeTop >= 0 && relativeTop < 300) {
            setActiveSection(item.id);
            break;
          }
        }
      }
    };

    main.addEventListener("scroll", handleScroll);
    return () => main.removeEventListener("scroll", handleScroll);
  }, []);

  if (scenarioLoading) {
    return (
      <div className="p-6">
        <div className="flex justify-center items-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        </div>
      </div>
    );
  }

  if (!scenario) {
    return (
      <div className="p-6">
        <div className="text-center py-16">
          <p className="text-gray-500">시나리오를 찾을 수 없습니다.</p>
          <button
            onClick={() => router.push("/scenarios")}
            className="mt-4 text-blue-600 hover:text-blue-700"
          >
            시나리오 목록으로 돌아가기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* 헤더 */}
      <div className="mb-6">
        <button
          onClick={() => router.push(`/scenarios`)}
          className="flex items-center gap-2 text-gray-600 hover:text-gray-800 transition-colors mb-3"
        >
          <ChevronLeftIcon className="h-5 w-5" />
          시나리오 목록으로
        </button>
        <h1 className="text-2xl font-semibold text-gray-800">시나리오 수정</h1>
        <p className="text-gray-600 mt-1">시나리오 정보와 룰 구성을 수정하세요</p>
      </div>

      {/* 메인 레이아웃 */}
      <div className="flex gap-6 min-h-screen">
        {/* 왼쪽 목차 - sticky (main 기준) */}
        <aside className="w-64 flex-shrink-0 sticky top-4 self-start">
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4 max-h-[calc(100vh-8rem)] overflow-y-auto">
            <nav className="space-y-1">
                {tocItems.map((item) => {
                  const Icon = item.icon;
                  const isActive = activeSection === item.id;
                  return (
                    <button
                      key={item.id}
                      onClick={() => handleTOCClick(item.id)}
                      className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg text-left transition-colors ${
                        isActive
                          ? "bg-blue-50 text-blue-700 font-medium"
                          : "text-gray-600 hover:bg-gray-50"
                      }`}
                    >
                      <Icon className={`h-5 w-5 ${isActive ? "text-blue-600" : "text-gray-400"}`} />
                      <span className="text-sm">{item.label}</span>
                    </button>
                  );
                })}
            </nav>
          </div>
        </aside>

          {/* 오른쪽 내용 */}
          <div className="flex-1 min-w-0 pb-6">
            <ScenarioForm
              scenario={scenario}
              onSubmit={handleSubmit}
              submitLabel="시나리오 수정"
              sectionRefs={sectionRefs}
            />
          </div>
        </div>
    </div>
  );
}

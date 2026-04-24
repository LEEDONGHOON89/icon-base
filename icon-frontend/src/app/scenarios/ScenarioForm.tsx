"use client";

import { useState, useEffect } from "react";
import { useForm, Controller } from "react-hook-form";
import { useQuery } from "@tanstack/react-query";
import {
  ScenarioOperator,
  ScenarioWithRules,
  updateBasicInfo,
  updateEntityFilter,
  updateScenarioRules,
  fetchRiskLevels,
  fetchEntityTypes,
  type RiskLevelMetadata,
  type EntityTypeMetadata,
} from "./api";
import { fetchActiveDetectionAreas, type DetectionArea } from "@/app/domain-settings/api";
// [2026-04-24] fetchAggregates(/api/v1/sensors)가 센서를 반환해 룰 목록이 비었던 버그 수정 → fetchActiveRules(/api/v1/rules/active) 로 교체
import { labelAggregateOperator } from "@/app/detections/api";
import { fetchActiveRules, type Rule } from "@/app/rules/api";

import { formatMinutes } from "@/utils/timeFormat";
import { useErrorHandling } from "@/hooks/useErrorHandling";
import Input from "@/components/common/Input";
import LoadingButton from "@/components/common/LoadingButton";
import toast from "react-hot-toast";
import { PlusIcon, TrashIcon } from "@heroicons/react/24/outline";
import EntityFilterEditor, { type EntityFilterCondition } from "./components/EntityFilterEditor";
import ScenarioVisualization from "./components/ScenarioVisualization";

export interface ScenarioFormData {
  scenarioName: string;
  description?: string;
  riskLevelId?: string;
  detectionAreaId?: string;
  primaryEntityType?: string;
  entityFilterJson?: string;
  isActive: boolean;
  // 엔진 설정 필드
  dedupMinutes?: number; // 중복 제거 창 (분)
  rules: {
    ruleId: string;
    operator?: ScenarioOperator;  // 첫 번째 룰은 operator가 없을 수 있음
    orderNo?: number;
  }[];
}

interface ScenarioFormProps {
  scenario?: ScenarioWithRules;
  onSubmit: (data: ScenarioFormData) => Promise<void>;
  submitLabel: string;
  sectionRefs?: React.MutableRefObject<{ [key: string]: HTMLDivElement | null }>;
}

export default function ScenarioForm({
  scenario,
  onSubmit,
  submitLabel,
  sectionRefs,
}: ScenarioFormProps) {
  const { handleError } = useErrorHandling();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSavingBasicInfo, setIsSavingBasicInfo] = useState(false);
  const [isSavingEntityFilter, setIsSavingEntityFilter] = useState(false);
  const [isSavingRules, setIsSavingRules] = useState(false);
  const [ruleFilter, setRuleFilter] = useState("");
  const [entityFilters, setEntityFilters] = useState<EntityFilterCondition[]>([]);

  // [2026-04-24] /api/v1/rules/active 에서 활성화된 룰 목록 조회
  const { data: availableAggregates = [], isLoading: aggregatesLoading } = useQuery({
    queryKey: ["rules", "active"],
    queryFn: async () => {
      const res = await fetchActiveRules();
      return res.data || [];
    },
  });

  // 위험 레벨 메타데이터 조회
  const { data: riskLevels = [] } = useQuery({
    queryKey: ["metadata", "riskLevels"],
    queryFn: fetchRiskLevels,
    staleTime: 5 * 60 * 1000, // 5분간 캐시
  });

  // 엔티티 타입 메타데이터 조회
  const { data: entityTypes = [] } = useQuery({
    queryKey: ["metadata", "entityTypes"],
    queryFn: fetchEntityTypes,
    staleTime: 5 * 60 * 1000, // 5분간 캐시
  });

  // 탐지 영역 조회
  const { data: detectionAreas = [] } = useQuery({
    queryKey: ["detectionAreas", "active"],
    queryFn: fetchActiveDetectionAreas,
    staleTime: 5 * 60 * 1000, // 5분간 캐시
  });

  // 🔍 디버깅: availableAggregates 로드 확인
  useEffect(() => {
    if (availableAggregates.length > 0) {
      console.log("🔍 [ScenarioForm] availableAggregates loaded:", {
        count: availableAggregates.length,
        aggregateIds: availableAggregates.map((a: any) => a.ruleId || a.id),
        hasAGG_ATM_NIGHT_1X: availableAggregates.some((a: any) => 
          (a.ruleId === "AGG_ATM_NIGHT_1X" || a.id === "AGG_ATM_NIGHT_1X")
        ),
        firstItem: availableAggregates[0],
      });
    }
  }, [availableAggregates]);



  const {
    register,
    control,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors },
  } = useForm<ScenarioFormData>({
    defaultValues: {
      scenarioName: "",
      description: "",
      riskLevelId: "",
      detectionAreaId: "",
      primaryEntityType: "",
      entityFilterJson: "",
      isActive: true,
      // 엔진 설정 필드
      dedupMinutes: undefined,
      rules: [],
    },
  });

  // 메타데이터 로딩 상태 확인
  const isMetadataLoaded = riskLevels.length > 0 && entityTypes.length > 0 && detectionAreas.length > 0;

  // 시나리오 데이터 로드 시 폼 초기화 (수정 모드)
  // 메타데이터가 모두 로드된 후에만 reset 호출 (select option이 렌더링된 후 값 설정)
  useEffect(() => {
    if (scenario && isMetadataLoaded) {
      console.log("🔍 [ScenarioForm] Initializing form with scenario (metadata loaded):", {
        scenarioId: scenario.scenarioId,
        riskLevelId: scenario.riskLevelId,
        detectionAreaId: scenario.detectionAreaId,
        primaryEntityType: scenario.primaryEntityType,
        riskLevelsCount: riskLevels.length,
        entityTypesCount: entityTypes.length,
        detectionAreasCount: detectionAreas.length,
      });

      // reset을 사용하여 전체 폼 값을 한 번에 설정 (DOM 업데이트 보장)
      reset({
        scenarioName: scenario.scenarioName,
        description: scenario.description || "",
        riskLevelId: scenario.riskLevelId || "",
        detectionAreaId: scenario.detectionAreaId || "",
        primaryEntityType: scenario.primaryEntityType || "",
        entityFilterJson: scenario.entityFilterJson || "",
        isActive: scenario.isActive,
        // 엔진 설정 필드
        dedupMinutes: scenario.dedupMinutes ?? undefined,
        rules: scenario.rules?.map((rule, index) => ({
          ruleId: rule.ruleId,
          operator: index === 0 ? undefined : ((rule as any).operator ?? (rule as any).scenarioOperator),
          orderNo: rule.orderNo,
        })) || [],
      });

      // entityFilterJson을 파싱하여 entityFilters 상태로 설정
      if (scenario.entityFilterJson) {
        try {
          const parsed = JSON.parse(scenario.entityFilterJson);
          console.log("Parsed entityFilterJson:", parsed);
          // 배열 형식인 경우
          if (Array.isArray(parsed)) {
            setEntityFilters(parsed as EntityFilterCondition[]);
          }
          // 객체 형식인 경우 (빈 배열로 초기화)
          else {
            console.warn("entityFilterJson is not an array, resetting to empty array");
            setEntityFilters([]);
          }
        } catch (e) {
          console.error("Failed to parse entityFilterJson:", e);
          setEntityFilters([]);
        }
      } else {
        setEntityFilters([]);
      }
    }
  }, [scenario, reset, isMetadataLoaded, riskLevels.length, entityTypes.length, detectionAreas.length]);

  // entityFilters 변경 시 JSON으로 변환하여 폼에 반영
  useEffect(() => {
    const json = entityFilters.length > 0
      ? JSON.stringify(entityFilters, null, 2)
      : "";
    setValue("entityFilterJson", json);
  }, [entityFilters, setValue]);

  const watchedRules = watch("rules");



  // 룰 추가 (사용 가능한 룰에서 선택)
  const addRule = (agg: Rule) => {
    const currentRules = watch("rules");

    // 이미 추가된 룰인지 확인
    if (currentRules.some((r) => r.ruleId === agg.ruleId)) {
      toast.error("이미 추가된 룰입니다.");
      return;
    }

    const newRule = {
      ruleId: agg.ruleId,
      operator:
        currentRules.length === 0
          ? undefined  // 첫 번째 룰은 operator 없음
          : ("AND" as ScenarioOperator),  // 두 번째 이후는 AND가 기본값
      orderNo: currentRules.length + 1,
    };

    setValue("rules", [...currentRules, newRule]);
    toast.success(`"${agg.name}" 룰이 추가되었습니다.`);
  };

  // 룰 제거
  const removeRule = (index: number) => {
    const currentRules = watch("rules");
    const updatedRules = currentRules.filter((_, i) => i !== index);

    // 순서 재정렬
    updatedRules.forEach((rule, i) => {
      rule.orderNo = i + 1;
    });

    setValue("rules", updatedRules);
  };

  // [2026-04-24] Rule 타입으로 변경, sensorName 필드 제거 (Rule은 name 필드 사용)
  const aggsArray = Array.isArray(availableAggregates) ? availableAggregates : [];
  const filteredRules = (aggsArray as Rule[]).filter((agg: Rule) => {
    const searchLower = ruleFilter.toLowerCase();
    const name = agg.name || '';
    const ruleId = agg.ruleId || '';
    return name.toLowerCase().includes(searchLower) || ruleId.toLowerCase().includes(searchLower);
  });

  // 폼 제출 (전체 저장 - 생성 모드에서만 사용)
  const handleFormSubmit = async (data: ScenarioFormData) => {

    // 가용 룰 검증: 존재하지 않는 룰 ID가 포함되면 저장 중단
    const validIds = new Set((aggsArray as Rule[]).map((a) => a.ruleId));
    const invalid = data.rules.find((r) => !validIds.has(r.ruleId));
    if (invalid) {
      toast.error(`정의되지 않은 룰이 포함되어 저장할 수 없습니다: ${invalid.ruleId}`);
      return;
    }
    if (data.rules.length === 0) {
      toast.error("최소 1개 이상의 룰을 추가해주세요.");
      return;
    }

    setIsSubmitting(true);
    console.log("📤 Submitting data:", data);
    console.log("📤 entityFilterJson:", data.entityFilterJson);
    try {
      await onSubmit(data);
    } catch (error) {
      handleError(error);
    } finally {
      setIsSubmitting(false);
    }
  };

  // 기본정보 저장 (수정 모드에서만 사용)
  const handleSaveBasicInfo = async (e: React.MouseEvent) => {
    e.preventDefault(); // 폼 제출 방지
    if (!scenario) return;

    const scenarioName = watch("scenarioName");
    const description = watch("description");
    const riskLevelId = watch("riskLevelId");
    const detectionAreaId = watch("detectionAreaId");
    const primaryEntityType = watch("primaryEntityType");
    const isActive = watch("isActive");
    // 엔진 설정 필드
    const dedupMinutes = watch("dedupMinutes");

    if (!scenarioName.trim()) {
      toast.error("시나리오 이름은 필수입니다.");
      return;
    }

    setIsSavingBasicInfo(true);
    try {
      await updateBasicInfo(scenario.scenarioId, {
        scenarioName,
        description,
        riskLevelId: riskLevelId || undefined,
        detectionAreaId: detectionAreaId || undefined,
        primaryEntityType: primaryEntityType || undefined,
        isActive,
        // 엔진 설정 필드 (NaN 처리)
        dedupMinutes: dedupMinutes && !isNaN(dedupMinutes) ? dedupMinutes : undefined,
      });
      toast.success("기본정보가 저장되었습니다.");
    } catch (error) {
      handleError(error);
    } finally {
      setIsSavingBasicInfo(false);
    }
  };

  // 엔티티 필터 저장 (수정 모드에서만 사용)
  const handleSaveEntityFilter = async (e: React.MouseEvent) => {
    e.preventDefault(); // 폼 제출 방지
    if (!scenario) return;

    const entityFilterJson = watch("entityFilterJson");

    setIsSavingEntityFilter(true);
    try {
      await updateEntityFilter(scenario.scenarioId, {
        entityFilterJson,
      });
      toast.success("엔티티 필터가 저장되었습니다.");
    } catch (error) {
      handleError(error);
    } finally {
      setIsSavingEntityFilter(false);
    }
  };

  // 룰 구성 저장 (수정 모드에서만 사용)
  const handleSaveRules = async (e: React.MouseEvent) => {
    e.preventDefault(); // 폼 제출 방지
    if (!scenario) return;

    const rules = watch("rules");

    if (rules.length === 0) {
      toast.error("최소 1개 이상의 룰을 추가해주세요.");
      return;
    }

    // 가용 룰 검증
    const validIds = new Set((aggsArray as AggregateDef[]).map((a) => a.ruleId));
    const invalid = rules.find((r) => !validIds.has(r.ruleId));
    if (invalid) {
      toast.error(`정의되지 않은 룰이 포함되어 저장할 수 없습니다: ${invalid.ruleId}`);
      return;
    }

    setIsSavingRules(true);
    try {
      await updateScenarioRules(scenario.scenarioId, {
        rules: rules.map((r) => ({
          ruleId: r.ruleId,
          operator: r.operator || "AND",
          orderNo: r.orderNo,
        })),
      });
      toast.success("룰 구성이 저장되었습니다.");
    } catch (error) {
      handleError(error);
    } finally {
      setIsSavingRules(false);
    }
  };

  return (
    <div className="space-y-8">
      <form onSubmit={handleSubmit(handleFormSubmit)}>
        {/* 상단: 기본 정보 */}
        <div
          ref={(el) => { if (sectionRefs) sectionRefs.current['basic-info'] = el; }}
          id="basic-info"
          className="bg-white rounded-3xl shadow-xl p-8"
        >
          <div className="flex justify-between items-start mb-6">
            <h2 className="text-xl font-bold text-gray-800">기본 정보</h2>
            <div className="flex gap-3">
              {scenario && (
                <LoadingButton
                  type="button"
                  loading={isSavingBasicInfo}
                  onClick={handleSaveBasicInfo}
                  className="px-4 py-2 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors shadow-md"
                >
                  저장
                </LoadingButton>
              )}
              {!scenario && (
                <LoadingButton
                  type="submit"
                  loading={isSubmitting}
                  className="px-6 py-2.5 bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-xl hover:scale-105 transition-all duration-200 shadow-lg"
                >
                  {submitLabel}
                </LoadingButton>
              )}
            </div>
          </div>
          <div className="space-y-6">
            <Input
              label="시나리오 이름"
              {...register("scenarioName", {
                required: "시나리오 이름은 필수입니다.",
              })}
              error={errors.scenarioName?.message}
              placeholder="예: 고액 이체 감지"
            />
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                설명 (선택)
              </label>
              <textarea
                {...register("description")}
                className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                rows={2}
                placeholder="시나리오에 대한 설명을 입력하세요."
              />
            </div>

            {/* 위험 레벨, 탐지 영역, 주요 엔티티, 중복 제거 창 - 4열 그리드 */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              {/* 위험 레벨 - Controller로 제어 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  위험 레벨
                </label>
                <Controller
                  name="riskLevelId"
                  control={control}
                  render={({ field }) => (
                    <select
                      {...field}
                      className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white"
                    >
                      <option value="">선택 안함</option>
                      {riskLevels.map((level) => (
                        <option key={level.value} value={level.value}>
                          {level.label}
                        </option>
                      ))}
                    </select>
                  )}
                />
              </div>

              {/* 탐지 영역 - Controller로 제어 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  탐지 영역
                </label>
                <Controller
                  name="detectionAreaId"
                  control={control}
                  render={({ field }) => (
                    <select
                      {...field}
                      className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white"
                    >
                      <option value="">선택 안함</option>
                      {detectionAreas.map((area) => (
                        <option key={area.detectionAreaId} value={area.detectionAreaId}>
                          {area.areaName}
                        </option>
                      ))}
                    </select>
                  )}
                />
              </div>

              {/* 주요 엔티티 타입 - Controller로 제어 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  주요 엔티티
                </label>
                <Controller
                  name="primaryEntityType"
                  control={control}
                  render={({ field }) => (
                    <select
                      {...field}
                      className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white"
                    >
                      <option value="">선택 안함</option>
                      {entityTypes.map((type) => (
                        <option key={type.value} value={type.value}>
                          {type.label}
                        </option>
                      ))}
                    </select>
                  )}
                />
              </div>

              {/* 중복 제거 창 (분) */}
              <div
                ref={(el) => { if (sectionRefs) sectionRefs.current['engine-settings'] = el; }}
                id="engine-settings"
              >
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  중복 제거 창 (분)
                </label>
                <input
                  type="number"
                  min={0}
                  {...register("dedupMinutes", { valueAsNumber: true })}
                  className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="예: 60"
                />
                <p className="text-xs text-gray-500 mt-1">
                  동일 탐지 중복 방지 시간
                </p>
              </div>
            </div>

            {/* 활성화 상태 토글 */}
            <div className="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
              <div>
                <label className="block text-sm font-medium text-gray-700">
                  시나리오 활성화
                </label>
                <p className="text-xs text-gray-500 mt-1">
                  비활성화하면 이 시나리오는 탐지되지 않습니다
                </p>
              </div>
              <Controller
                name="isActive"
                control={control}
                render={({ field }) => (
                  <button
                    type="button"
                    onClick={() => field.onChange(!field.value)}
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                      field.value ? 'bg-blue-600' : 'bg-gray-300'
                    }`}
                  >
                    <span
                      className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                        field.value ? 'translate-x-6' : 'translate-x-1'
                      }`}
                    />
                  </button>
                )}
              />
            </div>
          </div>
        </div>

        {/* 엔티티 필터 */}
        <div
          ref={(el) => { if (sectionRefs) sectionRefs.current['entity-filter'] = el; }}
          id="entity-filter"
          className="bg-white rounded-3xl shadow-xl p-8 mt-8"
        >
          <div className="flex justify-between items-start mb-6">
            <h2 className="text-xl font-bold text-gray-800">엔티티 필터</h2>
            {scenario && (
              <LoadingButton
                type="button"
                loading={isSavingEntityFilter}
                onClick={handleSaveEntityFilter}
                className="px-4 py-2 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors shadow-md"
              >
                저장
              </LoadingButton>
            )}
          </div>
          <EntityFilterEditor
            value={entityFilters}
            onChange={setEntityFilters}
          />
        </div>

        {/* 하단: 룰 구성과 사용 가능한 룰 */}
        <div
          ref={(el) => { if (sectionRefs) sectionRefs.current['rule-config'] = el; }}
          id="rule-config"
          className="bg-white rounded-3xl shadow-xl p-8 mt-8"
        >
          <h2 className="text-xl font-bold text-gray-800 mb-6">룰 구성 및 관리</h2>

          <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
            {/* 왼쪽: 룰 구성 */}
            <div className="lg:col-span-2">
              <div className="flex justify-between items-start mb-4">
                <h3 className="text-lg font-semibold text-gray-700">룰 구성</h3>
                {scenario && (
                  <LoadingButton
                    type="button"
                    loading={isSavingRules}
                    onClick={handleSaveRules}
                    className="px-4 py-2 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors shadow-md text-sm"
                  >
                    저장
                  </LoadingButton>
                )}
              </div>

              {/* 룰 구성 스크롤 영역 */}
              <div className="h-[600px] overflow-y-auto pr-2 border border-gray-200 rounded-xl p-4 bg-gray-50">
                {watchedRules.length === 0 ? (
                  <div className="text-center py-12 text-gray-500">
                    <p className="mb-4">아직 추가된 룰이 없습니다.</p>
                    <p className="text-sm">
                      오른쪽의 사용 가능한 룰에서 선택하세요.
                    </p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    <Controller
                      name="rules"
                      control={control}
                      render={({ field }) => (
                        <>
                          {field.value.map((rule, index) => {
                            // 🔍 디버깅: 룰 매칭 확인
                            console.log(`🔍 [ScenarioForm] Rule #${index + 1} lookup:`, {
                              ruleId: rule.ruleId,
                              aggsArrayLength: aggsArray.length,
                              aggsArrayIds: (aggsArray as AggregateDef[]).map((a: AggregateDef) => a.ruleId),
                              hasMatchingId: (aggsArray as AggregateDef[]).some((a: AggregateDef) => a.ruleId === rule.ruleId),
                            });

                            const selectedAgg = (aggsArray as AggregateDef[]).find((a: AggregateDef) => a.ruleId === rule.ruleId);
                            
                            // 🔍 디버깅: 매칭 결과 확인
                            console.log(`🔍 [ScenarioForm] Rule #${index + 1} result:`, {
                              ruleId: rule.ruleId,
                              found: !!selectedAgg,
                              selectedAgg: selectedAgg,
                            });

                            const displayName = selectedAgg?.sensorName || selectedAgg?.name || rule.ruleId;
                            const isMissing = !selectedAgg;
                            return (
                              <div key={rule.ruleId} className="relative">
                                {/* 연산자 표시 (첫 번째 룰은 제외) */}
                                {index > 0 && (
                                  <div className="flex justify-center mb-4">
                                    <select
                                      value={rule.operator}
                                      onChange={(e) => {
                                        const updatedRules = [...field.value];
                                        updatedRules[index].operator = e.target
                                          .value as ScenarioOperator;
                                        field.onChange(updatedRules);
                                      }}
                                      className="px-4 py-2 bg-white border border-gray-300 rounded-xl text-sm font-bold focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                    >
                                      <option value="AND">그리고 (AND)</option>
                                      <option value="OR">또는 (OR)</option>
                                    </select>
                                  </div>
                                )}

                                <div className="border border-gray-200 rounded-xl p-4 hover:shadow-md transition-shadow">
                                  <div className="flex justify-between items-start">
                                    <div className="flex-1">
                                      <div className="flex items-center gap-2 mb-1">
                                        <span className="px-2 py-1 bg-gray-100 text-gray-700 rounded-lg text-xs font-semibold">
                                          #{index + 1}
                                        </span>
                                        <h3 className={`font-semibold text-sm ${isMissing ? "text-red-600" : "text-gray-800"}`}>
                                          {displayName}
                                        </h3>
                                      </div>
                                      <div className="mb-2">
                                        <span className="text-xs text-gray-500 font-mono">{rule.ruleId}</span>
                                      </div>

                                      {(selectedAgg || isMissing) && (
                                        <>
                                          <div className="space-y-1">
                                            <div className="bg-gray-100 rounded-lg px-3 py-1 font-mono text-xs">
                                              {selectedAgg ? (<>
                                                {labelAggregateOperator(selectedAgg.operator)}
                                                {selectedAgg.windowMinutes ? ` · ${formatMinutes(selectedAgg.windowMinutes)}` : ""}
                                              </>) : (<>룰 정의 없음</>)}
                                            </div>

                                            {/* 임계값 정보 표시 */}
                                            {selectedAgg && (
                                              <div className="flex flex-wrap gap-2 text-xs">
                                                {selectedAgg.thresholdAmount && (
                                                  <span className="px-2 py-1 bg-blue-50 text-blue-700 rounded-md font-semibold">
                                                    💰 {Number(selectedAgg.thresholdAmount).toLocaleString()}원 이상
                                                  </span>
                                                )}
                                                {selectedAgg.thresholdCount && (
                                                  <span className="px-2 py-1 bg-green-50 text-green-700 rounded-md font-semibold">
                                                    🔢 {Number(selectedAgg.thresholdCount).toLocaleString()}회 이상
                                                  </span>
                                                )}
                                              </div>
                                            )}
                                          </div>

                                        </>
                                      )}
                                    </div>
                                    <button
                                      type="button"
                                      onClick={() => removeRule(index)}
                                      className="ml-2 text-red-600 hover:text-red-700"
                                    >
                                      <TrashIcon className="h-4 w-4" />
                                    </button>
                                  </div>
                                </div>
                              </div>
                            );
                          })}
                        </>
                      )}
                    />
                  </div>
                )}
              </div>
            </div>

          {/* 오른쪽: 사용 가능한 룰 */}
          <div className="lg:col-span-3">
            <div className="flex flex-col">
              <h3 className="text-lg font-semibold text-gray-700 mb-4">
                사용 가능한 룰
              </h3>

              {/* 검색/필터 */}
              <div className="mb-4">
                <input
                  type="text"
                  value={ruleFilter}
                  onChange={(e) => setRuleFilter(e.target.value)}
                  placeholder="룰 검색..."
                  className="w-full px-4 py-2 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
                />
              </div>

              {/* 룰 목록 스크롤 영역 */}
              <div className="h-[600px] overflow-y-auto pr-2 space-y-3 border border-gray-200 rounded-xl p-4 bg-gray-50">
                {aggregatesLoading ? (
                  <div className="flex justify-center items-center py-12">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                  </div>
                ) : filteredRules.length === 0 ? (
                  <p className="text-center text-gray-500 py-8">
                    {ruleFilter ? "검색 결과가 없습니다." : "사용 가능한 룰이 없습니다."}
                  </p>
                ) : (
                  filteredRules.map((agg: Rule) => {
                    const isAdded = watchedRules.some(
                      (r) => r.ruleId === agg.ruleId
                    );

                    return (
                      <div
                        key={agg.ruleId}
                        className={`border rounded-xl p-3 cursor-pointer transition-all ${isAdded
                          ? "border-gray-300 bg-gray-50 opacity-50 cursor-not-allowed"
                          : "border-gray-200 hover:border-blue-400 hover:shadow-md"
                          }`}
                        onClick={() => !isAdded && addRule(agg)}
                      >
                        <div className="flex justify-between items-start">
                          <div className="flex-1">
                            <h4 className="font-semibold text-gray-800 text-sm mb-1">
                              {agg.name}
                            </h4>
                            <div className="mb-1">
                              <span className="text-xs text-gray-500 font-mono">{agg.ruleId}</span>
                            </div>
                            <div className="space-y-1">
                              <div className="bg-gray-100 rounded px-2 py-0.5 text-xs font-mono inline-block">
                                {labelAggregateOperator(agg.operator)}
                                {agg.windowMinutes ? ` · ${formatMinutes(agg.windowMinutes)}` : ""}
                              </div>

                              {/* 임계값 정보 표시 */}
                              <div className="flex flex-wrap gap-1 text-xs">
                                {agg.thresholdAmount && (
                                  <span className="px-1.5 py-0.5 bg-blue-50 text-blue-700 rounded font-semibold">
                                    💰 {Number(agg.thresholdAmount).toLocaleString()}원 이상
                                  </span>
                                )}
                                {agg.thresholdCount && (
                                  <span className="px-1.5 py-0.5 bg-green-50 text-green-700 rounded font-semibold">
                                    🔢 {Number(agg.thresholdCount).toLocaleString()}회 이상
                                  </span>
                                )}
                              </div>
                            </div>

                          </div>
                          {isAdded ? (
                            <span className="text-green-600 ml-2">
                              <svg
                                className="w-4 h-4"
                                fill="currentColor"
                                viewBox="0 0 20 20"
                              >
                                <path
                                  fillRule="evenodd"
                                  d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                                  clipRule="evenodd"
                                />
                              </svg>
                            </span>
                          ) : (
                            <PlusIcon className="w-4 h-4 text-blue-600 ml-2" />
                          )}
                        </div>
                        <div className="flex items-center gap-1 mt-2">
                          {/* 추가 메타 배지 필요 시 여기에 표시 */}
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          </div>
          </div>
        </div>

        {/* 룰 구성 시각화 */}
        {scenario && (
          <div
            ref={(el) => { if (sectionRefs) sectionRefs.current['visualization'] = el; }}
            id="visualization"
            className="mt-8"
          >
            <ScenarioVisualization
              scenarioId={scenario.scenarioId}
            />
          </div>
        )}
      </form>
    </div>
  );
}

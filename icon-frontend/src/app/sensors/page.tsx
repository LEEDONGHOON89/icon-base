"use client";

import LoadingButton from "@/components/common/LoadingButton";
import {
  BeakerIcon,
  ChartBarIcon,
  CogIcon,
  MagnifyingGlassIcon,
  PencilIcon,
  PlusIcon,
  ShieldCheckIcon,
  TagIcon,
  Bars3Icon,
  Squares2X2Icon,
  ArrowPathIcon,
  BoltIcon,
} from "@heroicons/react/24/outline";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import {
  createSensor,
  fetchSensorById,
  fetchSensors,
  Sensor,
  SensorCategory,
  SensorCreateRequest,
  SensorUpdateRequest,
  updateSensor,
} from "./api";
import Alert from "@/components/common/Alert";
import { useErrorHandling } from "@/hooks/useErrorHandling";
import { useQueryWithErrorHandling } from "@/hooks/useQueryWithErrorHandling";
import { useEffect } from "react";
import Modal from "../users/Modal";
import type { SensorFormValues } from "./SensorForm";
import SensorForm from "./SensorForm";

export default function SensorsPage() {
  const queryClient = useQueryClient();
  const [searchKeyword, setSearchKeyword] = useState("");
  const [activatingId, setActivatingId] = useState<string | null>(null);

  // 뷰 모드 상태 (localStorage에서 초기화)
  const [viewMode, setViewMode] = useState<"list" | "card">(() => {
    if (typeof window !== "undefined") {
      const saved = localStorage.getItem("sensorsViewMode");
      return (saved === "list" || saved === "card") ? saved : "list";
    }
    return "list";
  });

  // 뷰 모드 변경 시 localStorage에 저장
  const handleViewModeChange = (mode: "list" | "card") => {
    setViewMode(mode);
    if (typeof window !== "undefined") {
      localStorage.setItem("sensorsViewMode", mode);
    }
  };

  // 전체 센서 조회
  const {
    data: sensorsResponse,
    isLoading,
    errorQuery: queryError,
  } = useQueryWithErrorHandling({
    queryKey: ["sensors", "all"],
    queryFn: () => fetchSensors(),
  });

  const allSensors = sensorsResponse?.data || [];

  // 클라이언트에서 필터링
  const filteredSensors = allSensors.filter((sensor) => {
    const keywordMatch =
      !searchKeyword ||
      sensor.sensorId.toLowerCase().includes(searchKeyword.toLowerCase()) ||
      sensor.sensorName.toLowerCase().includes(searchKeyword.toLowerCase()) ||
      (sensor.description && sensor.description.toLowerCase().includes(searchKeyword.toLowerCase()));
    return keywordMatch;
  });

  const [editingSensor, setEditingSensor] = useState<Sensor | null>(null);
  const [editingSensorId, setEditingSensorId] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const { error: formError, handleError, clearError } = useErrorHandling();

  // 수정용 상세 Sensor 조회
  const { data: sensorDetail, isLoading: isLoadingSensorDetail } =
    useQueryWithErrorHandling({
      queryKey: ["sensor", editingSensorId],
      queryFn: () => fetchSensorById(editingSensorId!),
      enabled: !!editingSensorId,
    });

  // sensorDetail이 로드되면 editingSensor 상태 업데이트
  useEffect(() => {
    if (sensorDetail) {
      setEditingSensor(sensorDetail);
    }
  }, [sensorDetail]);

  // 생성
  const createMutation = useMutation({
    mutationFn: (data: SensorCreateRequest) => createSensor(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sensors"] });
      setShowForm(false);
    },
    onError: handleError,
  });

  // 수정
  const updateMutation = useMutation({
    mutationFn: ({
      sensorId,
      data,
    }: {
      sensorId: string;
      data: SensorUpdateRequest;
    }) => updateSensor(sensorId, data),
    onSuccess: (res: Sensor) => {
      queryClient.invalidateQueries({ queryKey: ["sensors"] });
      queryClient.setQueryData(["sensor", editingSensorId], res);
      setEditingSensor(null);
      setEditingSensorId(null);
      setShowForm(false);
    },
    onError: handleError,
  });

  // 폼 제출 핸들러
  const handleSubmit = (values: SensorFormValues) => {
    clearError();
    if (editingSensor) {
      const updateData: SensorUpdateRequest = {
        sensorName: values.sensorName,
        domain: (values as any).domain,
        operator: values.operator,
        value: values.value,
        description: values.description,
        whereJson: (values as any).whereJson,
      };
      updateMutation.mutate({
        sensorId: editingSensor.sensorId,
        data: updateData,
      });
    } else {
      const createData: SensorCreateRequest = {
        sensorId: (values as any).sensorId,
        sensorName: values.sensorName,
        domain: (values as any).domain,
        fieldName: values.fieldName,
        operator: values.operator,
        value: values.value,
        whereJson: (values as any).whereJson,
        description: values.description,
      };
      createMutation.mutate(createData);
    }
  };

  // 수정 버튼 클릭
  const handleEdit = (sensor: Sensor) => {
    setEditingSensorId(sensor.sensorId);
    setShowForm(true);
    clearError();
  };

  // 새 센서 등록 버튼 클릭
  const handleNew = () => {
    setEditingSensor(null);
    setEditingSensorId(null);
    setShowForm(true);
    clearError();
  };

  // 센서 활성화/비활성화 토글
  const handleToggleActive = async (sensor: Sensor) => {
    setActivatingId(sensor.sensorId);
    try {
      await updateMutation.mutateAsync({
        sensorId: sensor.sensorId,
        data: { isActive: !sensor.isActive },
      });
    } finally {
      setActivatingId(null);
    }
  };


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
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">센서 관리</h1>
          <p className="text-sm text-gray-500 mt-1">
            이벤트 탐지를 위한 센서를 관리합니다. (S_ 프리픽스)
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => queryClient.invalidateQueries({ queryKey: ["sensors"] })}
            className="inline-flex items-center gap-2 px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-lg transition-colors"
            title="새로고침"
          >
            <ArrowPathIcon className="h-5 w-5" />
            새로고침
          </button>
          <LoadingButton
            onClick={handleNew}
            type="button"
            size="small"
            className="inline-flex items-center gap-2 bg-blue-600 hover:bg-blue-700"
          >
            <PlusIcon className="h-5 w-5" />
            새 센서 등록
          </LoadingButton>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
        <div className="flex items-center justify-between gap-4">
          {/* Search */}
          <div className="relative flex-1 max-w-md">
            <MagnifyingGlassIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              type="text"
              placeholder="센서 ID, 센서명 또는 설명으로 검색..."
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          {/* 뷰 모드 토글 */}
          <div className="flex items-center gap-2 bg-white rounded-lg border border-gray-300 p-1">
            <button
              onClick={() => handleViewModeChange("list")}
              className={`p-2 rounded transition-colors ${
                viewMode === "list"
                  ? "bg-blue-100 text-blue-600"
                  : "text-gray-600 hover:bg-gray-100"
              }`}
              title="리스트 보기"
            >
              <Bars3Icon className="h-5 w-5" />
            </button>
            <button
              onClick={() => handleViewModeChange("card")}
              className={`p-2 rounded transition-colors ${
                viewMode === "card"
                  ? "bg-blue-100 text-blue-600"
                  : "text-gray-600 hover:bg-gray-100"
              }`}
              title="카드 보기"
            >
              <Squares2X2Icon className="h-5 w-5" />
            </button>
          </div>
        </div>
      </div>

      {/* 에러 표시 */}
      {(queryError || formError) && (
        <Alert message={`${(queryError || formError)?.message}`} />
      )}

      {/* Sensor List */}
      {!isLoading && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          {filteredSensors.length === 0 ? (
            <div className="text-center py-12">
              <BoltIcon className="mx-auto h-12 w-12 text-gray-400" />
              <h3 className="mt-2 text-sm font-medium text-gray-900">
                센서가 없습니다
              </h3>
              <p className="mt-1 text-sm text-gray-500">
                {searchKeyword ? "검색 결과가 없습니다." : "새 센서를 생성하여 시작하세요."}
              </p>
            </div>
          ) : viewMode === "card" ? (
            /* 카드 뷰 */
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredSensors.map((sensor) => (
                  <div
                    key={sensor.sensorId}
                    className="bg-white rounded-xl border border-gray-200 p-6 hover:shadow-lg hover:border-blue-300 transition-all duration-200"
                  >
                    {/* 카드 헤더 */}
                    <div className="flex items-start justify-between mb-4">
                      <div></div>
                      <div className="flex items-center gap-2">
                        {/* 수정 버튼 */}
                        <button
                          onClick={() => handleEdit(sensor)}
                          className="text-sm text-blue-600 hover:text-blue-700 font-medium"
                        >
                          수정
                        </button>
                        {/* 활성화 토글 */}
                        <div className="flex flex-col items-end">
                          <span
                            className={`text-xs font-medium mb-1 ${
                              sensor.isActive ? "text-green-600" : "text-gray-500"
                            }`}
                          >
                            {sensor.isActive ? "활성" : "비활성"}
                          </span>
                          <button
                            onClick={() => handleToggleActive(sensor)}
                            disabled={activatingId === sensor.sensorId}
                            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 ${
                              sensor.isActive ? "bg-green-500" : "bg-gray-300"
                            } ${
                              activatingId === sensor.sensorId
                                ? "opacity-50 cursor-not-allowed"
                                : "cursor-pointer"
                            }`}
                          >
                            <span
                              className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                                sensor.isActive ? "translate-x-6" : "translate-x-1"
                              }`}
                            />
                          </button>
                        </div>
                      </div>
                    </div>

                    {/* 센서 이름과 ID */}
                    <div className="mb-2">
                      <h3 className="text-lg font-bold text-gray-900 line-clamp-1">
                        {sensor.sensorName} ({sensor.sensorId})
                      </h3>
                    </div>

                    {/* 연산자 & 설명 */}
                    {(sensor.operatorLabel || sensor.description) && (
                      <div className="flex items-center gap-2 mb-4">
                        {sensor.operatorLabel && (
                          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800 flex-shrink-0">
                            {sensor.operatorLabel}
                          </span>
                        )}
                        {sensor.description && (
                          <p className="text-sm text-gray-600 line-clamp-1">
                            {sensor.description}
                          </p>
                        )}
                      </div>
                    )}

                    {/* whereJson 미리보기 */}
                    {sensor.whereJson && (
                      <div>
                        <div className="bg-gray-50 rounded-lg p-3 border border-gray-200">
                          <pre className="text-xs text-gray-700 overflow-x-auto whitespace-pre max-h-24 overflow-y-auto">
                            {JSON.stringify(JSON.parse(sensor.whereJson), null, 2)}
                          </pre>
                        </div>
                      </div>
                    )}
                  </div>
              ))}
            </div>
          ) : (
            /* 리스트 뷰 */
            <div className="grid gap-4">
              {filteredSensors.map((sensor) => (
                  <div
                    key={sensor.sensorId}
                    className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 hover:shadow-lg transition-shadow duration-200"
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex items-start space-x-4">
                        <div className="flex-1">
                          <h3 className="text-lg font-semibold text-gray-900">
                            {sensor.sensorName} ({sensor.sensorId})
                          </h3>

                          {/* 연산자 & 설명 */}
                          {(sensor.operatorLabel || sensor.description) && (
                            <div className="flex items-center gap-2 mt-2">
                              {sensor.operatorLabel && (
                                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800 flex-shrink-0">
                                  {sensor.operatorLabel}
                                </span>
                              )}
                              {sensor.description && (
                                <p className="text-sm text-gray-600">
                                  {sensor.description}
                                </p>
                              )}
                            </div>
                          )}
                        </div>
                      </div>
                      <div className="flex items-center space-x-3 ml-4">
                        {/* 활성화 토글 스위치 */}
                        <div className="flex items-center space-x-2">
                          <span
                            className={`text-xs font-medium ${
                              sensor.isActive ? "text-green-600" : "text-gray-500"
                            }`}
                          >
                            {sensor.isActive ? "활성" : "비활성"}
                          </span>
                          <button
                            onClick={() => handleToggleActive(sensor)}
                            disabled={activatingId === sensor.sensorId}
                            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 ${
                              sensor.isActive ? "bg-green-500" : "bg-gray-300"
                            } ${
                              activatingId === sensor.sensorId
                                ? "opacity-50 cursor-not-allowed"
                                : "cursor-pointer"
                            }`}
                            title={sensor.isActive ? "비활성화" : "활성화"}
                          >
                            <span
                              className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                                sensor.isActive ? "translate-x-6" : "translate-x-1"
                              }`}
                            />
                          </button>
                        </div>

                        <button
                          onClick={() => handleEdit(sensor)}
                          className="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
                          title="수정"
                        >
                          <PencilIcon className="h-5 w-5" />
                        </button>
                      </div>
                    </div>
                  </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* 모달 */}
      <Modal
        open={showForm}
        onClose={() => {
          setShowForm(false);
          setEditingSensor(null);
          setEditingSensorId(null);
          clearError();
        }}
        title={editingSensor ? "센서 수정" : "새 센서 등록"}
        size="2xl"
        tall
      >
        {isLoadingSensorDetail && editingSensorId ? (
          <div className="py-8 text-center text-gray-500">
            센서 정보를 불러오는 중...
          </div>
        ) : editingSensorId && !editingSensor ? (
          <div className="py-8">
            <div className="mt-4 text-center">
              <LoadingButton
                onClick={() => {
                  setShowForm(false);
                  setEditingSensorId(null);
                  setEditingSensor(null);
                }}
                type="button"
                variant="secondary"
              >
                닫기
              </LoadingButton>
            </div>
          </div>
        ) : (
          <SensorForm
            onSubmit={handleSubmit}
            defaultValues={
              editingSensor
                ? {
                  sensorId: editingSensor.sensorId,
                  sensorName: editingSensor.sensorName,
                  whereJson: (editingSensor as any).whereJson || "",
                  fieldName: editingSensor.condition?.fieldName || "",
                  operator: (editingSensor.condition?.operator || "") as any,
                  value: editingSensor.condition?.value ?? "",
                  description: editingSensor.description,
                }
                : {}
            }
            isEdit={!!editingSensor}
            isPending={createMutation.isPending || updateMutation.isPending}
            error={formError?.message}
          />
        )}
      </Modal>
    </div>
  );
}

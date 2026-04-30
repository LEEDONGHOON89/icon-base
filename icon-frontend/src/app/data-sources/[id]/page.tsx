"use client";

import { useParams, useRouter } from "next/navigation";
import { useState } from "react";
import { useQueryWithErrorHandling } from "@/hooks/useQueryWithErrorHandling";
import { fetchDataSource, updateDataSource, fetchDataProfiles, fetchDataSourceOriginalSchemas } from "../api";
import { fetchDataSourceTypes } from "@/app/metadata/api";
import LoadingButton from "@/components/common/LoadingButton";
import {
  ArrowLeftIcon,
  PencilIcon,
  ServerIcon,
  CircleStackIcon,
  FolderIcon,
  DocumentTextIcon,
  SignalIcon,
  CloudIcon,
  GlobeAltIcon,
  InboxStackIcon,
  MagnifyingGlassCircleIcon,
  ChartBarIcon,
  CheckIcon,
  XMarkIcon,
  MapIcon,
  PlayIcon,
  CogIcon,
  RocketLaunchIcon,
  ArrowPathIcon,
  ExclamationTriangleIcon,
} from "@heroicons/react/24/outline";
import OriginalSchemaView from "@/components/datasource/OriginalSchemaView";
import ProfileManagementView from "@/components/profile/ProfileManagementView";
// [2026-04-20] 파서 연결 섹션
import DataSourceParserSection from "@/components/datasource/DataSourceParserSection";
import { useQueryClient, useMutation } from "@tanstack/react-query";
import { toast } from "react-hot-toast";
import { useErrorHandling } from "@/hooks/useErrorHandling";
// [2026-04-22] 수집기 초기화 API
import { resetCollection } from "@/app/data-sources/api";
// [2026-04-22] 수집 원본 / 매핑 결과 조회 컴포넌트 — TODO-001/002
import LandingRecordView from "@/components/datasource/LandingRecordView";
import MappedStorageView from "@/components/datasource/MappedStorageView";

// 아이콘 매핑
const ICON_MAP: Record<string, React.ComponentType<{ className?: string }>> = {
  database: CircleStackIcon,
  folder: FolderIcon,
  server: ServerIcon,
  globe: GlobeAltIcon,
  inbox: InboxStackIcon,
  cloud: CloudIcon,
  document: DocumentTextIcon,
  signal: SignalIcon,
  search: MagnifyingGlassCircleIcon,
  chart: ChartBarIcon,
};

export default function DataSourceDetailPage() {
  const params = useParams();
  const router = useRouter();
  const queryClient = useQueryClient();
  const { handleError } = useErrorHandling();
  const dataSourceId = params.id as string;
  const [isEditingInfo, setIsEditingInfo] = useState(false);
  const [editingData, setEditingData] = useState({
    name: "",
    description: "",
  });
  // [2026-04-22] "landing"(수집 원본), "mapped"(매핑 결과) 탭 추가 — TODO-001/002
  const [activeTab, setActiveTab] = useState<"overview" | "config" | "originalSchema" | "profileSchema" | "parsers" | "landing" | "mapped">("overview");
  // [2026-04-22] 수집기 초기화 확인 다이얼로그 상태
  const [showResetConfirm, setShowResetConfirm] = useState(false);

  // 데이터 소스 상세 정보 조회
  const { data: dataSource, isLoading } = useQueryWithErrorHandling({
    queryKey: ["data-sources", dataSourceId],
    queryFn: () => fetchDataSource(dataSourceId),
  });

  // 데이터 소스 타입 메타데이터 조회
  const { data: dataSourceTypes } = useQueryWithErrorHandling({
    queryKey: ["metadata", "data-source-types"],
    queryFn: fetchDataSourceTypes,
  });

  // 프로파일 목록 조회
  const { data: profiles = [] } = useQueryWithErrorHandling({
    queryKey: ["schemaProfiles", dataSourceId],
    queryFn: () => fetchDataProfiles(dataSourceId),
  });

  // 원본 스키마 조회
  const { data: originalSchemas = [] } = useQueryWithErrorHandling({
    queryKey: ["dataSourceOriginalSchemas", dataSourceId],
    queryFn: () => fetchDataSourceOriginalSchemas(dataSourceId),
  });

  // [2026-04-22] 수집기 초기화 mutation
  const resetMutation = useMutation({
    mutationFn: () => resetCollection(dataSourceId),
    onSuccess: (result) => {
      setShowResetConfirm(false);
      if (result.mode === "AGENT" && result.agentConnected === false) {
        toast("⚠️ " + result.message, { duration: 5000 });
      } else {
        toast.success(result.message);
      }
    },
    onError: handleError,
  });

  // 수정 mutation
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: any }) =>
      updateDataSource(id, data),
    onSuccess: () => {
      toast.success("데이터 소스가 수정되었습니다.");
      queryClient.invalidateQueries({ queryKey: ["data-sources", dataSourceId] });
      queryClient.invalidateQueries({ queryKey: ["data-sources"] });
      setIsEditingInfo(false);
    },
    onError: handleError,
  });

  const startEdit = () => {
    if (dataSource) {
      setEditingData({
        name: dataSource.name,
        description: dataSource.description || "",
      });
      setIsEditingInfo(true);
    }
  };

  const cancelEdit = () => {
    setIsEditingInfo(false);
    setEditingData({ name: "", description: "" });
  };

  const handleSave = () => {
    if (!editingData.name.trim()) {
      toast.error("이름은 필수입니다.");
      return;
    }

    updateMutation.mutate({
      id: dataSourceId,
      data: {
        name: editingData.name.trim(),
        description: editingData.description.trim(),
      },
    });
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!dataSource) {
    return (
      <div className="max-w-7xl mx-auto py-8">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-gray-900">
            데이터 소스를 찾을 수 없습니다
          </h2>
          <button
            onClick={() => router.push("/data-sources")}
            className="mt-4 text-blue-600 hover:text-blue-800"
          >
            목록으로 돌아가기
          </button>
        </div>
      </div>
    );
  }

  const typeMetadata = dataSourceTypes?.find(
    (t) => t.value === dataSource.sourceType
  );
  const IconComponent = typeMetadata
    ? ICON_MAP[typeMetadata.iconType] || ServerIcon
    : ServerIcon;

  return (
    <div className="max-w-7xl mx-auto py-8 space-y-8">
      {/* 헤더 */}
      <div className="bg-white rounded-3xl shadow-xl overflow-hidden border border-gray-100">
        <div className="bg-gradient-to-r from-blue-600 to-indigo-600 px-8 py-6">
          <div className="flex justify-between items-center">
            <div className="flex items-center gap-4">
              <button
                onClick={() => router.push("/data-sources")}
                className="p-2 text-white hover:bg-white/20 rounded-lg transition-colors"
              >
                <ArrowLeftIcon className="h-5 w-5" />
              </button>
              <div className="flex items-center gap-3">
                <div className="p-3 bg-white/20 rounded-xl">
                  <IconComponent className="h-8 w-8 text-white" />
                </div>
                <div>
                  <h1 className="text-3xl font-bold text-white">
                    {dataSource.name}
                  </h1>
                  <p className="mt-1 text-blue-100">
                    {typeMetadata?.label || dataSource.sourceType}
                  </p>
                </div>
              </div>
            </div>
            {!isEditingInfo && (
              <LoadingButton
                onClick={startEdit}
                type="button"
                size="small"
                className="inline-flex items-center gap-2"
              >
                <PencilIcon className="h-5 w-5" />
                정보 수정
              </LoadingButton>
            )}
          </div>
        </div>

        {/* 상세 정보 */}
        <div className="px-8 py-6 space-y-6">
          {/* 기본 정보 */}
          <div>
            <div className="flex justify-between items-center mb-4">
              {isEditingInfo && (
                <div className="flex gap-2">
                  <button
                    onClick={handleSave}
                    disabled={updateMutation.isPending}
                    className="inline-flex items-center px-3 py-1.5 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
                  >
                    <CheckIcon className="h-4 w-4 mr-1" />
                    저장
                  </button>
                  <button
                    onClick={cancelEdit}
                    className="inline-flex items-center px-3 py-1.5 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                  >
                    <XMarkIcon className="h-4 w-4 mr-1" />
                    취소
                  </button>
                </div>
              )}
            </div>

            {isEditingInfo ? (
              // 편집 모드
              <div className="space-y-4 bg-blue-50 p-4 rounded-lg">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    이름 <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={editingData.name}
                    onChange={(e) => setEditingData({ ...editingData, name: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    설명
                  </label>
                  <textarea
                    value={editingData.description}
                    onChange={(e) => setEditingData({ ...editingData, description: e.target.value })}
                    rows={3}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">타입</label>
                    <div className="flex items-center gap-2 mt-1">
                      <IconComponent className="h-5 w-5 text-gray-600" />
                      <span className="text-sm text-gray-900">
                        {typeMetadata?.label || dataSource.sourceType}
                      </span>
                      <span className="text-xs text-gray-500">(변경 불가)</span>
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">상태</label>
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${dataSource.isActive
                          ? "bg-green-100 text-green-800"
                          : "bg-gray-100 text-gray-800"
                        }`}
                    >
                      {dataSource.isActive ? "활성" : "비활성"}
                    </span>
                  </div>
                </div>
              </div>
            ) : (
              // 보기 모드
              <>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <dt className="text-sm font-medium text-gray-500">상태</dt>
                    <dd className="mt-1">
                      <span
                        className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${dataSource.isActive
                            ? "bg-green-100 text-green-800"
                            : "bg-gray-100 text-gray-800"
                          }`}
                      >
                        {dataSource.isActive ? "활성" : "비활성"}
                      </span>
                    </dd>
                  </div>
                  <div>
                    <dt className="text-sm font-medium text-gray-500">
                      연결된 룰 수
                    </dt>
                    <dd className="mt-1 text-sm text-gray-900">
                      {dataSource.mappedRuleCount}개
                    </dd>
                  </div>
                </div>
                {dataSource.description && (
                  <div className="mt-4">
                    <dt className="text-sm font-medium text-gray-500">설명</dt>
                    <dd className="mt-1 text-sm text-gray-900">
                      {dataSource.description}
                    </dd>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>

      {/* 탭 네비게이션 */}
      <div className="bg-white rounded-3xl shadow-xl overflow-hidden border border-gray-100">
        <div className="border-b border-gray-200">
          <nav className="-mb-px flex space-x-8 px-8" aria-label="Tabs">
            <button
              className={`py-4 px-1 border-b-2 font-medium text-sm ${activeTab === "overview"
                  ? "border-blue-500 text-blue-600"
                  : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                }`}
              onClick={() => setActiveTab("overview")}
            >
              개요
            </button>
            <button
              className={`py-4 px-1 border-b-2 font-medium text-sm ${activeTab === "config"
                  ? "border-blue-500 text-blue-600"
                  : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                }`}
              onClick={() => setActiveTab("config")}
            >
              연결 설정
            </button>
            <button
              className={`py-4 px-1 border-b-2 font-medium text-sm ${activeTab === "originalSchema"
                  ? "border-blue-500 text-blue-600"
                  : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                }`}
              onClick={() => setActiveTab("originalSchema")}
            >
              원본 필드
            </button>
            <button
              className={`py-4 px-1 border-b-2 font-medium text-sm ${activeTab === "profileSchema"
                  ? "border-blue-500 text-blue-600"
                  : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                }`}
              onClick={() => setActiveTab("profileSchema")}
            >
              프로파일 관리
            </button>
            {/* [2026-04-21] DATABASE 타입은 JDBC 직접 수집이므로 파서 설정 탭 미표시 */}
            {dataSource.sourceType !== "DATABASE" && (
              <button
                className={`py-4 px-1 border-b-2 font-medium text-sm ${activeTab === "parsers"
                    ? "border-blue-500 text-blue-600"
                    : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                  }`}
                onClick={() => setActiveTab("parsers")}
              >
                파서 설정
              </button>
            )}
            {/* [2026-04-22] 수집 원본 탭 — TODO-001 */}
            <button
              className={`py-4 px-1 border-b-2 font-medium text-sm ${activeTab === "landing"
                  ? "border-blue-500 text-blue-600"
                  : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                }`}
              onClick={() => setActiveTab("landing")}
            >
              수집 원본
            </button>
            {/* [2026-04-22] 매핑 결과 탭 — TODO-002 */}
            <button
              className={`py-4 px-1 border-b-2 font-medium text-sm ${activeTab === "mapped"
                  ? "border-blue-500 text-blue-600"
                  : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                }`}
              onClick={() => setActiveTab("mapped")}
            >
              매핑 결과
            </button>
          </nav>
        </div>

        {/* 탭 컨텐츠 */}
        <div className="p-8">
          {activeTab === "overview" && (
            <div className="space-y-8">
              {/* 빠른 시작 섹션 */}
              <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-2xl p-8 border border-blue-100">
                <div className="text-center space-y-4">
                  <div className="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-r from-blue-500 to-indigo-600 rounded-full">
                    <RocketLaunchIcon className="h-8 w-8 text-white" />
                  </div>
                  <div>
                    <h3 className="text-2xl font-bold text-gray-900">빠른 시작</h3>
                    <p className="text-gray-600 mt-2">복잡한 설정 없이 바로 룰 생성하고 데이터를 분석하세요</p>
                  </div>
                  <div className="flex flex-col sm:flex-row gap-4 justify-center">
                    <LoadingButton
                      onClick={() => router.push(`/rules/new?dataSourceId=${dataSourceId}`)}
                      className="inline-flex items-center gap-2 bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700 text-white px-6 py-3 rounded-xl font-semibold shadow-lg hover:shadow-xl transform hover:scale-105 transition-all duration-200"
                    >
                      <PlayIcon className="h-5 w-5" />
                      룰 바로 생성
                    </LoadingButton>
                    <button
                      onClick={() => setActiveTab('originalSchema')}
                      className="inline-flex items-center gap-2 bg-white text-gray-700 px-6 py-3 rounded-xl font-semibold border border-gray-200 hover:bg-gray-50 hover:shadow-md transition-all duration-200"
                    >
                      <CogIcon className="h-5 w-5" />
                      고급 설정
                    </button>
                  </div>
                  <p className="text-sm text-gray-500 max-w-2xl mx-auto">
                    &quot;룰 바로 생성&quot; 옵션은 기본 설정으로 단순하게 시작할 수 있게 도와드립니다.
                    더 많은 제어가 필요하다면 &quot;고급 설정&quot;을 사용하세요.
                  </p>
                </div>
              </div>

              {/* 현재 상태 */}
              <div>
                <h3 className="text-lg font-semibold text-gray-900 mb-4">현재 상태</h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                  <div className="bg-gray-50 rounded-xl p-6">
                    <div className="flex items-center gap-3">
                      <div className="p-3 bg-blue-100 rounded-lg">
                        <CircleStackIcon className="h-6 w-6 text-blue-600" />
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">원본 필드</p>
                        <p className="text-2xl font-bold text-gray-900">{originalSchemas.length}</p>
                      </div>
                    </div>
                  </div>
                  <div className="bg-gray-50 rounded-xl p-6">
                    <div className="flex items-center gap-3">
                      <div className="p-3 bg-green-100 rounded-lg">
                        <MapIcon className="h-6 w-6 text-green-600" />
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">프로파일</p>
                        <p className="text-2xl font-bold text-gray-900">{profiles.length}</p>
                      </div>
                    </div>
                  </div>
                  <div className="bg-gray-50 rounded-xl p-6">
                    <div className="flex items-center gap-3">
                      <div className="p-3 bg-purple-100 rounded-lg">
                        <DocumentTextIcon className="h-6 w-6 text-purple-600" />
                      </div>
                      <div>
                        <p className="text-sm text-gray-600">연결된 룰</p>
                        <p className="text-2xl font-bold text-gray-900">{dataSource.mappedRuleCount}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeTab === "config" && (
            <div className="space-y-6">
              {/* 타입에 따른 연결 설정 폼 */}
              {/* dynamic import 회피를 위해 require 사용 */}
              {(() => {
                // eslint-disable-next-line @typescript-eslint/no-var-requires
                const ConfigForm = require("@/components/datasource/ConfigForm").default;
                return <ConfigForm dataSource={dataSource} />;
              })()}

              {/* [2026-04-22] 수집 초기화 섹션 — DATABASE / FILE_SYSTEM / FILE_SYSTEM_REALTIME만 표시 */}
              {(dataSource.sourceType === "DATABASE" ||
                dataSource.sourceType === "FILE_SYSTEM" ||
                dataSource.sourceType === "FILE_SYSTEM_REALTIME") && (
                <div className="border border-red-200 rounded-xl p-5 bg-red-50">
                  <div className="flex items-start gap-3">
                    <ExclamationTriangleIcon className="h-5 w-5 text-red-500 mt-0.5 flex-shrink-0" />
                    <div className="flex-1">
                      <h4 className="text-sm font-semibold text-red-800">수집 초기화</h4>
                      <p className="mt-1 text-xs text-red-600">
                        마지막 수집 위치(last_position)를 초기화하여 파일 또는 DB를 처음부터 재수집합니다.
                        기존 수집 데이터는 삭제되지 않으며 수집 위치 정보만 초기화됩니다.
                      </p>
                      {!showResetConfirm ? (
                        <button
                          onClick={() => setShowResetConfirm(true)}
                          className="mt-3 inline-flex items-center gap-2 px-3 py-1.5 text-xs font-medium text-red-700 bg-white border border-red-300 rounded-lg hover:bg-red-50 transition-colors"
                        >
                          <ArrowPathIcon className="h-4 w-4" />
                          수집 초기화
                        </button>
                      ) : (
                        <div className="mt-3 p-3 bg-white border border-red-300 rounded-lg space-y-2">
                          <p className="text-xs font-medium text-red-800">
                            정말로 수집 위치를 초기화하시겠습니까?
                          </p>
                          <p className="text-xs text-red-600">
                            초기화 후 다음 수집 주기에 처음부터 재수집됩니다.
                            중복 수집이 발생할 수 있습니다.
                          </p>
                          <div className="flex gap-2">
                            <button
                              onClick={() => resetMutation.mutate()}
                              disabled={resetMutation.isPending}
                              className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 disabled:opacity-50 transition-colors"
                            >
                              {resetMutation.isPending ? (
                                <span className="animate-spin h-3 w-3 border border-white border-t-transparent rounded-full" />
                              ) : (
                                <ArrowPathIcon className="h-3 w-3" />
                              )}
                              초기화 확인
                            </button>
                            <button
                              onClick={() => setShowResetConfirm(false)}
                              disabled={resetMutation.isPending}
                              className="px-3 py-1.5 text-xs font-medium text-gray-600 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 transition-colors"
                            >
                              취소
                            </button>
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {activeTab === "originalSchema" && (
            <OriginalSchemaView
              dataSourceId={dataSourceId}
              dataSourceName={dataSource.name}
            />
          )}

          {activeTab === "profileSchema" && (
            <ProfileManagementView
              dataSourceId={dataSourceId}
              dataSourceName={dataSource.name}
            />
          )}

          {/* [2026-04-21] DATABASE 타입은 파서 설정 탭 미표시 */}
          {activeTab === "parsers" && dataSource.sourceType !== "DATABASE" && (
            <DataSourceParserSection dataSourceId={dataSourceId} />
          )}

          {/* [2026-04-22] 수집 원본 조회 — TODO-001 */}
          {activeTab === "landing" && (
            <LandingRecordView dataSourceId={dataSourceId} />
          )}

          {/* [2026-04-22] 매핑 결과 조회 — TODO-002 */}
          {activeTab === "mapped" && (
            <MappedStorageView dataSourceId={dataSourceId} />
          )}

        </div>
      </div>

    </div>
  );
}

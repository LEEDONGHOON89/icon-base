"use client";

import Alert from "@/components/common/Alert";
import LoadingButton from "@/components/common/LoadingButton";
import { useErrorHandling } from "@/hooks/useErrorHandling";
import { useQueryWithErrorHandling } from "@/hooks/useQueryWithErrorHandling";
import { useRouter } from "next/navigation";
import {
  ChartBarIcon,
  CircleStackIcon,
  CloudIcon,
  DocumentTextIcon,
  FolderIcon,
  FunnelIcon,
  GlobeAltIcon,
  InboxStackIcon,
  MagnifyingGlassCircleIcon,
  MagnifyingGlassIcon,
  PlusIcon,
  PowerIcon,
  ServerIcon,
  SignalIcon,
} from "@heroicons/react/24/outline";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "react-hot-toast";
import { fetchDataSourceTypes } from "../metadata/api";
import Modal from "../users/Modal";
import {
  activateDataSource,
  DataSource,
  deactivateDataSource,
  fetchDataSources,
} from "./api";
import DataSourceForm from "./DataSourceForm";

export default function DataSourcesPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState("");
  const [filterType, setFilterType] = useState<string>("ALL");
  const [filterActive, setFilterActive] = useState<string>("ALL");
  const [showForm, setShowForm] = useState(false);
  const [editingDataSource, setEditingDataSource] = useState<DataSource | null>(
    null
  );
  const { error: formError, handleError, clearError } = useErrorHandling();

  // 데이터 소스 목록 조회
  const {
    data: dataSources = [],
    isLoading,
    errorQuery: queryError,
  } = useQueryWithErrorHandling({
    queryKey: ["dataSources"],
    queryFn: fetchDataSources,
  });

  // 데이터 소스 타입 메타데이터 조회
  const { data: dataSourceTypes } = useQueryWithErrorHandling({
    queryKey: ["metadata", "data-source-types"],
    queryFn: fetchDataSourceTypes,
  });

  // 아이콘 매핑
  const ICON_MAP: Record<
    string,
    React.ComponentType<{ className?: string }>
  > = {
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

  // 삭제 mutation
  // const deleteMutation = useMutation({
  //   mutationFn: deleteDataSource,
  //   onSuccess: () => {
  //     queryClient.invalidateQueries({ queryKey: ["dataSources"] });
  //     toast.success("데이터 소스가 삭제되었습니다.");
  //   },
  //   onError: handleError,
  // });

  // 활성화/비활성화 mutations
  const activateMutation = useMutation({
    mutationFn: activateDataSource,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["dataSources"] });
      toast.success("데이터 소스가 활성화되었습니다.");
    },
    onError: handleError,
  });

  const deactivateMutation = useMutation({
    mutationFn: deactivateDataSource,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["dataSources"] });
      toast.success("데이터 소스가 비활성화되었습니다.");
    },
    onError: handleError,
  });

  // 토글 핸들러
  const handleToggleActive = (dataSource: DataSource) => {
    if (dataSource.isActive) {
      deactivateMutation.mutate(dataSource.dataSourceId);
    } else {
      activateMutation.mutate(dataSource.dataSourceId);
    }
  };

  // 필터링된 데이터
  const filteredDataSources = dataSources.filter((ds) => {
    const matchesSearch =
      !searchTerm ||
      ds.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (ds.description &&
        ds.description.toLowerCase().includes(searchTerm.toLowerCase()));

    const matchesType = filterType === "ALL" || ds.sourceType === filterType;

    const matchesActive =
      filterActive === "ALL" ||
      (filterActive === "ACTIVE" && ds.isActive) ||
      (filterActive === "INACTIVE" && !ds.isActive);

    return matchesSearch && matchesType && matchesActive;
  });

  // const handleDelete = (id: string) => {
  //   if (confirm("정말로 이 데이터 소스를 삭제하시겠습니까?")) {
  //     deleteMutation.mutate(id);
  //   }
  // };

  const handleNew = () => {
    setEditingDataSource(null);
    setShowForm(true);
    clearError();
  };

  const handleEdit = (dataSource: DataSource) => {
    setEditingDataSource(dataSource);
    setShowForm(true);
    clearError();
  };

  const handleFormSuccess = () => {
    queryClient.invalidateQueries({ queryKey: ["dataSources"] });
    setShowForm(false);
    setEditingDataSource(null);
  };

  return (
    <div className="p-6 space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">데이터 소스 관리</h1>
          <p className="text-sm text-gray-500 mt-1">수집할 데이터 소스를 관리합니다.</p>
        </div>
        <LoadingButton
          onClick={handleNew}
          type="button"
          size="small"
          className="inline-flex items-center gap-2"
        >
          <PlusIcon className="h-5 w-5" />새 데이터 소스 생성
        </LoadingButton>
      </div>

      {/* 검색 필터 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
          <div className="flex items-center gap-3">
            {/* Search */}
            <div className="relative flex-1 max-w-md">
              <MagnifyingGlassIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
              <input
                type="text"
                placeholder="이름 또는 설명으로 검색..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            {/* Type Filter */}
            <div className="relative">
              <FunnelIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
              <select
                value={filterType}
                onChange={(e) => setFilterType(e.target.value)}
                className="pl-10 pr-8 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 appearance-none"
              >
                <option value="ALL">모든 타입</option>
                {dataSourceTypes?.map((type) => (
                  <option key={type.value} value={type.value}>
                    {type.label}
                  </option>
                ))}
              </select>
            </div>

            {/* Status Filter */}
            <div className="relative">
              <PowerIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
              <select
                value={filterActive}
                onChange={(e) => setFilterActive(e.target.value)}
                className="pl-10 pr-8 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 appearance-none"
              >
                <option value="ALL">모든 상태</option>
                <option value="ACTIVE">활성</option>
                <option value="INACTIVE">비활성</option>
              </select>
            </div>
          </div>
      </div>

      {/* 에러 표시 */}
      {(queryError || formError) && (
        <Alert message={`${(queryError || formError)?.message}`} />
      )}

      {/* Data Source List */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        {/* Loading State */}
        {isLoading && (
          <div className="flex justify-center items-center py-20">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          </div>
        )}

        {!isLoading && (
          <div>
            {filteredDataSources.length === 0 ? (
              <div className="text-center py-12">
                <CircleStackIcon className="mx-auto h-12 w-12 text-gray-400" />
                <h3 className="mt-2 text-sm font-medium text-gray-900">
                  데이터 소스가 없습니다
                </h3>
                <p className="mt-1 text-sm text-gray-500">
                  새 데이터 소스를 생성하여 시작하세요.
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {filteredDataSources.map((dataSource) => {
                  const typeMetadata = dataSourceTypes?.find(
                    (t) => t.value === dataSource.sourceType
                  );
                  const IconComponent = typeMetadata
                    ? ICON_MAP[typeMetadata.iconType] || ServerIcon
                    : ServerIcon;

                  return (
                    <div
                      key={dataSource.dataSourceId}
                      className="bg-white rounded-xl border border-gray-200 p-6 hover:shadow-lg transition-all duration-200 flex flex-col cursor-pointer"
                      // [2026-04-24] window.open(_blank) → router.push 로 변경 — 새 창 대신 현재 탭 내 페이지 전환
                      onClick={() => router.push(`/data-sources/${dataSource.dataSourceId}`)}
                    >
                      {/* 헤더 영역 */}
                      <div className="flex items-start justify-between mb-4">
                        <div className="flex-1">
                          <div className="flex items-center gap-3 mb-2">
                            <div className="p-2 bg-blue-100 rounded-lg">
                              <IconComponent className="h-6 w-6 text-blue-600" />
                            </div>
                            <h3 className="text-lg font-semibold text-gray-900 truncate">
                              {dataSource.name}
                            </h3>
                          </div>
                          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full bg-blue-100 text-blue-800 text-xs">
                            {typeMetadata?.label || dataSource.sourceType}
                          </span>
                        </div>
                      </div>

                      {/* 설명 영역 */}
                      {dataSource.description && (
                        <p className="text-sm text-gray-600 mb-3 line-clamp-2">
                          {dataSource.description}
                        </p>
                      )}

                      {/* 하단 정보 영역 */}
                      <div className="mt-auto pt-4 border-t border-gray-100">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-3 text-sm">
                            {dataSource.mappedRuleCount > 0 && (
                              <span className="text-gray-600">
                                룰 {dataSource.mappedRuleCount}개
                              </span>
                            )}
                          </div>
                          
                          {/* 활성화 토글 */}
                          <div className="flex items-center gap-2">
                            <span
                              className={`text-xs font-medium ${
                                dataSource.isActive
                                  ? "text-green-600"
                                  : "text-gray-500"
                              }`}
                            >
                              {dataSource.isActive ? "활성" : "비활성"}
                            </span>
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                handleToggleActive(dataSource);
                              }}
                              disabled={
                                activateMutation.isPending ||
                                deactivateMutation.isPending
                              }
                              className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1 ${
                                dataSource.isActive
                                  ? "bg-green-500"
                                  : "bg-gray-300"
                              } ${
                                activateMutation.isPending ||
                                deactivateMutation.isPending
                                  ? "opacity-50 cursor-not-allowed"
                                  : "cursor-pointer"
                              }`}
                              title={
                                dataSource.isActive ? "비활성화" : "활성화"
                              }
                            >
                              <span
                                className={`inline-block h-3.5 w-3.5 transform rounded-full bg-white transition-transform ${
                                  dataSource.isActive
                                    ? "translate-x-5"
                                    : "translate-x-1"
                                }`}
                              />
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </div>

      {/* 생성/수정 Modal */}
      <Modal
        open={showForm}
        onClose={() => {
          setShowForm(false);
          setEditingDataSource(null);
          clearError();
        }}
        title={editingDataSource ? "데이터 소스 정보" : "새 데이터 소스 생성"}
        size="lg"
      >
        <DataSourceForm
          dataSource={editingDataSource}
          onSuccess={handleFormSuccess}
          onCancel={() => {
            setShowForm(false);
            setEditingDataSource(null);
          }}
        />
      </Modal>
    </div>
  );
}

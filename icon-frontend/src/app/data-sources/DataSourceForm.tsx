import LoadingButton from "@/components/common/LoadingButton";
import { useErrorHandling } from "@/hooks/useErrorHandling";
import { useQueryWithErrorHandling } from "@/hooks/useQueryWithErrorHandling";
import {
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
} from "@heroicons/react/24/outline";
import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "react-hot-toast";
import {
  createDataSource,
  DataSource,
  DataSourceType,
  updateDataSource,
} from "./api";
import { fetchDataSourceTypes } from "../metadata/api";

interface DataSourceFormProps {
  dataSource?: DataSource | null;
  onSuccess?: () => void;
  onCancel?: () => void;
}

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

export default function DataSourceForm({
  dataSource,
  onSuccess,
  onCancel,
}: DataSourceFormProps) {
  const { handleError } = useErrorHandling();
  const [name, setName] = useState(dataSource?.name || "");
  const [description, setDescription] = useState(dataSource?.description || "");
  const [sourceType, setSourceType] = useState<DataSourceType>(
    dataSource?.sourceType || "DATABASE"
  );
  const [errors, setErrors] = useState<{ [key: string]: string }>({});

  const isEditMode = !!dataSource;

  // 데이터 소스 타입 메타데이터 조회
  const { data: dataSourceTypes, isLoading: isLoadingTypes } =
    useQueryWithErrorHandling({
      queryKey: ["metadata", "data-source-types"],
      queryFn: fetchDataSourceTypes,
    });

  // 생성 mutation
  const createMutation = useMutation({
    mutationFn: createDataSource,
    onSuccess: () => {
      toast.success("데이터 소스가 생성되었습니다.");
      onSuccess?.();
    },
    onError: handleError,
  });

  // 수정 mutation
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: any }) =>
      updateDataSource(id, data),
    onSuccess: () => {
      toast.success("데이터 소스가 수정되었습니다.");
      onSuccess?.();
    },
    onError: handleError,
  });

  const validate = () => {
    const newErrors: { [key: string]: string } = {};

    if (!name.trim()) {
      newErrors.name = "이름은 필수입니다.";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate()) {
      return;
    }

    if (isEditMode && dataSource) {
      updateMutation.mutate({
        id: dataSource.dataSourceId,
        data: {
          name: name.trim(),
          description: description.trim(),
        },
      });
    } else {
      createMutation.mutate({
        name: name.trim(),
        description: description.trim(),
        sourceType,
      });
    }
  };

  const isPending =
    createMutation.isPending ||
    updateMutation.isPending;

  // 현재 선택된 타입의 메타데이터 찾기
  const selectedTypeMetadata = dataSourceTypes?.find(
    (type) => type.value === sourceType
  );

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* 이름 입력 */}
      <div>
        <label
          htmlFor="name"
          className="block text-sm font-medium text-gray-700 mb-2"
        >
          데이터 소스 이름 <span className="text-red-500">*</span>
        </label>
        <input
          type="text"
          id="name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className={`w-full px-4 py-2 border ${
            errors.name ? "border-red-300" : "border-gray-300"
          } rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500`}
          placeholder="예: 메인 데이터베이스"
          disabled={isPending}
        />
        {errors.name && (
          <p className="mt-1 text-sm text-red-600">{errors.name}</p>
        )}
      </div>

      {/* 설명 입력 */}
      <div>
        <label
          htmlFor="description"
          className="block text-sm font-medium text-gray-700 mb-2"
        >
          설명
        </label>
        <textarea
          id="description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={3}
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          placeholder="데이터 소스에 대한 설명을 입력하세요."
          disabled={isPending}
        />
      </div>

      {/* 타입 선택 - 수정 모드에서는 비활성화 */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          데이터 소스 타입{" "}
          {!isEditMode && <span className="text-red-500">*</span>}
        </label>
        {isEditMode && selectedTypeMetadata ? (
          <div className="p-4 bg-gray-50 rounded-lg">
            <div className="flex items-center gap-3">
              {(() => {
                const IconComponent =
                  ICON_MAP[selectedTypeMetadata.iconType] || ServerIcon;
                return <IconComponent className="h-6 w-6 text-gray-600" />;
              })()}
              <div>
                <p className="font-medium text-gray-900">
                  {selectedTypeMetadata.label}
                </p>
                <p className="text-sm text-gray-500">
                  {selectedTypeMetadata.description}
                </p>
              </div>
            </div>
            <p className="mt-2 text-sm text-gray-500">
              * 타입은 생성 후 변경할 수 없습니다.
            </p>
          </div>
        ) : isLoadingTypes ? (
          <div className="text-center py-4 text-gray-500">
            타입 정보를 불러오는 중...
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-3">
            {dataSourceTypes?.map((type) => {
              const IconComponent = ICON_MAP[type.iconType] || ServerIcon;
              return (
                <label
                  key={type.value}
                  className={`relative flex items-center p-4 border rounded-lg cursor-pointer transition-all ${
                    sourceType === type.value
                      ? "border-blue-500 bg-blue-50"
                      : "border-gray-300 hover:border-gray-400"
                  }`}
                >
                  <input
                    type="radio"
                    name="sourceType"
                    value={type.value}
                    checked={sourceType === type.value}
                    onChange={(e) =>
                      setSourceType(e.target.value as DataSourceType)
                    }
                    className="sr-only"
                  />
                  <IconComponent className="h-6 w-6 text-gray-600 flex-shrink-0" />
                  <div className="ml-3">
                    <span className="block text-sm font-medium text-gray-900">
                      {type.label}
                    </span>
                    <span className="block text-sm text-gray-500">
                      {type.description}
                    </span>
                  </div>
                  {sourceType === type.value && (
                    <div className="absolute right-4 text-blue-500">
                      <svg
                        className="h-5 w-5"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                          clipRule="evenodd"
                        />
                      </svg>
                    </div>
                  )}
                </label>
              );
            })}
          </div>
        )}
      </div>


      {/* 액션 버튼 */}
      <div className="flex gap-3 pt-4">
        <LoadingButton
          type="submit"
          loading={createMutation.isPending || updateMutation.isPending}
          disabled={isPending || isLoadingTypes}
          className="flex-1"
        >
          {isEditMode ? "정보 수정" : "생성"}
        </LoadingButton>
        <LoadingButton
          type="button"
          variant="secondary"
          onClick={onCancel}
          disabled={isPending}
          className="flex-1"
        >
          취소
        </LoadingButton>
      </div>
    </form>
  );
}

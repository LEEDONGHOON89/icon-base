"use client";

import { useForm, Controller } from "react-hook-form";
import { useState } from "react";
import LoadingButton from "@/components/common/LoadingButton";
import type {
  DetectionArea,
  CreateDetectionAreaRequest,
  UpdateDetectionAreaRequest,
} from "./api";

interface DomainFormProps {
  initialData?: DetectionArea;
  onSubmit: (
    data: CreateDetectionAreaRequest | UpdateDetectionAreaRequest
  ) => Promise<void>;
  onCancel: () => void;
  isEdit?: boolean;
}

// 아이콘 옵션 (추후 확장 가능)
const ICON_OPTIONS = [
  { value: "UserIcon", label: "UserIcon (사용자)" },
  { value: "CreditCardIcon", label: "CreditCardIcon (카드)" },
  { value: "DevicePhoneMobileIcon", label: "DevicePhoneMobileIcon (모바일)" },
  { value: "KeyIcon", label: "KeyIcon (키)" },
  { value: "ShieldCheckIcon", label: "ShieldCheckIcon (보안)" },
  { value: "DocumentIcon", label: "DocumentIcon (문서)" },
];

// 프리셋 색상 옵션 (빠른 선택용)
const PRESET_COLORS = [
  "#3B82F6", // blue
  "#10B981", // green
  "#8B5CF6", // purple
  "#F97316", // orange
  "#EF4444", // red
  "#F59E0B", // yellow
  "#6B7280", // gray
  "#EC4899", // pink
  "#14B8A6", // teal
];

export function DomainForm({
  initialData,
  onSubmit,
  onCancel,
  isEdit = false,
}: DomainFormProps) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    formState: { errors },
  } = useForm<CreateDetectionAreaRequest | UpdateDetectionAreaRequest>({
    defaultValues: initialData
      ? {
          areaName: initialData.areaName,
          description: initialData.description || "",
          icon: initialData.icon || "",
          color: initialData.color || "",
          displayOrder: initialData.displayOrder || 0,
        }
      : {
          detectionAreaId: "",
          areaName: "",
          description: "",
          icon: "",
          color: "",
          displayOrder: 0,
        },
  });

  const handleFormSubmit = async (
    data: CreateDetectionAreaRequest | UpdateDetectionAreaRequest
  ) => {
    try {
      setIsSubmitting(true);
      setError(null);

      // 생성 시 detectionAreaId 형식 검증
      if (!isEdit && "detectionAreaId" in data) {
        const detectionAreaIdPattern = /^[A-Z0-9_]+$/;
        if (!detectionAreaIdPattern.test(data.detectionAreaId)) {
          setError(
            "도메인 ID는 영문 대문자, 숫자, 언더스코어만 사용할 수 있습니다"
          );
          return;
        }
      }

      await onSubmit(data);
    } catch (err: any) {
      setError(err.response?.data?.message || "저장 중 오류가 발생했습니다");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6">
      {error && (
        <div className="rounded-lg bg-red-50 p-4 text-sm text-red-600">
          {error}
        </div>
      )}

      {/* 도메인 ID (생성 시에만 입력 가능) */}
      {!isEdit && (
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            도메인 ID *
          </label>
          <input
            {...register("detectionAreaId", {
              required: "도메인 ID는 필수입니다",
            })}
            type="text"
            placeholder="예: CUSTOMER, ACCOUNT (영문 대문자, 숫자, 언더스코어만)"
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            disabled={isEdit}
          />
          {"detectionAreaId" in errors && errors.detectionAreaId && (
            <p className="mt-1 text-sm text-red-600">
              {(errors as any).detectionAreaId.message}
            </p>
          )}
          <p className="mt-1 text-xs text-gray-500">
            한번 생성하면 변경할 수 없습니다
          </p>
        </div>
      )}

      {/* 도메인 이름 */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          도메인 이름 *
        </label>
        <input
          {...register("areaName", {
            required: "도메인 이름은 필수입니다",
          })}
          type="text"
          placeholder="예: 고객, 계좌"
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        />
        {errors.areaName && (
          <p className="mt-1 text-sm text-red-600">
            {errors.areaName.message}
          </p>
        )}
      </div>

      {/* 설명 */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          설명
        </label>
        <textarea
          {...register("description")}
          placeholder="도메인에 대한 설명을 입력하세요"
          rows={3}
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        {/* 아이콘 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            아이콘
          </label>
          <select
            {...register("icon")}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="">선택 안함</option>
            {ICON_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        {/* 색상 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            색상 (HEX)
          </label>
          <Controller
            name="color"
            control={control}
            rules={{
              pattern: {
                value: /^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/,
                message: "올바른 HEX 색상 형식이 아닙니다 (예: #FF5733)",
              },
            }}
            render={({ field }) => (
              <div className="space-y-2">
                {/* 색상 입력 + 피커 */}
                <div className="flex gap-2">
                  <div className="relative flex-1">
                    <input
                      {...field}
                      type="text"
                      placeholder="#3B82F6"
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 pr-12"
                    />
                    {/* 색상 미리보기 */}
                    {field.value && /^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/.test(field.value) && (
                      <div
                        className="absolute right-3 top-1/2 -translate-y-1/2 w-6 h-6 rounded border border-gray-300"
                        style={{ backgroundColor: field.value }}
                      />
                    )}
                  </div>
                  {/* 네이티브 색상 피커 */}
                  <input
                    type="color"
                    value={field.value || "#3B82F6"}
                    onChange={(e) => field.onChange(e.target.value.toUpperCase())}
                    className="w-10 h-10 p-1 border border-gray-300 rounded-lg cursor-pointer"
                  />
                </div>
                {/* 프리셋 색상 */}
                <div className="flex gap-1.5 flex-wrap">
                  {PRESET_COLORS.map((color) => (
                    <button
                      key={color}
                      type="button"
                      onClick={() => field.onChange(color)}
                      className={`w-6 h-6 rounded border-2 transition-all ${
                        field.value === color
                          ? "border-blue-500 scale-110"
                          : "border-gray-200 hover:border-gray-400"
                      }`}
                      style={{ backgroundColor: color }}
                      title={color}
                    />
                  ))}
                </div>
              </div>
            )}
          />
          {errors.color && (
            <p className="mt-1 text-sm text-red-600">
              {errors.color.message}
            </p>
          )}
        </div>
      </div>

      {/* 표시 순서 */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          표시 순서
        </label>
        <input
          {...register("displayOrder", {
            valueAsNumber: true,
          })}
          type="number"
          min="0"
          placeholder="0"
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        />
        <p className="mt-1 text-xs text-gray-500">
          숫자가 작을수록 먼저 표시됩니다
        </p>
      </div>

      {/* 버튼 */}
      <div className="flex justify-end gap-3 pt-4">
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-500"
        >
          취소
        </button>
        <LoadingButton
          type="submit"
          loading={isSubmitting}
          className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          {isEdit ? "수정" : "생성"}
        </LoadingButton>
      </div>
    </form>
  );
}

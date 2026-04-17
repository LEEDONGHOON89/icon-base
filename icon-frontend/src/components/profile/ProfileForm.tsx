"use client";

import { useState, useEffect } from "react";
import { XMarkIcon } from "@heroicons/react/24/outline";
import { 
  type DataProfile, 
  type ProfilePurpose, 
  type DataProfileCreateRequest, 
  type DataProfileUpdateRequest 
} from "@/app/data-sources/api";
import LoadingButton from "@/components/common/LoadingButton";
import Input from "@/components/common/Input";

interface ProfileFormProps {
  dataSourceId: string;
  profile?: DataProfile;
  onSubmit: (data: DataProfileCreateRequest | DataProfileUpdateRequest) => void;
  onCancel: () => void;
  isLoading: boolean;
  title: string;
}

const PURPOSE_OPTIONS: { value: ProfilePurpose; label: string; description: string }[] = [
  { value: "DEFAULT", label: "기본", description: "일반적인 데이터 분석 용도" },
  { value: "SECURITY", label: "보안 분석", description: "보안 이벤트 및 위협 탐지" },
  { value: "PERFORMANCE", label: "성능 분석", description: "시스템 성능 모니터링" },
  { value: "BUSINESS", label: "비즈니스 분석", description: "비즈니스 인텔리전스 및 분석" },
  { value: "COMPLIANCE", label: "컴플라이언스", description: "규정 준수 및 감사" },
];

export default function ProfileForm({
  dataSourceId,
  profile,
  onSubmit,
  onCancel,
  isLoading,
  title,
}: ProfileFormProps) {
  const [formData, setFormData] = useState({
    profileName: profile?.profileName || "",
    profilePurpose: profile?.profilePurpose || "DEFAULT" as ProfilePurpose,
    description: profile?.description || "",
    displayOrder: profile?.displayOrder || 0,
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  const validateForm = () => {
    const newErrors: Record<string, string> = {};

    if (!formData.profileName.trim()) {
      newErrors.profileName = "프로파일명은 필수입니다.";
    } else if (formData.profileName.length > 100) {
      newErrors.profileName = "프로파일명은 100자를 초과할 수 없습니다.";
    }

    if (formData.description && formData.description.length > 500) {
      newErrors.description = "설명은 500자를 초과할 수 없습니다.";
    }

    if (formData.displayOrder < 0) {
      newErrors.displayOrder = "표시 순서는 0 이상이어야 합니다.";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    const submitData = profile 
      ? {
          profileName: formData.profileName.trim(),
          profilePurpose: formData.profilePurpose,
          description: formData.description.trim() || undefined,
          displayOrder: formData.displayOrder,
        }
      : {
          dataSourceId,
          profileName: formData.profileName.trim(),
          profilePurpose: formData.profilePurpose,
          description: formData.description.trim() || undefined,
          displayOrder: formData.displayOrder,
        };

    onSubmit(submitData);
  };

  const selectedPurpose = PURPOSE_OPTIONS.find(p => p.value === formData.profilePurpose);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-xl max-w-md w-full max-h-[90vh] overflow-y-auto">
        <div className="p-6">
          {/* 헤더 */}
          <div className="flex justify-between items-center mb-6">
            <h3 className="text-lg font-semibold text-gray-900">{title}</h3>
            <button
              onClick={onCancel}
              className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
            >
              <XMarkIcon className="h-5 w-5 text-gray-400" />
            </button>
          </div>

          {/* 폼 */}
          <form onSubmit={handleSubmit} className="space-y-4">
            {/* 프로파일명 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                프로파일명 <span className="text-red-500">*</span>
              </label>
              <Input
                type="text"
                value={formData.profileName}
                onChange={(e) =>
                  setFormData({ ...formData, profileName: e.target.value })
                }
                placeholder="프로파일명을 입력하세요"
                error={errors.profileName}
                maxLength={100}
                required
              />
            </div>

            {/* 프로파일 용도 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                프로파일 용도 <span className="text-red-500">*</span>
              </label>
              <select
                value={formData.profilePurpose}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    profilePurpose: e.target.value as ProfilePurpose,
                  })
                }
                className="w-full px-3 py-2 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                required
              >
                {PURPOSE_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              {selectedPurpose && (
                <p className="mt-1 text-xs text-gray-500">
                  {selectedPurpose.description}
                </p>
              )}
            </div>

            {/* 설명 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                설명
              </label>
              <textarea
                value={formData.description}
                onChange={(e) =>
                  setFormData({ ...formData, description: e.target.value })
                }
                placeholder="프로파일에 대한 설명을 입력하세요 (선택사항)"
                rows={3}
                maxLength={500}
                className={`w-full px-3 py-2 border rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                  errors.description
                    ? "border-red-300 focus:ring-red-500 focus:border-red-500"
                    : "border-gray-300"
                }`}
              />
              {errors.description && (
                <p className="mt-1 text-sm text-red-600">{errors.description}</p>
              )}
              <div className="mt-1 text-xs text-gray-500 text-right">
                {formData.description.length}/500
              </div>
            </div>

            {/* 표시 순서 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                표시 순서
              </label>
              <Input
                type="number"
                value={formData.displayOrder.toString()}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    displayOrder: parseInt(e.target.value) || 0,
                  })
                }
                placeholder="0"
                error={errors.displayOrder}
                min={0}
              />
              <p className="mt-1 text-xs text-gray-500">
                숫자가 작을수록 먼저 표시됩니다.
              </p>
            </div>

            {/* 버튼 */}
            <div className="flex gap-3 pt-4">
              <LoadingButton
                type="submit"
                loading={isLoading}
                className="flex-1"
              >
                {profile ? "수정" : "생성"}
              </LoadingButton>
              <button
                type="button"
                onClick={onCancel}
                className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-xl hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
              >
                취소
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
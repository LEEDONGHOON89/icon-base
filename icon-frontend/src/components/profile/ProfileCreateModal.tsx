"use client";

import { useState } from "react";
import { XMarkIcon } from "@heroicons/react/24/outline";
import LoadingButton from "@/components/common/LoadingButton";
import { ProfilePurpose } from "@/app/data-sources/api";

interface ProfileCreateModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: ProfileCreateData) => void;
  isLoading?: boolean;
}

export interface ProfileCreateData {
  profileName: string;
  profilePurpose: ProfilePurpose;
  description?: string;
  displayOrder?: number;
}

const PURPOSE_OPTIONS: { value: ProfilePurpose; label: string; description: string }[] = [
  { value: "DEFAULT", label: "기본", description: "일반적인 데이터 분석 및 모니터링" },
  { value: "SECURITY", label: "보안 분석", description: "보안 위협 탐지 및 감시" },
  { value: "PERFORMANCE", label: "성능 분석", description: "시스템 성능 모니터링 및 최적화" },
  { value: "BUSINESS", label: "비즈니스 분석", description: "비즈니스 메트릭 및 인사이트" },
  { value: "COMPLIANCE", label: "컴플라이언스", description: "규정 준수 및 감사" },
];

export default function ProfileCreateModal({
  isOpen,
  onClose,
  onSubmit,
  isLoading = false,
}: ProfileCreateModalProps) {
  const [formData, setFormData] = useState<ProfileCreateData>({
    profileName: "",
    profilePurpose: "DEFAULT",
    description: "",
    displayOrder: 1,
  });

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.profileName.trim()) {
      return;
    }

    onSubmit({
      ...formData,
      displayOrder: formData.displayOrder || 1,
    });
  };

  const handleInputChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: name === "displayOrder" ? parseInt(value) || 1 : value,
    }));
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="bg-white rounded-lg w-full max-w-md p-6">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-xl font-semibold text-gray-900">새 프로파일 생성</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-500 focus:outline-none"
          >
            <XMarkIcon className="h-6 w-6" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label
              htmlFor="profileName"
              className="block text-sm font-medium text-gray-700 mb-2"
            >
              프로파일명 <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              id="profileName"
              name="profileName"
              value={formData.profileName}
              onChange={handleInputChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="예: 보안 모니터링 프로파일"
              required
            />
          </div>

          {/* 용도 필드 주석 처리 - 사용하지 않음 */}
          {/* <div>
            <label
              htmlFor="profilePurpose"
              className="block text-sm font-medium text-gray-700 mb-2"
            >
              용도 <span className="text-red-500">*</span>
            </label>
            <select
              id="profilePurpose"
              name="profilePurpose"
              value={formData.profilePurpose}
              onChange={handleInputChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              required
            >
              {PURPOSE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <p className="mt-1 text-sm text-gray-500">
              {PURPOSE_OPTIONS.find((opt) => opt.value === formData.profilePurpose)?.description}
            </p>
          </div> */}

          <div>
            <label
              htmlFor="description"
              className="block text-sm font-medium text-gray-700 mb-2"
            >
              설명
            </label>
            <textarea
              id="description"
              name="description"
              value={formData.description}
              onChange={handleInputChange}
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="프로파일에 대한 상세 설명을 입력하세요"
            />
          </div>

          <div>
            <label
              htmlFor="displayOrder"
              className="block text-sm font-medium text-gray-700 mb-2"
            >
              표시 순서
            </label>
            <input
              type="number"
              id="displayOrder"
              name="displayOrder"
              value={formData.displayOrder}
              onChange={handleInputChange}
              min="1"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
            <p className="mt-1 text-sm text-gray-500">
              낮은 숫자가 먼저 표시됩니다
            </p>
          </div>

          <div className="flex gap-3 pt-4">
            <LoadingButton
              type="submit"
              loading={isLoading}
              className="flex-1 bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            >
              생성
            </LoadingButton>
            <button
              type="button"
              onClick={onClose}
              disabled={isLoading}
              className="flex-1 bg-gray-200 text-gray-700 py-2 px-4 rounded-md hover:bg-gray-300 focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2 disabled:opacity-50"
            >
              취소
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
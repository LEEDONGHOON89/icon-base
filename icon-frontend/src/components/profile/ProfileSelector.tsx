"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ChevronDownIcon, CheckIcon } from "@heroicons/react/24/outline";
import { 
  fetchDataProfiles, 
  type DataProfile, 
  type ProfilePurpose 
} from "@/app/data-sources/api";

interface ProfileSelectorProps {
  dataSourceId: string;
  selectedProfileId?: string;
  onSelect: (profile: DataProfile) => void;
  placeholder?: string;
  className?: string;
}

const PURPOSE_LABELS: Record<ProfilePurpose, string> = {
  DEFAULT: "기본",
  SECURITY: "보안 분석",
  PERFORMANCE: "성능 분석",
  BUSINESS: "비즈니스 분석",
  COMPLIANCE: "컴플라이언스"
};

const PURPOSE_COLORS: Record<ProfilePurpose, string> = {
  DEFAULT: "bg-gray-100 text-gray-800",
  SECURITY: "bg-red-100 text-red-800",
  PERFORMANCE: "bg-blue-100 text-blue-800",
  BUSINESS: "bg-green-100 text-green-800",
  COMPLIANCE: "bg-purple-100 text-purple-800"
};

export default function ProfileSelector({
  dataSourceId,
  selectedProfileId,
  onSelect,
  placeholder = "프로파일을 선택하세요",
  className = "",
}: ProfileSelectorProps) {
  const [isOpen, setIsOpen] = useState(false);

  // 프로파일 목록 조회
  const { data: profiles = [], isLoading } = useQuery({
    queryKey: ["data-profiles", dataSourceId],
    queryFn: () => fetchDataProfiles(dataSourceId),
  });

  const selectedProfile = profiles.find(p => p.profileId === selectedProfileId);
  const activeProfiles = profiles.filter(p => p.isActive).sort((a, b) => a.displayOrder - b.displayOrder);

  const handleSelect = (profile: DataProfile) => {
    onSelect(profile);
    setIsOpen(false);
  };

  return (
    <div className={`relative ${className}`}>
      {/* 선택 버튼 */}
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        disabled={isLoading || activeProfiles.length === 0}
        className="relative w-full bg-white border border-gray-300 rounded-xl pl-3 pr-10 py-2 text-left cursor-default focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-50 disabled:cursor-not-allowed"
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            {selectedProfile ? (
              <>
                <span className="block truncate text-gray-900">
                  {selectedProfile.profileName}
                </span>
                <span
                  className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                    PURPOSE_COLORS[selectedProfile.profilePurpose]
                  }`}
                >
                  {PURPOSE_LABELS[selectedProfile.profilePurpose]}
                </span>
              </>
            ) : (
              <span className="block truncate text-gray-400">
                {isLoading ? "로딩 중..." : 
                 activeProfiles.length === 0 ? "활성화된 프로파일이 없습니다" : 
                 placeholder}
              </span>
            )}
          </div>
          <ChevronDownIcon
            className={`h-5 w-5 text-gray-400 transition-transform ${
              isOpen ? "rotate-180" : ""
            }`}
          />
        </div>
      </button>

      {/* 드롭다운 목록 */}
      {isOpen && activeProfiles.length > 0 && (
        <div className="absolute z-10 mt-1 w-full bg-white shadow-lg rounded-xl border border-gray-200 max-h-60 overflow-auto">
          <div className="py-1">
            {activeProfiles.map((profile) => (
              <button
                key={profile.profileId}
                type="button"
                onClick={() => handleSelect(profile)}
                className="w-full px-3 py-2 text-left hover:bg-gray-50 focus:outline-none focus:bg-gray-50 transition-colors"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="text-gray-900">
                      {profile.profileName}
                    </span>
                    <span
                      className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                        PURPOSE_COLORS[profile.profilePurpose]
                      }`}
                    >
                      {PURPOSE_LABELS[profile.profilePurpose]}
                    </span>
                  </div>
                  {selectedProfileId === profile.profileId && (
                    <CheckIcon className="h-4 w-4 text-blue-600" />
                  )}
                </div>
                {profile.description && (
                  <p className="mt-1 text-xs text-gray-500 truncate">
                    {profile.description}
                  </p>
                )}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* 백드롭 */}
      {isOpen && (
        <div
          className="fixed inset-0 z-0"
          onClick={() => setIsOpen(false)}
        />
      )}
    </div>
  );
}
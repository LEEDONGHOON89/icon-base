"use client";

import { DataProfile } from "@/app/data-sources/api";
import { PlusIcon, UserGroupIcon } from "@heroicons/react/24/outline";

interface ProfileListPanelProps {
  profiles: DataProfile[];
  selectedProfileId: string | null;
  onSelectProfile: (profile: DataProfile) => void;
  onAddProfile?: () => void;
  totalFieldCount?: number;
  showFieldCount?: boolean;
  title?: string;
  showHeader?: boolean;
}

export default function ProfileListPanel({
  profiles,
  selectedProfileId,
  onSelectProfile,
  onAddProfile,
  totalFieldCount,
  showFieldCount = true,
  title = "프로파일 목록",
}: ProfileListPanelProps) {
  return (
    <div className="w-64 bg-gray-50 border-r border-gray-200">
      <div className="p-4 border-b border-gray-200">
        <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
          <UserGroupIcon className="h-5 w-5" />
          {title}
        </h3>
      </div>

      <div className="p-4">
        <div className="space-y-2">
          {profiles.map((profile) => {
            const isSelected = selectedProfileId === profile.profileId;
            return (
              <button
                key={profile.profileId}
                onClick={() => onSelectProfile(profile)}
                className={`w-full text-left px-3 py-2 rounded-lg transition-all ${
                  isSelected
                    ? "bg-blue-100 text-blue-700 font-medium"
                    : "hover:bg-gray-100 text-gray-700"
                }`}
              >
                <div className="flex justify-between items-center">
                  <div>
                    <div className="font-medium">{profile.profileName}</div>
                    <div className="text-xs text-gray-500 font-mono">{profile.profileId}</div>
                    {showFieldCount && totalFieldCount !== undefined && (
                      <div className="text-sm text-gray-500">
                        {profile.schemaCount}/{totalFieldCount} 필드
                      </div>
                    )}
                    {/* 용도 표시 주석 처리 - 사용하지 않음 */}
                    {/* {profile.profilePurpose && (
                      <div className="text-xs text-gray-400">
                        {profile.profilePurpose}
                      </div>
                    )} */}
                  </div>
                  {isSelected && (
                    <div className="text-xs bg-blue-600 text-white px-2 py-1 rounded">
                      선택됨
                    </div>
                  )}
                </div>
              </button>
            );
          })}
        </div>

        {onAddProfile && (
          <button
            onClick={onAddProfile}
            className="mt-4 w-full flex items-center justify-center gap-2 px-3 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 transition-colors"
          >
            <PlusIcon className="h-4 w-4" />
            프로파일 추가
          </button>
        )}
      </div>
    </div>
  );
}

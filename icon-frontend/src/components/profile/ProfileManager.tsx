"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { PlusIcon, PencilIcon, TrashIcon, EyeIcon, EyeSlashIcon } from "@heroicons/react/24/outline";
import { toast } from "react-hot-toast";
import { 
  fetchDataProfiles, 
  createDataProfile, 
  updateDataProfile, 
  deleteDataProfile, 
  toggleDataProfile,
  type DataProfile,
  type ProfilePurpose,
  type DataProfileCreateRequest 
} from "@/app/data-sources/api";
import { useErrorHandling } from "@/hooks/useErrorHandling";
import LoadingButton from "@/components/common/LoadingButton";
import ProfileForm from "./ProfileForm";

interface ProfileManagerProps {
  dataSourceId: string;
  dataSourceName: string;
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

export default function ProfileManager({ dataSourceId, dataSourceName }: ProfileManagerProps) {
  const [isCreating, setIsCreating] = useState(false);
  const [editingProfile, setEditingProfile] = useState<DataProfile | null>(null);
  const queryClient = useQueryClient();
  const { handleError } = useErrorHandling();

  // 프로파일 목록 조회
  const { data: profiles = [], isLoading } = useQuery({
    queryKey: ["data-profiles", dataSourceId],
    queryFn: () => fetchDataProfiles(dataSourceId),
  });

  // 프로파일 생성 mutation
  const createMutation = useMutation({
    mutationFn: createDataProfile,
    onSuccess: () => {
      toast.success("프로파일이 생성되었습니다.");
      queryClient.invalidateQueries({ queryKey: ["data-profiles", dataSourceId] });
      setIsCreating(false);
    },
    onError: handleError,
  });

  // 프로파일 수정 mutation
  const updateMutation = useMutation({
    mutationFn: ({ profileId, data }: { profileId: string; data: any }) =>
      updateDataProfile(profileId, data),
    onSuccess: () => {
      toast.success("프로파일이 수정되었습니다.");
      queryClient.invalidateQueries({ queryKey: ["data-profiles", dataSourceId] });
      setEditingProfile(null);
    },
    onError: handleError,
  });

  // 프로파일 삭제 mutation
  const deleteMutation = useMutation({
    mutationFn: deleteDataProfile,
    onSuccess: () => {
      toast.success("프로파일이 삭제되었습니다.");
      queryClient.invalidateQueries({ queryKey: ["data-profiles", dataSourceId] });
    },
    onError: handleError,
  });

  // 프로파일 활성화/비활성화 mutation
  const toggleMutation = useMutation({
    mutationFn: toggleDataProfile,
    onSuccess: (updatedProfile) => {
      toast.success(
        `프로파일이 ${updatedProfile.isActive ? "활성화" : "비활성화"}되었습니다.`
      );
      queryClient.invalidateQueries({ queryKey: ["data-profiles", dataSourceId] });
    },
    onError: handleError,
  });

  const handleDelete = (profile: DataProfile) => {
    if (confirm(`"${profile.profileName}" 프로파일을 삭제하시겠습니까?\n연결된 스키마도 함께 삭제됩니다.`)) {
      deleteMutation.mutate(profile.profileId);
    }
  };

  const handleToggle = (profile: DataProfile) => {
    toggleMutation.mutate(profile.profileId);
  };

  const sortedProfiles = [...profiles].sort((a, b) => a.displayOrder - b.displayOrder);

  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 헤더 */}
      <div className="flex justify-between items-center">
        <div>
          <h3 className="text-lg font-semibold text-gray-900">데이터 프로파일</h3>
          <p className="text-sm text-gray-600">
            {dataSourceName}의 분석 목적별 데이터 프로파일을 관리합니다.
          </p>
        </div>
        <LoadingButton
          onClick={() => setIsCreating(true)}
          size="small"
          className="inline-flex items-center gap-2"
        >
          <PlusIcon className="h-4 w-4" />
          프로파일 추가
        </LoadingButton>
      </div>

      {/* 프로파일 목록 */}
      {sortedProfiles.length === 0 ? (
        <div className="text-center py-12 bg-gray-50 rounded-xl">
          <div className="text-gray-400 mb-4">
            <svg className="mx-auto h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          </div>
          <h3 className="text-lg font-medium text-gray-900 mb-2">
            프로파일이 없습니다
          </h3>
          <p className="text-gray-600 mb-4">
            첫 번째 데이터 프로파일을 생성해보세요.
          </p>
          <LoadingButton
            onClick={() => setIsCreating(true)}
            size="small"
            className="inline-flex items-center gap-2"
          >
            <PlusIcon className="h-4 w-4" />
            프로파일 추가
          </LoadingButton>
        </div>
      ) : (
        <div className="space-y-4">
          {sortedProfiles.map((profile) => (
            <div
              key={profile.profileId}
              className="bg-white border border-gray-200 rounded-xl p-4 hover:shadow-md transition-shadow"
            >
              <div className="flex items-center justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <h4 className="font-medium text-gray-900">
                      {profile.profileName}
                    </h4>
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        PURPOSE_COLORS[profile.profilePurpose]
                      }`}
                    >
                      {PURPOSE_LABELS[profile.profilePurpose]}
                    </span>
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        profile.isActive
                          ? "bg-green-100 text-green-800"
                          : "bg-gray-100 text-gray-800"
                      }`}
                    >
                      {profile.isActive ? "활성" : "비활성"}
                    </span>
                  </div>
                  {profile.description && (
                    <p className="text-sm text-gray-600">{profile.description}</p>
                  )}
                </div>
                
                <div className="flex items-center gap-2 ml-4">
                  <button
                    onClick={() => handleToggle(profile)}
                    disabled={toggleMutation.isPending}
                    className="p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100 transition-colors"
                    title={profile.isActive ? "비활성화" : "활성화"}
                  >
                    {profile.isActive ? (
                      <EyeIcon className="h-4 w-4" />
                    ) : (
                      <EyeSlashIcon className="h-4 w-4" />
                    )}
                  </button>
                  <button
                    onClick={() => setEditingProfile(profile)}
                    className="p-2 text-gray-400 hover:text-blue-600 rounded-lg hover:bg-blue-50 transition-colors"
                    title="수정"
                  >
                    <PencilIcon className="h-4 w-4" />
                  </button>
                  <button
                    onClick={() => handleDelete(profile)}
                    disabled={deleteMutation.isPending}
                    className="p-2 text-gray-400 hover:text-red-600 rounded-lg hover:bg-red-50 transition-colors"
                    title="삭제"
                  >
                    <TrashIcon className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* 프로파일 생성 폼 */}
      {isCreating && (
        <ProfileForm
          dataSourceId={dataSourceId}
          onSubmit={(data) => createMutation.mutate(data as DataProfileCreateRequest)}
          onCancel={() => setIsCreating(false)}
          isLoading={createMutation.isPending}
          title="프로파일 생성"
        />
      )}

      {/* 프로파일 수정 폼 */}
      {editingProfile && (
        <ProfileForm
          dataSourceId={dataSourceId}
          profile={editingProfile}
          onSubmit={(data) =>
            updateMutation.mutate({
              profileId: editingProfile.profileId,
              data,
            })
          }
          onCancel={() => setEditingProfile(null)}
          isLoading={updateMutation.isPending}
          title="프로파일 수정"
        />
      )}
    </div>
  );
}
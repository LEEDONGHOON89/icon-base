"use client";

import { useState, useEffect } from "react";
import { useQueryWithErrorHandling } from "@/hooks/useQueryWithErrorHandling";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  fetchDataProfiles,
  fetchStandardFields,
  createDataProfile,
  updateDataProfile,
  DataProfile,
  DataProfileCreateRequest,
  DataProfileUpdateRequest,
  DetectKeyType,
} from "@/app/data-sources/api";
import ProfileListPanel from "./ProfileListPanel";
import ProfileDetailPanel from "./ProfileDetailPanel";
import ProfileCreateModal, { ProfileCreateData } from "./ProfileCreateModal";
import { toast } from "react-hot-toast";
import { useErrorHandling } from "@/hooks/useErrorHandling";

interface ProfileManagementViewProps {
  dataSourceId: string;
  dataSourceName: string;
}

export default function ProfileManagementView({
  dataSourceId,
}: ProfileManagementViewProps) {
  const [selectedProfile, setSelectedProfile] = useState<DataProfile | null>(
    null
  );
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const queryClient = useQueryClient();
  const { handleError } = useErrorHandling();

  // 프로파일 목록 조회
  const { data: profiles = [] } = useQueryWithErrorHandling({
    queryKey: ["schemaProfiles", dataSourceId],
    queryFn: () => fetchDataProfiles(dataSourceId),
  });

  // 표준 필드 조회
  const { data: standardFields = [] } = useQueryWithErrorHandling({
    queryKey: ["standardFields"],
    queryFn: fetchStandardFields,
  });


  // 프로파일 생성 mutation
  const createProfileMutation = useMutation({
    mutationFn: (data: DataProfileCreateRequest) => createDataProfile(data),
    onSuccess: async (newProfile) => {
      toast.success("프로파일이 생성되었습니다.");

      // DEFAULT 프로파일인 경우 원본 스키마를 바로 사용
      if (newProfile.profileName === "DEFAULT") {
        toast.success("DEFAULT 프로파일이 생성되었습니다.");
      }

      queryClient.invalidateQueries({
        queryKey: ["schemaProfiles", dataSourceId],
      });
      setIsCreateModalOpen(false);
      setSelectedProfile(newProfile);
    },
    onError: handleError,
  });

  // 첫 프로파일 자동 선택
  useEffect(() => {
    if (profiles.length > 0 && !selectedProfile) {
      setSelectedProfile(profiles[0]);
    }
  }, [profiles, selectedProfile]);


  const handleSelectProfile = (profile: DataProfile) => {
    setSelectedProfile(profile);
  };

  const handleAddProfile = () => {
    setIsCreateModalOpen(true);
  };

  const handleCreateProfile = (data: ProfileCreateData) => {
    const createRequest: DataProfileCreateRequest = {
      dataSourceId,
      profileName: data.profileName,
      profilePurpose: data.profilePurpose,
      description: data.description,
      displayOrder: data.displayOrder,
    };

    createProfileMutation.mutate(createRequest);
  };

  const handleSave = async (
    detectKey?: string,
    detectKeyType?: DetectKeyType,
    destinationType?: string,
    entityType?: string,
    entityIdField?: string,
    storeFields?: string[]
  ) => {
    if (!selectedProfile) return;

    try {
      // 프로파일 업데이트
      const profileUpdateRequest: DataProfileUpdateRequest = {
        profileName: selectedProfile.profileName,
        profilePurpose: selectedProfile.profilePurpose,
        description: selectedProfile.description,
        displayOrder: selectedProfile.displayOrder,
        detectKey: detectKey || undefined,
        detectKeyType: detectKeyType || undefined,
        destinationType: destinationType || undefined,
        entityType: entityType || undefined,
        entityIdField: entityIdField || undefined,
        storeFields: storeFields || undefined,
      };

      await updateDataProfile(selectedProfile.profileId, profileUpdateRequest);

      // 선택된 프로파일 정보 업데이트
      setSelectedProfile({
        ...selectedProfile,
        detectKey,
        detectKeyType,
        destinationType,
        entityType,
        entityIdField,
        storeFields,
      });

      // 프로파일 목록 쿼리 무효화
      queryClient.invalidateQueries({
        queryKey: ["schemaProfiles", dataSourceId],
      });

      toast.success("프로파일이 저장되었습니다.");
    } catch (error) {
      console.error("저장 실패:", error);
      handleError(error);
    }
  };

  return (
    <>
      <div className="flex h-[600px] border border-gray-200 rounded-lg overflow-hidden">
        <ProfileListPanel
          profiles={profiles}
          selectedProfileId={selectedProfile?.profileId || null}
          onSelectProfile={handleSelectProfile}
          onAddProfile={handleAddProfile}
          totalFieldCount={0}
          showFieldCount={false}
        />

        <ProfileDetailPanel
          profile={selectedProfile}
          standardFields={standardFields}
          onSave={handleSave}
        />
      </div>

      <ProfileCreateModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSubmit={handleCreateProfile}
        isLoading={createProfileMutation.isPending}
      />
    </>
  );
}

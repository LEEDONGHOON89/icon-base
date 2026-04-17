"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "react-hot-toast";
import {
  PlusIcon,
  PencilIcon,
  TrashIcon,
  CheckCircleIcon,
  XCircleIcon,
} from "@heroicons/react/24/outline";
import {
  fetchDetectionAreas,
  createDetectionArea,
  updateDetectionArea,
  deleteDetectionArea,
  activateDetectionArea,
  deactivateDetectionArea,
  type DetectionArea,
  type CreateDetectionAreaRequest,
  type UpdateDetectionAreaRequest,
} from "./api";
import { DomainForm } from "./DomainForm";
import { Modal } from "@/components/ui/Modal";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";

export default function DetectionAreasPage() {
  const queryClient = useQueryClient();
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [editingDetectionArea, setEditingDetectionArea] = useState<DetectionArea | null>(
    null
  );
  const [deletingDetectionArea, setDeletingDetectionArea] = useState<DetectionArea | null>(
    null
  );

  // 도메인 목록 조회
  const { data: detectionAreas = [], isLoading } = useQuery({
    queryKey: ["detectionAreas"],
    queryFn: fetchDetectionAreas,
  });

  // 생성 mutation
  const createMutation = useMutation({
    mutationFn: createDetectionArea,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["detectionAreas"] });
      toast.success("도메인이 생성되었습니다");
      setIsCreateModalOpen(false);
    },
    onError: (error: any) => {
      toast.error(
        error.response?.data?.message || "도메인 생성에 실패했습니다"
      );
    },
  });

  // 수정 mutation
  const updateMutation = useMutation({
    mutationFn: ({
      detectionAreaId,
      data,
    }: {
      detectionAreaId: string;
      data: UpdateDetectionAreaRequest;
    }) => updateDetectionArea(detectionAreaId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["detectionAreas"] });
      toast.success("도메인이 수정되었습니다");
      setEditingDetectionArea(null);
    },
    onError: (error: any) => {
      toast.error(
        error.response?.data?.message || "도메인 수정에 실패했습니다"
      );
    },
  });

  // 삭제 mutation
  const deleteMutation = useMutation({
    mutationFn: deleteDetectionArea,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["detectionAreas"] });
      toast.success("도메인이 삭제되었습니다");
      setDeletingDetectionArea(null);
    },
    onError: (error: any) => {
      toast.error(
        error.response?.data?.message || "도메인 삭제에 실패했습니다"
      );
    },
  });

  // 활성화/비활성화 mutation
  const toggleActiveMutation = useMutation({
    mutationFn: ({
      detectionAreaId,
      isActive,
    }: {
      detectionAreaId: string;
      isActive: boolean;
    }) => {
      return isActive
        ? deactivateDetectionArea(detectionAreaId)
        : activateDetectionArea(detectionAreaId);
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["detectionAreas"] });
      toast.success(
        variables.isActive
          ? "도메인이 비활성화되었습니다"
          : "도메인이 활성화되었습니다"
      );
    },
    onError: (error: any) => {
      toast.error(
        error.response?.data?.message || "상태 변경에 실패했습니다"
      );
    },
  });

  // 정렬된 도메인 목록 (displayOrder 기준)
  const sortedDetectionAreas = [...detectionAreas].sort(
    (a, b) => (a.displayOrder || 0) - (b.displayOrder || 0)
  );

  return (
    <div className="p-6 space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">도메인 설정</h1>
          <p className="text-sm text-gray-500 mt-1">
            도메인을 관리합니다. 도메인은 시나리오 분류의 기본이 됩니다.
          </p>
        </div>
        <button
          onClick={() => setIsCreateModalOpen(true)}
          className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <PlusIcon className="w-5 h-5 mr-2" />
          도메인 생성
        </button>
      </div>

      {/* 도메인 테이블 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        {isLoading ? (
          <div className="p-8 text-center text-gray-500">로딩 중...</div>
        ) : sortedDetectionAreas.length === 0 ? (
          <div className="p-8 text-center text-gray-500">
            등록된 도메인이 없습니다
          </div>
        ) : (
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  도메인 ID
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  도메인 이름
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  설명
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  아이콘
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  색상
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  표시 순서
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  상태
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  액션
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {sortedDetectionAreas.map((detectionArea) => (
                <tr key={detectionArea.detectionAreaId} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <span className="px-3 py-1 text-xs font-semibold text-blue-800 bg-blue-100 rounded-full">
                        {detectionArea.detectionAreaId}
                      </span>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">
                      {detectionArea.areaName}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="text-sm text-gray-500 max-w-xs truncate">
                      {detectionArea.description || "-"}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {detectionArea.icon || "-"}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {detectionArea.color ? (
                      <span
                        className="inline-flex items-center px-2 py-1 rounded text-xs font-medium"
                        style={{
                          backgroundColor: `${detectionArea.color}20`,
                          color: detectionArea.color,
                        }}
                      >
                        {detectionArea.color}
                      </span>
                    ) : (
                      <span className="text-sm text-gray-500">-</span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {detectionArea.displayOrder ?? "-"}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <button
                      onClick={() =>
                        toggleActiveMutation.mutate({
                          detectionAreaId: detectionArea.detectionAreaId,
                          isActive: detectionArea.isActive,
                        })
                      }
                      disabled={toggleActiveMutation.isPending}
                      className={`inline-flex items-center px-2.5 py-1.5 rounded-full text-xs font-medium ${
                        detectionArea.isActive
                          ? "bg-green-100 text-green-800 hover:bg-green-200"
                          : "bg-gray-100 text-gray-800 hover:bg-gray-200"
                      } transition-colors disabled:opacity-50`}
                    >
                      {detectionArea.isActive ? (
                        <>
                          <CheckCircleIcon className="w-4 h-4 mr-1" />
                          활성
                        </>
                      ) : (
                        <>
                          <XCircleIcon className="w-4 h-4 mr-1" />
                          비활성
                        </>
                      )}
                    </button>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <button
                      onClick={() => setEditingDetectionArea(detectionArea)}
                      className="text-blue-600 hover:text-blue-900 mr-3"
                    >
                      <PencilIcon className="w-5 h-5" />
                    </button>
                    <button
                      onClick={() => setDeletingDetectionArea(detectionArea)}
                      className="text-red-600 hover:text-red-900"
                    >
                      <TrashIcon className="w-5 h-5" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* 생성 모달 */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="도메인 생성"
      >
        <DomainForm
          onSubmit={async (data) => {
            await createMutation.mutateAsync(data as CreateDetectionAreaRequest);
          }}
          onCancel={() => setIsCreateModalOpen(false)}
        />
      </Modal>

      {/* 수정 모달 */}
      <Modal
        isOpen={!!editingDetectionArea}
        onClose={() => setEditingDetectionArea(null)}
        title="도메인 수정"
      >
        {editingDetectionArea && (
          <DomainForm
            initialData={editingDetectionArea}
            isEdit
            onSubmit={async (data) => {
              await updateMutation.mutateAsync({
                detectionAreaId: editingDetectionArea.detectionAreaId,
                data: data as UpdateDetectionAreaRequest,
              });
            }}
            onCancel={() => setEditingDetectionArea(null)}
          />
        )}
      </Modal>

      {/* 삭제 확인 다이얼로그 */}
      <ConfirmDialog
        isOpen={!!deletingDetectionArea}
        onClose={() => setDeletingDetectionArea(null)}
        onConfirm={() => {
          if (deletingDetectionArea) {
            deleteMutation.mutate(deletingDetectionArea.detectionAreaId);
          }
        }}
        title="도메인 삭제"
        message={`정말로 "${deletingDetectionArea?.areaName}" 도메인을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.`}
        confirmText="삭제"
        confirmColor="red"
      />
    </div>
  );
}

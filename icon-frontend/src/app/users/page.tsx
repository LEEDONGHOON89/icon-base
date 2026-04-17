"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  PlusIcon,
  PencilIcon,
  MagnifyingGlassIcon,
  UserIcon,
  EnvelopeIcon,
  IdentificationIcon,
  UserCircleIcon,
  FunnelIcon,
  LockClosedIcon,
  UsersIcon,
} from "@heroicons/react/24/outline";
import {
  changePassword,
  createUser,
  fetchUsers,
  PasswordChangeRequest,
  updateUser,
  User,
  UserCreateRequest,
  UserUpdateRequest,
} from "./api";
import Modal from "./Modal";
import UserFormTabs, { UserFormValues } from "./UserFormTabs";
import Alert from "@/components/common/Alert";
import LoadingButton from "@/components/common/LoadingButton";
import { useErrorHandling } from "@/hooks/useErrorHandling";
import { useQueryWithErrorHandling } from "@/hooks/useQueryWithErrorHandling";
import toast from "react-hot-toast";

export default function UsersPage() {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState("");
  const [filterType, setFilterType] = useState<"ALL" | "ACTIVE" | "INACTIVE">("ALL");
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [showForm, setShowForm] = useState(false);
  const { error: mutationError, handleError, clearError } = useErrorHandling();

  const {
    data: usersResponse,
    isLoading,
    errorQuery: errorQuery,
  } = useQueryWithErrorHandling({
    queryKey: ["users"],
    queryFn: fetchUsers,
  });

  const users = usersResponse?.data || [];

  // 검색 및 필터링
  const filteredUsers = users.filter((user) => {
    const searchLower = searchTerm.toLowerCase();
    const matchesSearch = 
      user.loginId.toLowerCase().includes(searchLower) ||
      user.userName.toLowerCase().includes(searchLower) ||
      user.email.toLowerCase().includes(searchLower) ||
      (user.description && user.description.toLowerCase().includes(searchLower));
    
    // 여기서는 active/inactive 필터가 없으므로 검색만 적용
    return matchesSearch;
  });

  // 생성
  const createMutation = useMutation({
    mutationFn: (data: UserCreateRequest) => createUser(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
      setShowForm(false);
      toast.success("사용자가 생성되었습니다.");
    },
    onError: handleError,
  });

  // 수정
  const updateMutation = useMutation({
    mutationFn: ({
      userId,
      data,
    }: {
      userId: string;
      data: UserUpdateRequest;
    }) => updateUser(userId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
      setEditingUser(null);
      setShowForm(false);
      toast.success("사용자 정보가 수정되었습니다.");
    },
    onError: handleError,
  });

  // 비밀번호 변경
  const passwordMutation = useMutation({
    mutationFn: ({
      userId,
      data,
    }: {
      userId: string;
      data: PasswordChangeRequest;
    }) => changePassword(userId, data),
    onSuccess: () => {
      toast.success("비밀번호가 성공적으로 변경되었습니다.");
      queryClient.invalidateQueries({ queryKey: ["users"] });
      setEditingUser(null);
      setShowForm(false);
    },
    onError: handleError,
  });

  // 폼 제출 핸들러
  const handleSubmit = (values: UserFormValues) => {
    clearError();
    if (editingUser) {
      const { confirmPassword: _, password: __, ...updateData } = values;
      updateMutation.mutate({
        userId: editingUser.userId,
        data: updateData as UserUpdateRequest,
      });
    } else {
      createMutation.mutate(values as UserCreateRequest);
    }
  };

  // 비밀번호 변경 핸들러
  const handlePasswordChange = (values: {
    password: string;
    confirmPassword: string;
  }) => {
    if (editingUser) {
      passwordMutation.mutate({
        userId: editingUser.userId,
        data: { password: values.password },
      });
    }
  };

  // 수정 버튼 클릭
  const handleEdit = (user: User) => {
    setEditingUser(user);
    setShowForm(true);
    clearError();
  };

  // 새 유저 등록 버튼 클릭
  const handleNew = () => {
    setEditingUser(null);
    setShowForm(true);
    clearError();
  };

  // 현재 표시할 에러
  const displayError = errorQuery || mutationError;

  if (isLoading) {
    return (
      <div className="p-6 space-y-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200">
          <div className="flex justify-center items-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">사용자 관리</h1>
          <p className="text-sm text-gray-500 mt-1">
            시스템 사용자를 관리하고 권한을 설정합니다.
          </p>
        </div>
        <LoadingButton
          onClick={handleNew}
          type="button"
          size="small"
          className="inline-flex items-center gap-2 bg-blue-600 hover:bg-blue-700"
        >
          <PlusIcon className="h-5 w-5" />
          새 사용자 등록
        </LoadingButton>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
        <div className="relative">
          <MagnifyingGlassIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            type="text"
            placeholder="이름, ID, 이메일로 검색..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          />
        </div>
      </div>

      {/* 에러 표시 */}
      {displayError && (
        <Alert message={displayError.message} />
      )}

      {/* User List */}
      {!isLoading && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            {filteredUsers.length === 0 ? (
              <div className="text-center py-12">
                <UsersIcon className="mx-auto h-12 w-12 text-gray-400" />
                <h3 className="mt-2 text-sm font-medium text-gray-900">
                  사용자가 없습니다
                </h3>
                <p className="mt-1 text-sm text-gray-500">
                  {searchTerm ? "검색 결과가 없습니다." : "새 사용자를 등록하여 시작하세요."}
                </p>
              </div>
            ) : (
              <div className="grid gap-4">
                {filteredUsers.map((user) => (
                  <div
                    key={user.userId}
                    className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 hover:shadow-lg transition-shadow duration-200"
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex items-start space-x-4">
                        <div className="flex-shrink-0">
                          <div className="p-3 bg-blue-100 rounded-lg">
                            <UserCircleIcon className="h-6 w-6 text-blue-600" />
                          </div>
                        </div>
                        <div className="flex-1">
                          <h3 className="text-lg font-semibold text-gray-900">
                            {user.userName}
                          </h3>
                          <p className="text-sm text-gray-600">
                            @{user.loginId}
                          </p>
                          <div className="mt-2 flex items-center space-x-4 text-sm">
                            <span className="flex items-center text-gray-500">
                              <EnvelopeIcon className="h-4 w-4 mr-1" />
                              {user.email}
                            </span>
                          </div>
                          {user.description && (
                            <p className="mt-2 text-sm text-gray-600">
                              {user.description}
                            </p>
                          )}
                        </div>
                      </div>
                      <div className="flex items-center space-x-3 ml-4">
                        <button
                          onClick={() => handleEdit(user)}
                          className="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
                          title="수정"
                        >
                          <PencilIcon className="h-5 w-5" />
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
        </div>
      )}

      {/* 모달 */}
      <Modal
        open={showForm}
        onClose={() => {
          setShowForm(false);
          setEditingUser(null);
          clearError();
        }}
        title={editingUser ? "사용자 수정" : "새 사용자 등록"}
      >
        <UserFormTabs
          onSubmit={handleSubmit}
          onPasswordChange={handlePasswordChange}
          defaultValues={editingUser ?? {}}
          isEdit={!!editingUser}
          isPending={
            createMutation.isPending ||
            updateMutation.isPending ||
            passwordMutation.isPending
          }
          error={displayError?.message}
        />
      </Modal>
    </div>
  );
}
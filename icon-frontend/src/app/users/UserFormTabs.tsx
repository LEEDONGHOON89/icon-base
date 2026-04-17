import { useState } from "react";
import { UserIcon, LockClosedIcon } from "@heroicons/react/24/outline";
import UserInfoForm, { UserFormValues } from "./components/UserInfoForm";
import PasswordChangeForm from "./components/PasswordChangeForm";

// UserFormValues를 re-export
export type { UserFormValues } from "./components/UserInfoForm";

interface UserFormTabsProps {
  onSubmit: (values: UserFormValues) => void;
  onPasswordChange?: (values: {
    password: string;
    confirmPassword: string;
  }) => void;
  defaultValues?: Partial<UserFormValues>;
  isEdit?: boolean;
  isPending?: boolean;
  error?: string | null;
}

/**
 * 사용자 생성/수정 폼 (탭 구조)
 * - isEdit: true면 수정, false면 생성
 * - defaultValues: 수정 시 기존 값
 * - isPending: 로딩 상태
 * - error: 에러 메시지
 */
export default function UserFormTabs({
  onSubmit,
  onPasswordChange,
  defaultValues = {},
  isEdit = false,
  isPending = false,
  error,
}: UserFormTabsProps) {
  const [activeTab, setActiveTab] = useState<"info" | "password">("info");

  // 탭 스타일
  const tabClass = (isActive: boolean) =>
    `flex-1 py-3 px-6 text-sm font-medium rounded-t-xl transition-all duration-200 ${isActive
      ? "bg-white text-blue-600 border-b-2 border-blue-600"
      : "bg-gray-100 text-gray-600 hover:bg-gray-200"
    }`;

  return (
    <div className="w-full">
      {/* 탭 헤더 - 수정 모드일 때만 표시 */}
      {isEdit && (
        <div className="flex space-x-2 mb-2">
          <button
            type="button"
            onClick={() => setActiveTab("info")}
            className={tabClass(activeTab === "info")}
          >
            <div className="flex items-center justify-center">
              <UserIcon className="w-4 h-4 mr-2" />
              기본 정보
            </div>
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("password")}
            className={tabClass(activeTab === "password")}
          >
            <div className="flex items-center justify-center">
              <LockClosedIcon className="w-4 h-4 mr-2" />
              비밀번호 변경
            </div>
          </button>
        </div>
      )}

      {/* 탭 컨텐츠 */}
      <div className="bg-white rounded-2xl shadow-lg p-8">
        {/* 기본 정보 탭 */}
        {(!isEdit || activeTab === "info") && (
          <UserInfoForm
            onSubmit={onSubmit}
            defaultValues={defaultValues}
            isEdit={isEdit}
            isPending={isPending}
            error={error}
          />
        )}

        {/* 비밀번호 변경 탭 - 수정 모드일 때만 */}
        {isEdit && activeTab === "password" && (
          <PasswordChangeForm
            defaultValues={defaultValues}
            onSubmit={onPasswordChange!}
            isPending={isPending}
            error={error}
          />
        )}
      </div>
    </div>
  );
}

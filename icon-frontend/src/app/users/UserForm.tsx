import { useForm } from "react-hook-form";
import { useState } from "react";
import { UserCreateRequest } from "./api";
import Input from "@/components/common/Input";
import LoadingButton from "@/components/common/LoadingButton";
import Alert from "@/components/common/Alert";

// UserFormValues는 UserCreateRequest + confirmPassword
export type UserFormValues = UserCreateRequest & { confirmPassword?: string };

interface UserFormProps {
  onSubmit: (values: UserFormValues) => void;
  defaultValues?: Partial<UserFormValues>;
  isEdit?: boolean;
  isPending?: boolean;
  error?: string | null;
}

/**
 * 사용자 생성/수정 폼 (공통)
 * - isEdit: true면 수정, false면 생성
 * - defaultValues: 수정 시 기존 값
 * - isPending: 로딩 상태
 * - error: 에러 메시지
 */
export default function UserForm({
  onSubmit,
  defaultValues = {},
  isEdit = false,
  isPending = false,
  error,
}: UserFormProps) {
  const [showPasswordChange, setShowPasswordChange] = useState(false);
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<UserFormValues>({
    defaultValues,
  });

  const password = watch("password");

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <Input
        label="로그인 ID"
        {...register("loginId", {
          required: !isEdit ? "로그인 ID는 필수입니다." : false,
          minLength: { value: 4, message: "아이디는 4자 이상" },
          maxLength: { value: 20, message: "아이디는 20자 이하" },
        })}
        disabled={isEdit}
        placeholder={"아이디"}
      />
      {!isEdit && errors.loginId?.message && (
        <Alert message={errors.loginId.message as string} />
      )}

      {/* 비밀번호 변경 토글 (수정 시에만 표시) */}
      {isEdit && (
        <div className="flex items-center gap-2">
          <input
            type="checkbox"
            id="showPasswordChange"
            checked={showPasswordChange}
            onChange={(e) => setShowPasswordChange(e.target.checked)}
            className="w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500"
          />
          <label htmlFor="showPasswordChange" className="text-sm font-medium text-gray-700">
            비밀번호 변경
          </label>
        </div>
      )}

      {/* 비밀번호/비밀번호 확인: 생성 시 또는 수정 시 토글 활성화 시 노출 */}
      {(!isEdit || showPasswordChange) && (
        <>
          <Input
            label="비밀번호"
            type="password"
            {...register("password", {
              required: !isEdit || showPasswordChange ? "비밀번호는 필수입니다." : false,
              minLength: { value: 8, message: "비밀번호는 8자 이상" },
              maxLength: { value: 20, message: "비밀번호는 20자 이하" },
            })}
            placeholder={"비밀번호"}
          />
          {errors.password?.message && (
            <Alert message={errors.password.message as string} />
          )}

          <Input
            label="비밀번호 확인"
            type="password"
            {...register("confirmPassword", {
              required: !isEdit || showPasswordChange ? "비밀번호 확인은 필수입니다." : false,
              validate: (value) =>
                value === password || "비밀번호가 일치하지 않습니다.",
            })}
            placeholder={"비밀번호 확인"}
          />
          {errors.confirmPassword?.message && (
            <Alert message={errors.confirmPassword.message as string} />
          )}
        </>
      )}

      <Input
        label="이름"
        {...register("userName", { required: "이름은 필수입니다." })}
        placeholder={"이름"}
      />
      {errors.userName?.message && (
        <Alert message={errors.userName.message as string} />
      )}

      <Input
        label="이메일"
        type="email"
        {...register("email")}
        placeholder={"이메일"}
      />

      <Input label="설명" {...register("description")} placeholder={"설명"} />

      {error && <Alert message={error} />}

      <LoadingButton loading={isPending} type="submit" size="medium" className="w-full">
        {isEdit ? "수정하기" : "등록하기"}
      </LoadingButton>
    </form>
  );
}

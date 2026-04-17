import { useForm } from "react-hook-form";
import Input from "@/components/common/Input";
import LoadingButton from "@/components/common/LoadingButton";
import Alert from "@/components/common/Alert";
import {
  UserIcon,
  LockClosedIcon,
  EnvelopeIcon,
  UserCircleIcon,
  InformationCircleIcon,
} from "@heroicons/react/24/outline";
import { UserCreateRequest } from "../api";

// UserFormValues는 UserCreateRequest + confirmPassword
export type UserFormValues = UserCreateRequest & { confirmPassword?: string };

interface UserInfoFormProps {
  onSubmit: (values: UserFormValues) => void;
  defaultValues?: Partial<UserFormValues>;
  isEdit?: boolean;
  isPending?: boolean;
  error?: string | null;
}

/**
 * 사용자 기본 정보 폼 컴포넌트
 * - 생성/수정 모두 지원
 * - 수정 시에는 로그인 ID 변경 불가, 비밀번호 필드 숨김
 */
export default function UserInfoForm({
  onSubmit,
  defaultValues = {},
  isEdit = false,
  isPending = false,
  error,
}: UserInfoFormProps) {
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
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <div className="space-y-4">
        <div>
          <div className="flex items-center space-x-2 mb-2">
            <UserCircleIcon className="w-5 h-5 text-blue-600" />
            <label className="text-sm font-medium text-gray-700">
              로그인 ID
            </label>
          </div>
          <Input
            {...register("loginId", {
              required: !isEdit ? "로그인 ID는 필수입니다." : false,
              minLength: { value: 4, message: "아이디는 4자 이상" },
              maxLength: { value: 20, message: "아이디는 20자 이하" },
            })}
            disabled={isEdit}
            placeholder="아이디를 입력하세요"
            error={errors.loginId?.message}
          />
        </div>

        {/* 생성 모드일 때만 비밀번호 필드 표시 */}
        {!isEdit && (
          <>
            <div>
              <div className="flex items-center space-x-2 mb-2">
                <LockClosedIcon className="w-5 h-5 text-green-600" />
                <label className="text-sm font-medium text-gray-700">
                  비밀번호
                </label>
              </div>
              <Input
                type="password"
                {...register("password", {
                  required: "비밀번호는 필수입니다.",
                  minLength: { value: 8, message: "비밀번호는 8자 이상" },
                  maxLength: { value: 20, message: "비밀번호는 20자 이하" },
                })}
                placeholder="비밀번호를 입력하세요"
                error={errors.password?.message}
              />
            </div>

            <div>
              <div className="flex items-center space-x-2 mb-2">
                <LockClosedIcon className="w-5 h-5 text-green-600" />
                <label className="text-sm font-medium text-gray-700">
                  비밀번호 확인
                </label>
              </div>
              <Input
                type="password"
                {...register("confirmPassword", {
                  required: "비밀번호 확인은 필수입니다.",
                  validate: (value) =>
                    value === password || "비밀번호가 일치하지 않습니다.",
                })}
                placeholder="비밀번호를 다시 입력하세요"
                error={errors.confirmPassword?.message}
              />
            </div>
          </>
        )}

        <div>
          <div className="flex items-center space-x-2 mb-2">
            <UserIcon className="w-5 h-5 text-purple-600" />
            <label className="text-sm font-medium text-gray-700">이름</label>
          </div>
          <Input
            {...register("userName", {
              required: "이름은 필수입니다.",
            })}
            placeholder="이름을 입력하세요"
            error={errors.userName?.message}
          />
        </div>

        <div>
          <div className="flex items-center space-x-2 mb-2">
            <EnvelopeIcon className="w-5 h-5 text-amber-600" />
            <label className="text-sm font-medium text-gray-700">이메일</label>
          </div>
          <Input
            type="email"
            {...register("email", {
              pattern: {
                value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                message: "올바른 이메일 형식이 아닙니다.",
              },
            })}
            placeholder="이메일을 입력하세요"
            error={errors.email?.message}
          />
        </div>

        <div>
          <div className="flex items-center space-x-2 mb-2">
            <InformationCircleIcon className="w-5 h-5 text-indigo-600" />
            <label className="text-sm font-medium text-gray-700">설명</label>
          </div>
          <textarea
            {...register("description")}
            placeholder="추가 설명을 입력하세요"
            rows={3}
            className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors resize-none"
          />
        </div>
      </div>

      {error && (
        <div className="mt-4">
          <Alert message={error} />
        </div>
      )}

      <LoadingButton
        loading={isPending}
        type="submit"
        size="medium"
        className="w-full bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700"
      >
        <UserIcon className="w-5 h-5 mr-2" />
        {isEdit ? "정보 수정하기" : "사용자 등록하기"}
      </LoadingButton>
    </form>
  );
}
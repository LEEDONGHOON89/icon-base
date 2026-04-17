import { useForm } from "react-hook-form";
import Input from "@/components/common/Input";
import LoadingButton from "@/components/common/LoadingButton";
import Alert from "@/components/common/Alert";
import {
  LockClosedIcon,
  InformationCircleIcon,
} from "@heroicons/react/24/outline";
import { UserFormValues } from "../UserForm";

interface PasswordChangeFormProps {
  defaultValues?: Partial<UserFormValues>;
  onSubmit: (values: { password: string; confirmPassword: string }) => void;
  isPending?: boolean;
  error?: string | null;
}

/**
 * 비밀번호 변경 폼 컴포넌트
 * - 새 비밀번호와 확인 입력
 * - 복잡도 검증 (영문, 숫자, 특수문자 포함)
 */
export default function PasswordChangeForm({
  defaultValues = {},
  onSubmit,
  isPending = false,
  error,
}: PasswordChangeFormProps) {
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<{
    password: string;
    confirmPassword: string;
  }>();

  const passwordValue = watch("password");

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <div className="mb-4 p-4 bg-amber-50 rounded-xl border border-amber-200">
        <div className="flex items-center space-x-2">
          <InformationCircleIcon className="w-5 h-5 text-amber-600" />
          <p className="text-sm text-amber-700 font-medium">
            비밀번호 변경 시 보안을 위해 다시 로그인이 필요할 수 있습니다.
          </p>
        </div>
      </div>
      <div className={"mb-2 font-bold text-lg space-y-4"}>
        {defaultValues.userName}
      </div>

      <div className="space-y-4">
        <div>
          <div className="flex items-center space-x-2 mb-2">
            <LockClosedIcon className="w-5 h-5 text-green-600" />
            <label className="text-sm font-medium text-gray-700">
              새 비밀번호
            </label>
          </div>
          <Input
            type="password"
            {...register("password", {
              required: "새 비밀번호는 필수입니다.",
              minLength: {
                value: 8,
                message: "비밀번호는 8자 이상이어야 합니다.",
              },
              maxLength: {
                value: 20,
                message: "비밀번호는 20자 이하여야 합니다.",
              },
              pattern: {
                value: /^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*])/,
                message: "영문, 숫자, 특수문자를 포함해야 합니다.",
              },
            })}
            placeholder="새 비밀번호를 입력하세요"
            error={errors.password?.message}
          />
          <p className="mt-1 text-xs text-gray-500">
            영문, 숫자, 특수문자를 포함하여 8자 이상
          </p>
        </div>

        <div>
          <div className="flex items-center space-x-2 mb-2">
            <LockClosedIcon className="w-5 h-5 text-green-600" />
            <label className="text-sm font-medium text-gray-700">
              새 비밀번호 확인
            </label>
          </div>
          <Input
            type="password"
            {...register("confirmPassword", {
              required: "비밀번호 확인은 필수입니다.",
              validate: (value) =>
                value === passwordValue || "비밀번호가 일치하지 않습니다.",
            })}
            placeholder="새 비밀번호를 다시 입력하세요"
            error={errors.confirmPassword?.message}
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
        className="w-full bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-700 hover:to-emerald-700"
      >
        <LockClosedIcon className="w-5 h-5 mr-2" />
        비밀번호 변경하기
      </LoadingButton>
    </form>
  );
}

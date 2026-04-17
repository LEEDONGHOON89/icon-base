import { useForm } from "react-hook-form";
import { CreateRelationRuleRequest } from "./api";
import Input from "@/components/common/Input";
import LoadingButton from "@/components/common/LoadingButton";
import Alert from "@/components/common/Alert";

export type RelationRuleFormValues = CreateRelationRuleRequest;

interface RelationRuleFormProps {
  onSubmit: (values: RelationRuleFormValues) => void;
  defaultValues?: Partial<RelationRuleFormValues>;
  isEdit?: boolean;
  isPending?: boolean;
  error?: string | null;
}

// 관계 타입 옵션
const RELATION_TYPES = [
  { value: "OWNS", label: "OWNS (소유)" },
  { value: "USES", label: "USES (사용)" },
  { value: "AUTHENTICATES", label: "AUTHENTICATES (인증)" },
  { value: "ACCESSES", label: "ACCESSES (접근)" },
];

// 엔티티 타입 옵션
const ENTITY_TYPES = [
  { value: "CUSTOMER", label: "CUSTOMER (고객)" },
  { value: "ACCOUNT", label: "ACCOUNT (계좌)" },
  { value: "DEVICE", label: "DEVICE (디바이스)" },
  { value: "AUTHENTICATION", label: "AUTHENTICATION (인증)" },
];

export default function RelationRuleForm({
  onSubmit,
  defaultValues = {},
  isEdit = false,
  isPending = false,
  error,
}: RelationRuleFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RelationRuleFormValues>({
    defaultValues,
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      {/* 데이터소스 ID */}
      <Input
        label="데이터소스 ID"
        {...register("dataSourceId", {
          required: "데이터소스 ID는 필수입니다.",
          maxLength: { value: 50, message: "데이터소스 ID는 50자 이하" },
        })}
        placeholder="예: P_AUTH"
      />
      {errors.dataSourceId?.message && (
        <Alert message={errors.dataSourceId.message as string} />
      )}

      {/* From 도메인 타입 */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          From 도메인 타입
        </label>
        <select
          {...register("fromEntityType", {
            required: "From 도메인 타입은 필수입니다.",
          })}
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        >
          <option value="">선택하세요</option>
          {ENTITY_TYPES.map((type) => (
            <option key={type.value} value={type.value}>
              {type.label}
            </option>
          ))}
        </select>
        {errors.fromEntityType?.message && (
          <Alert message={errors.fromEntityType.message as string} />
        )}
      </div>

      {/* From ID 필드명 */}
      <Input
        label="From ID 필드명"
        {...register("fromIdField", {
          required: "From ID 필드명은 필수입니다.",
          maxLength: { value: 100, message: "From ID 필드명은 100자 이하" },
        })}
        placeholder="예: cust_id"
      />
      {errors.fromIdField?.message && (
        <Alert message={errors.fromIdField.message as string} />
      )}

      {/* 관계 타입 */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          관계 타입
        </label>
        <select
          {...register("relationType", {
            required: "관계 타입은 필수입니다.",
          })}
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        >
          <option value="">선택하세요</option>
          {RELATION_TYPES.map((type) => (
            <option key={type.value} value={type.value}>
              {type.label}
            </option>
          ))}
        </select>
        {errors.relationType?.message && (
          <Alert message={errors.relationType.message as string} />
        )}
      </div>

      {/* To 도메인 타입 */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          To 도메인 타입
        </label>
        <select
          {...register("toEntityType", {
            required: "To 도메인 타입은 필수입니다.",
          })}
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        >
          <option value="">선택하세요</option>
          {ENTITY_TYPES.map((type) => (
            <option key={type.value} value={type.value}>
              {type.label}
            </option>
          ))}
        </select>
        {errors.toEntityType?.message && (
          <Alert message={errors.toEntityType.message as string} />
        )}
      </div>

      {/* To ID 필드명 */}
      <Input
        label="To ID 필드명"
        {...register("toIdField", {
          required: "To ID 필드명은 필수입니다.",
          maxLength: { value: 100, message: "To ID 필드명은 100자 이하" },
        })}
        placeholder="예: auth_id"
      />
      {errors.toIdField?.message && (
        <Alert message={errors.toIdField.message as string} />
      )}

      {/* 설명 */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          설명 (선택)
        </label>
        <textarea
          {...register("description", {
            maxLength: { value: 500, message: "설명은 500자 이하" },
          })}
          placeholder="규칙에 대한 설명을 입력하세요"
          rows={3}
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        />
        {errors.description?.message && (
          <Alert message={errors.description.message as string} />
        )}
      </div>

      {error && <Alert message={error} />}

      <LoadingButton
        loading={isPending}
        type="submit"
        size="medium"
        className="w-full"
      >
        {isEdit ? "수정하기" : "등록하기"}
      </LoadingButton>
    </form>
  );
}

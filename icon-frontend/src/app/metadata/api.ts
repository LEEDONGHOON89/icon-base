import api from "@/lib/api";

// 필드 메타데이터
export interface FieldMetadata {
  name: string;
  label: string;
  category: string;
  description?: string;
  valueOptions?: Array<{
    value: string;
    label: string;
  }>;
  availableOperators?: OperatorMetadata[]; // 이 필드에서 사용 가능한 연산자 목록
}

// 파라미터 정의
export interface ParameterDefinition {
  name: string; // 파라미터 이름
  type: string; // 파라미터 타입 (number, string, time, boolean)
  required: boolean; // 필수 여부
  description: string; // 파라미터 설명
  placeholder?: string; // 입력 힌트
  unit?: string; // 단위 (분, 원, 개 등)
  validation?: {
    // 유효성 검사 규칙
    min?: number;
    max?: number;
    minLength?: number;
    maxLength?: number;
    pattern?: string;
    format?: string;
  };
}

// 연산자 메타데이터
export interface OperatorMetadata {
  value: string; // 연산자 enum name (예: "GREATER_THAN_OR_EQUALS")
  symbol: string; // 연산자 기호 (예: ">=")
  label: string;
  category: string;
  description?: string;
  supportedTypes: string[];
  requiresParameters?: boolean;
  parameterFormat?: string;
  isAggregateOperator?: boolean;
  parameterDefinitions?: ParameterDefinition[];
}

// 룰 필드 메타데이터 조회
export const fetchRuleFields = async (): Promise<FieldMetadata[]> => {
  const response = await api.get<ResponseList<FieldMetadata>>("/api/v1/metadata/rule-fields");
  return response.data.data;
};

// 카테고리 메타데이터
export interface CategoryMetadata {
  value: string;
  label: string;
  description?: string;
}

// 룰 카테고리 메타데이터 조회
export const fetchRuleCategories = async (): Promise<CategoryMetadata[]> => {
  const response = await api.get<ResponseList<CategoryMetadata>>("/api/v1/metadata/rule-categories");
  return response.data.data;
};

// 데이터 소스 타입 메타데이터
export interface DataSourceTypeMetadata {
  value: string;
  label: string;
  description: string;
  iconType: string;
  isDatabaseType: boolean;
  isLogType: boolean;
  isFileBasedType: boolean;
  isStreamingType: boolean;
}

// 데이터 소스 타입 메타데이터 조회
export const fetchDataSourceTypes = async (): Promise<DataSourceTypeMetadata[]> => {
  const response = await api.get<ResponseList<DataSourceTypeMetadata>>("/api/v1/metadata/data-source-types");
  return response.data.data;
};

// 룰 도메인 메타데이터
export interface RuleDomainMetadata {
  value: string;
  label: string;
  description?: string;
  fieldDatetime: string;
}

// 룰 도메인 메타데이터 조회
export const fetchRuleDomains = async (): Promise<RuleDomainMetadata[]> => {
  const response = await api.get<ResponseList<RuleDomainMetadata>>("/api/v1/metadata/rule-domains");
  return response.data.data;
};

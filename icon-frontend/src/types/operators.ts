// 백엔드 RuleOperator enum과 동기화된 연산자 타입 정의

// 기본 비교 연산자
export const EQUALS = "=" as const;
export const NOT_EQUALS = "!=" as const;

// 숫자 비교 연산자
export const GREATER_THAN_OR_EQUALS = ">=" as const;
export const LESS_THAN_OR_EQUALS = "<=" as const;
export const BETWEEN = "between" as const;

// 시간 범위 연산자들 - 타입 세이프티를 위해 명확히 구분
export const HOUR_RANGE = "hour_range" as const;
export const MINUTE_RANGE = "minute_range" as const;
export const TIME_RANGE = "time_range" as const;

// 문자열 연산자
export const CONTAINS = "contains" as const;
export const NOT_CONTAINS = "not_contains" as const;

// 포함 관련 연산자
export const IN = "in" as const;
export const NOT_IN = "not_in" as const;

// 존재 여부 연산자
export const EXISTS = "exists" as const;
export const NOT_EXISTS = "not_exists" as const;
export const IS_NULL = "is_null" as const;
export const IS_NOT_NULL = "is_not_null" as const;

// 시간 관련 연산자
export const WITHIN = "within" as const;
export const BEFORE = "before" as const;
export const AFTER = "after" as const;
export const TIME_DIFF = "time_diff" as const;

// 집계 연산자
export const SUM_WITHIN = "sum_within" as const;
export const COUNT_WITHIN = "count_within" as const;
export const DISTINCT_COUNT = "distinct_count" as const;

// 모든 연산자 타입
export type RuleOperator =
  | typeof EQUALS
  | typeof NOT_EQUALS
  | typeof GREATER_THAN_OR_EQUALS
  | typeof LESS_THAN_OR_EQUALS
  | typeof BETWEEN
  | typeof HOUR_RANGE
  | typeof TIME_RANGE
  | typeof MINUTE_RANGE
  | typeof CONTAINS
  | typeof NOT_CONTAINS
  | typeof IN
  | typeof NOT_IN
  | typeof EXISTS
  | typeof NOT_EXISTS
  | typeof IS_NULL
  | typeof IS_NOT_NULL
  | typeof WITHIN
  | typeof BEFORE
  | typeof AFTER
  | typeof TIME_DIFF
  | typeof SUM_WITHIN
  | typeof COUNT_WITHIN
  | typeof DISTINCT_COUNT;

// 연산자 카테고리
export type OperatorCategory =
  | "기본 비교"
  | "범위"
  | "문자열"
  | "포함"
  | "존재"
  | "시간"
  | "집계";

// 연산자 정보 (메타데이터와 동일한 구조)
export interface OperatorInfo {
  value: RuleOperator;
  label: string;
  category: OperatorCategory;
  description?: string;
  supportedTypes: string[];
  requiresParameters?: boolean;
  parameterFormat?: string;
  isAggregateOperator?: boolean;
}

// 시간 범위 연산자 구분을 위한 헬퍼 함수
export const isTimeRangeOperator = (operator: string): boolean => {
  return (
    operator === HOUR_RANGE ||
    operator === TIME_RANGE ||
    operator === MINUTE_RANGE
  );
};

// HOUR_RANGE: 시간대를 숫자로 (예: "10,11" - 10시~11시)
export const isHourRangeOperator = (
  operator: string
): operator is typeof HOUR_RANGE => {
  return operator === HOUR_RANGE;
};

// TIME_RANGE: HH:mm 형식으로 정밀하게 (예: "10:30,11:45")
export const isTimeRangeWithMinutesOperator = (
  operator: string
): operator is typeof TIME_RANGE => {
  return operator === TIME_RANGE;
};

// MINUTE_RANGE: 분을 숫자로 (예: "15,45" - 15분~45분)
export const isMinuteRangeOperator = (
  operator: string
): operator is typeof MINUTE_RANGE => {
  return operator === MINUTE_RANGE;
};

// 파라미터가 필요한 연산자인지 확인
export const requiresParameter = (operator: RuleOperator): boolean => {
  const noParamOperators = [EXISTS, NOT_EXISTS, IS_NULL, IS_NOT_NULL];
  return !noParamOperators.includes(operator as any);
};

// 집계 연산자인지 확인
export const isAggregateOperator = (operator: RuleOperator): boolean => {
  const aggregateOperators = [SUM_WITHIN, COUNT_WITHIN, DISTINCT_COUNT];
  return aggregateOperators.includes(operator as any);
};

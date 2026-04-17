import {
  RuleOperator,
  HOUR_RANGE,
  TIME_RANGE,
  MINUTE_RANGE,
  BETWEEN,
  IN,
  NOT_IN,
  TIME_DIFF,
  SUM_WITHIN,
  COUNT_WITHIN,
  DISTINCT_COUNT,
} from "@/types/operators";

// 연산자별 입력 형식 가이드
export const getOperatorInputGuide = (
  operator: RuleOperator | string
): string => {
  switch (operator) {
    // 시간 범위 연산자들
    case HOUR_RANGE:
      return "시작시,종료시 (예: 10,11 - 10시부터 11시까지)";
    case TIME_RANGE:
      return "시작시간,종료시간 (예: 10:30,11:45)";
    case MINUTE_RANGE:
      return "시작분,종료분 (예: 15,45 - 15분부터 45분까지)";

    // 범위 연산자
    case BETWEEN:
      return "최솟값,최댓값 (예: 100,200)";

    // 리스트 연산자
    case IN:
    case NOT_IN:
      return "콤마로 구분된 값 목록 (예: value1,value2,value3)";

    // 시간 관련 연산자
    case TIME_DIFF:
      return "비교 필드명,임계값(분) (예: login_time,30)";

    // 집계 연산자
    case SUM_WITHIN:
      return "시간윈도우(분),임계값 (예: 60,1000000)";
    case COUNT_WITHIN:
      return "시간윈도우(분),횟수 (예: 30,5)";
    case DISTINCT_COUNT:
      return "시간윈도우(분),개수 (예: 60,10)";

    default:
      return "";
  }
};

// 연산자별 입력 예시
export const getOperatorExample = (
  operator: RuleOperator | string,
  fieldName?: string
): string => {
  // 필드에 따른 특별한 예시가 있는 경우
  if (fieldName) {
    switch (fieldName) {
      case "transaction_amount":
        switch (operator) {
          case ">=":
            return "1000000";
          case "<=":
            return "5000000";
          case BETWEEN:
            return "1000000,5000000";
          default:
            break;
        }
        break;

      case "transaction_hour":
        switch (operator) {
          case HOUR_RANGE:
            return "22,6"; // 밤 10시부터 새벽 6시
          case "=":
            return "23";
          default:
            break;
        }
        break;

      case "transaction_time":
        switch (operator) {
          case TIME_RANGE:
            return "22:00,06:00"; // 밤 10시부터 새벽 6시
          default:
            break;
        }
        break;
    }
  }

  // 일반적인 예시
  return getOperatorInputGuide(operator);
};

// 연산자에 따른 입력 타입 결정
export const getOperatorInputType = (
  operator: RuleOperator | string
): "text" | "number" | "select" => {
  // 숫자만 입력받는 연산자들
  const numberOperators = [">=", "<=", ">", "<", "=", "!="];
  if (numberOperators.includes(operator)) {
    return "number";
  }

  // 텍스트 입력이 필요한 연산자들
  return "text";
};

// 연산자별 유효성 검사(현재 백엔드에서 주기떄문에 쓰진 않지만 퍼포먼스때문에 써야할 수 있기 때문에 코드 유지)
export const validateOperatorValue = (
  operator: RuleOperator | string,
  value: string
): { valid: boolean; error?: string } => {
  if (!value || value.trim() === "") {
    return { valid: false, error: "값을 입력해주세요." };
  }

  switch (operator) {
    case HOUR_RANGE: {
      const parts = value.split(",");
      if (parts.length !== 2) {
        return {
          valid: false,
          error: "시작시와 종료시를 콤마로 구분해서 입력하세요. (예: 10,11)",
        };
      }
      const start = parseInt(parts[0]);
      const end = parseInt(parts[1]);
      if (
        isNaN(start) ||
        isNaN(end) ||
        start < 0 ||
        start > 23 ||
        end < 0 ||
        end > 23
      ) {
        return { valid: false, error: "시간은 0-23 사이의 숫자여야 합니다." };
      }
      break;
    }

    case TIME_RANGE: {
      const parts = value.split(",");
      if (parts.length !== 2) {
        return {
          valid: false,
          error:
            "시작시간과 종료시간을 콤마로 구분해서 입력하세요. (예: 10:30,11:45)",
        };
      }
      const timeRegex = /^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/;
      if (
        !timeRegex.test(parts[0].trim()) ||
        !timeRegex.test(parts[1].trim())
      ) {
        return { valid: false, error: "시간은 HH:mm 형식이어야 합니다." };
      }
      break;
    }

    case MINUTE_RANGE: {
      const parts = value.split(",");
      if (parts.length !== 2) {
        return {
          valid: false,
          error: "시작분과 종료분을 콤마로 구분해서 입력하세요. (예: 15,45)",
        };
      }
      const start = parseInt(parts[0]);
      const end = parseInt(parts[1]);
      if (
        isNaN(start) ||
        isNaN(end) ||
        start < 0 ||
        start > 59 ||
        end < 0 ||
        end > 59
      ) {
        return { valid: false, error: "분은 0-59 사이의 숫자여야 합니다." };
      }
      break;
    }

    case BETWEEN: {
      const parts = value.split(",");
      if (parts.length !== 2) {
        return {
          valid: false,
          error: "최솟값과 최댓값을 콤마로 구분해서 입력하세요. (예: 100,200)",
        };
      }
      const min = parseFloat(parts[0]);
      const max = parseFloat(parts[1]);
      if (isNaN(min) || isNaN(max)) {
        return { valid: false, error: "숫자를 입력해주세요." };
      }
      if (min > max) {
        return { valid: false, error: "최솟값은 최댓값보다 작아야 합니다." };
      }
      break;
    }
  }

  return { valid: true };
};

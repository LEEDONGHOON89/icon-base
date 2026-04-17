package com.itmasters.icon.common.domain.rule;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

import com.itmasters.icon.common.domain.type.FieldType;
import lombok.Getter;

/**
 * 룰 연산자 정의
 * 
 * ⚠️ 중요: 숫자 비교 연산자 정책
 * 
 * 이 시스템에서는 "초과(>)", "미만(<)" 연산자를 지원하지 않습니다.
 * 오직 "이상(>=)", "이하(<=)" 연산자만 사용하세요.
 * 
 * ✅ 사용 가능:
 * - GREATER_THAN_OR_EQUALS (>=) : 값 이상
 * - LESS_THAN_OR_EQUALS (<=)    : 값 이하
 * 
 * ❌ 사용 불가:
 * - GREATER_THAN (>)  : 초과 (존재하지 않음)
 * - LESS_THAN (<)     : 미만 (존재하지 않음)
 * 
 * 예시:
 * - data_volume >= 100  (O)
 * - data_volume > 100   (X) - 컴파일 오류
 */
@Getter
public enum RuleOperator {
    // 기본 비교 연산자
    EQUALS("=", "같음", "기본 비교", "값이 정확히 일치",
            List.of(FieldType.NUMBER, FieldType.STRING, FieldType.BOOLEAN),
            List.of(
                    ParameterDefinition.required("비교값", FieldType.ANY, "비교할 값")
            )),
    NOT_EQUALS("!=", "같지 않음", "기본 비교", "값이 일치하지 않음",
            List.of(FieldType.NUMBER, FieldType.STRING, FieldType.BOOLEAN),
            List.of(
                    ParameterDefinition.required("비교값", FieldType.ANY, "비교할 값")
            )),

    // 숫자 비교 연산자
    GREATER_THAN_OR_EQUALS(">=", "이상", "기본 비교", "값 이상",
            List.of(FieldType.NUMBER),
            List.of(
                    ParameterDefinition.required("임계값", FieldType.NUMBER, "비교할 숫자값")
            )),

    LESS_THAN_OR_EQUALS("<=", "이하", "기본 비교", "값 이하",
            List.of(FieldType.NUMBER),
            List.of(
                    ParameterDefinition.required("임계값", FieldType.NUMBER, "비교할 숫자값")
            )),

    BETWEEN("between", "숫자 범위", "범위", "두 숫자 값 사이에 있음",
            List.of(FieldType.NUMBER),
            List.of(
                    ParameterDefinition.required("최솟값", FieldType.NUMBER, "범위 최솟값"),
                    ParameterDefinition.required("최댓값", FieldType.NUMBER, "범위 최댓값")
            )),

    // 시간 범위 연산자들 - 타입 세이프티를 위해 명확히 구분
    HOUR_RANGE("hour_range", "시간대 범위 (시 단위)", "범위", "특정 시간대 사이에 있는지 확인 (0-23 숫자로 지정)",
            List.of(FieldType.NUMBER),
            List.of(
                    ParameterDefinition.required("시작시", FieldType.NUMBER, "시작 시간 (0-23)"),
                    ParameterDefinition.required("종료시", FieldType.NUMBER, "종료 시간 (0-23)")
            )),

    MINUTE_RANGE("minute_range", "분 범위 (분 단위)", "범위", "특정 분 범위 사이에 있는지 확인 (0-59 숫자로 지정)",
            List.of(FieldType.NUMBER),
            List.of(
                    ParameterDefinition.required("시작분", FieldType.NUMBER, "시작 분 (0-59)"),
                    ParameterDefinition.required("종료분", FieldType.NUMBER, "종료 분 (0-59)")
            )),
    TIME_RANGE("time_range", "시간 범위 (HH:mm)", "범위", "특정 시간 범위 사이에 있는지 확인 (HH:mm 형식으로 정밀하게 지정)",
            List.of(FieldType.TIME),
            List.of(
                    ParameterDefinition.required("시작시간", FieldType.TIME, "시작 시간 (HH:mm 형식)"),
                    ParameterDefinition.required("종료시간", FieldType.TIME, "종료 시간 (HH:mm 형식)")
            )),


    // 문자열 연산자
    CONTAINS("contains", "문자열 포함", "문자열", "문자열을 포함함",
            List.of(FieldType.STRING),
            List.of(
                    ParameterDefinition.required("검색 문자열", FieldType.STRING, "포함 여부를 확인할 문자열")
            )),
    NOT_CONTAINS("not_contains", "문자열 미포함", "문자열", "문자열을 포함하지 않음",
            List.of(FieldType.STRING),
            List.of(
                    ParameterDefinition.required("검색 문자열", FieldType.STRING, "포함되지 않아야 할 문자열")
            )),

    // 포함 관련 연산자
    IN("in", "포함", "포함", "값 목록에 포함됨",
            List.of(FieldType.STRING, FieldType.NUMBER),
            List.of(
                    ParameterDefinition.required("값 목록", FieldType.STRING, "콤마로 구분된 값 목록 (예: value1,value2,value3)")
            )),

    NOT_IN("not_in", "포함 안됨", "포함", "값 목록에 포함되지 않음",
            List.of(FieldType.STRING, FieldType.NUMBER),
            List.of(
                    ParameterDefinition.required("값 목록", FieldType.STRING, "콤마로 구분된 값 목록 (예: value1,value2,value3)")
            )),

    // 존재 여부 연산자 - 파라미터가 필요 없는 특수한 케이스
    EXISTS("exists", "존재함", "존재", "데이터/이력이 존재함",
            List.of(FieldType.NUMBER, FieldType.STRING),
            Collections.emptyList()),  // 파라미터 필요 없음
    NOT_EXISTS("not_exists", "존재하지 않음", "존재", "데이터/이력이 존재하지 않음",
            List.of(FieldType.NUMBER, FieldType.STRING),
            Collections.emptyList()),  // 파라미터 필요 없음
    IS_NULL("is_null", "NULL임", "존재", "값이 NULL임",
            List.of(FieldType.NUMBER, FieldType.STRING, FieldType.BOOLEAN),
            Collections.emptyList()),  // 파라미터 필요 없음
    IS_NOT_NULL("is_not_null", "NULL이 아님", "존재", "값이 NULL이 아님",
            List.of(FieldType.NUMBER, FieldType.STRING, FieldType.BOOLEAN),
            Collections.emptyList()),  // 파라미터 필요 없음

    // Boolean 전용 연산자 - 파라미터가 필요 없는 특수한 케이스
    IS_TRUE("is_true", "참", "Boolean", "값이 참(true)임",
            List.of(FieldType.BOOLEAN),
            Collections.emptyList()),  // 파라미터 필요 없음
    IS_FALSE("is_false", "거짓", "Boolean", "값이 거짓(false)임",
            List.of(FieldType.BOOLEAN),
            Collections.emptyList()),  // 파라미터 필요 없음

    // 시간 관련 연산자
    WITHIN("within", "기간 이내", "시간", "지정된 기간 이내",
            List.of(FieldType.NUMBER),
            List.of(
                    ParameterDefinition.required("기간", FieldType.NUMBER, "기간 (분 단위)")
            )),
    BEFORE("before", "이전", "시간", "지정된 시간 이전",
            List.of(FieldType.TIME),
            List.of(
                    ParameterDefinition.required("기준 시간", FieldType.TIME, "기준 시간 (HH:mm 형식)")
            )),
    AFTER("after", "이후", "시간", "지정된 시간 이후",
            List.of(FieldType.TIME),
            List.of(
                    ParameterDefinition.required("기준 시간", FieldType.TIME, "기준 시간 (HH:mm 형식)")
            )),
    TIME_DIFF("time_diff", "시간 차이", "시간", "두 시간 필드 간의 차이",
            List.of(FieldType.TIME),
            List.of(
                    ParameterDefinition.required("비교 필드", FieldType.STRING, "비교할 다른 시간 필드명"),
                    ParameterDefinition.required("임계값", FieldType.NUMBER, "시간 차이 임계값 (분 단위)")
            )),

    // 집계 연산자
    SUM_WITHIN("sum_within"
            , "시간 내 합계"
            , "집계"
            , "지정 시간 내 합계가 임계값 이상",
            List.of(FieldType.NUMBER),
            List.of(
                    ParameterDefinition.required("시간윈도우", FieldType.NUMBER, "시간 윈도우 (분 단위)"),
                    ParameterDefinition.required("임계값", FieldType.NUMBER, "합계 임계값")
            )),
    COUNT_WITHIN("count_within", "시간 내 횟수", "집계", "지정 시간 내 횟수가 임계값 이상",
            List.of(FieldType.NUMBER),
            List.of(
                    ParameterDefinition.required("시간윈도우", FieldType.NUMBER, "시간 윈도우 (분 단위)"),
                    ParameterDefinition.required("횟수", FieldType.NUMBER, "개수 임계값")
            )),
    DISTINCT_COUNT("distinct_count", "중복 제거 개수", "집계", "지정 시간 내 고유 개수가 임계값 이상",
            List.of(FieldType.NUMBER),
            List.of(
                    ParameterDefinition.required("시간윈도우", FieldType.NUMBER, "시간 윈도우 (분 단위)"),
                    ParameterDefinition.required("개수", FieldType.NUMBER, "고유 개수 임계값")
            )),
    
    // 무이력(분 단위) - 일반화된 NO_ACTIVITY (분 단위 사용)
    NO_ACTIVITY_WITHIN("no_activity_within", "N분 이내 무이력", "이력", "지정된 분 수 이내 이력이 없음",
            List.of(FieldType.NUMBER),
            List.of(
                    ParameterDefinition.required("분", FieldType.NUMBER, "시간 간격 (분 단위)")
            )),
    
    // 97개 샘플 지원을 위한 추가 연산자들
    NO_HISTORY_WITHIN_MONTHS("no_history_within_months", "N개월 이내 이력 없음", "이력", "지정된 개월 수 내 이력이 없음",
            List.of(FieldType.NUMBER),
            List.of(
                    ParameterDefinition.required("개월수", FieldType.NUMBER, "확인할 개월 수")
            )),
    
    WITHIN_MINUTES("within_minutes", "N분 이내", "시간", "지정된 분 수 이내",
            List.of(FieldType.TIME),
            List.of(
                    ParameterDefinition.required("분", FieldType.NUMBER, "시간 간격 (분 단위)")
            )),
    
    WITHIN_HOURS("within_hours", "N시간 이내", "시간", "지정된 시간 수 이내",
            List.of(FieldType.TIME),
            List.of(
                    ParameterDefinition.required("시간", FieldType.NUMBER, "시간 간격 (시간 단위)")
            )),
    
    WITHIN_DAYS("within_days", "N일 이내", "시간", "지정된 일 수 이내",
            List.of(FieldType.TIME),
            List.of(
                    ParameterDefinition.required("일", FieldType.NUMBER, "시간 간격 (일 단위)")
            )),
    
    REPEATED_TIMES("repeated_times", "N회 반복", "빈도", "지정된 횟수만큼 반복",
            List.of(FieldType.NUMBER),
            List.of(
                    ParameterDefinition.required("횟수", FieldType.NUMBER, "반복 횟수")
            )),
    
    PERCENTAGE_OF_BALANCE("percentage_of_balance", "잔액 대비 비율", "금액", "잔액 대비 특정 비율",
            List.of(FieldType.NUMBER),
            List.of(
                    ParameterDefinition.required("비율", FieldType.NUMBER, "잔액 대비 비율 (백분율)")
            )),
    
    SAME_AS_FIELD("same_as_field", "다른 필드와 동일", "비교", "지정된 필드와 값이 동일함",
            List.of(FieldType.STRING, FieldType.NUMBER),
            List.of(
                    ParameterDefinition.required("필드명", FieldType.STRING, "비교할 필드명")
            )),
    
    // 시퀀스 + 시간 제약 (A 후 B가 N분 이내)
    SEQUENCE_WITHIN("sequence_within", "A 후 B (N분 이내)", "순서", "이벤트 A 이후 N분 이내 이벤트 B 발생",
            List.of(FieldType.STRING),
            List.of(
                    ParameterDefinition.required("이전이벤트", FieldType.STRING, "먼저 발생해야 할 이벤트"),
                    ParameterDefinition.required("다음이벤트", FieldType.STRING, "이후 발생할 이벤트"),
                    ParameterDefinition.required("분", FieldType.NUMBER, "시간 간격 (분 단위)")
            )),
    
    SEQUENCE_AFTER("sequence_after", "A 후 B 시도", "순서", "A 이벤트 후 B 이벤트 발생",
            List.of(FieldType.STRING),
            List.of(
                    ParameterDefinition.required("이전이벤트", FieldType.STRING, "먼저 발생해야 할 이벤트"),
                    ParameterDefinition.required("다음이벤트", FieldType.STRING, "이후 발생할 이벤트")
            )),
    
    SIMULTANEOUS_CONDITIONS("simultaneous_conditions", "동시 조건 만족", "순서", "여러 조건이 동시에 만족됨",
            List.of(FieldType.STRING),
            List.of(
                    ParameterDefinition.required("조건목록", FieldType.STRING, "동시에 만족해야 할 조건들")
            )),
    
    // ATMCondition 등에서 사용 중인 특수 연산자들(표준화 정리)
    EXCEEDS_LIMIT("exceeds_limit", "한도 초과", "한도", "잔액/일일 한도 초과 여부",
            List.of(FieldType.STRING),
            List.of(
                    ParameterDefinition.required("한도종류", FieldType.STRING, "BALANCE_LIMIT 또는 DAILY_LIMIT")
            )),
    NO_HISTORY("no_history", "이력 없음", "이력", "해당 맥락의 이력이 없음",
            List.of(FieldType.STRING, FieldType.NUMBER),
            Collections.emptyList()),
    FREQUENCY_WITHIN_TIME("frequency_within_time", "기간 내 횟수", "집계", "지정된 시간 내 횟수가 임계값 이상",
            List.of(FieldType.NUMBER),
            List.of(
                    ParameterDefinition.required("시간", FieldType.NUMBER, "시간 (시간 단위)"),
                    ParameterDefinition.required("횟수", FieldType.NUMBER, "최소 횟수")
            ));

    private final String symbol;
    private final String label;
    private final String category;
    private final String description;
    private final List<FieldType> supportedTypes;
    private final List<ParameterDefinition> parameterDefinitions;

    // 모든 연산자는 파라미터 정의를 가짐 (빈 리스트일 수도 있음)
    RuleOperator(String symbol, String label, String category, String description,
                 List<FieldType> supportedTypes, List<ParameterDefinition> parameterDefinitions) {
        this.symbol = symbol;
        this.label = label;
        this.category = category;
        this.description = description;
        this.supportedTypes = supportedTypes;
        this.parameterDefinitions = parameterDefinitions;
    }

    /**
     * 특정 필드 타입에서 사용 가능한 연산자 목록 조회
     */
    public static List<RuleOperator> getOperatorsForType(FieldType fieldType) {
        return Arrays.stream(values())
                .filter(op -> op.supportsType(fieldType))
                .collect(Collectors.toList());
    }

    /**
     * 해당 연산자가 특정 타입을 지원하는지 확인
     */
    public boolean supportsType(FieldType fieldType) {
        return supportedTypes.contains(fieldType) ||
                supportedTypes.contains(FieldType.ANY);
    }

    /**
     * 지원하는 타입 목록을 문자열 배열로 반환
     */
    public String[] getSupportedTypeStrings() {
        return supportedTypes.stream()
                .map(FieldType::getCode)
                .toArray(String[]::new);
    }

    /**
     * 집계 연산자인지 확인
     */
    public boolean isAggregateOperator() {
        return this == SUM_WITHIN || this == COUNT_WITHIN || this == DISTINCT_COUNT
                || this == FREQUENCY_WITHIN_TIME;
    }

    /**
     * 파라미터가 필요한 연산자인지 확인
     */
    public boolean requiresParameters() {
        return !parameterDefinitions.isEmpty();
    }

    /**
     * 파라미터 형식 반환
     */
    public String getParameterFormat() {
        if (parameterDefinitions.isEmpty()) {
            return null;
        }

        return parameterDefinitions.stream()
                .map(ParameterDefinition::getFormatDescription)
                .collect(Collectors.joining(", "));
    }

    /**
     * Symbol로 RuleOperator 찾기 (예: "<=" -> LESS_THAN_OR_EQUALS)
     * valueOf()와 달리 symbol 값으로 검색
     * 별칭 지원: "EQUALS" -> "=", "NOT_EQUALS" -> "!=" 등
     */
    public static RuleOperator fromSymbol(String symbol) {
        if (symbol == null) {
            throw new IllegalArgumentException("Operator symbol cannot be null");
        }

        // 대소문자 무시 처리 추가
        String lowerSymbol = symbol.toLowerCase();

        // 별칭 매핑 (데이터베이스에 enum 이름으로 저장된 경우 대응)
        String normalizedSymbol = switch (lowerSymbol) {
            case "equals" -> "=";
            case "not_equals" -> "!=";
            case "greater_than_or_equals" -> ">=";
            case "less_than_or_equals" -> "<=";
            default -> lowerSymbol;
        };

        for (RuleOperator op : values()) {
            if (op.symbol.equalsIgnoreCase(normalizedSymbol)) {
                return op;
            }
        }

        throw new IllegalArgumentException("No enum constant with symbol: " + symbol);
    }

    /**
     * 파라미터 유효성 검증
     */
    public boolean validateParameters(String value) {
        if (!requiresParameters()) {
            return true;
        }

        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        // IN, NOT_IN 연산자는 콤마로 구분된 값 목록을 하나의 파라미터로 취급
        if (this == IN || this == NOT_IN) {
            // 최소한 하나의 값은 있어야 함
            String[] values = value.split(",");
            for (String val : values) {
                if (val.trim().isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        String[] params = value.split(",");

        // 파라미터 개수 검증
        if (params.length != parameterDefinitions.size()) {
            return false;
        }

        // 각 파라미터 타입별 검증
        for (int i = 0; i < parameterDefinitions.size(); i++) {
            ParameterDefinition definition = parameterDefinitions.get(i);
            String paramValue = params[i].trim();

            if (!definition.validateValue(paramValue)) {
                return false;
            }
        }

        return true;
    }
}

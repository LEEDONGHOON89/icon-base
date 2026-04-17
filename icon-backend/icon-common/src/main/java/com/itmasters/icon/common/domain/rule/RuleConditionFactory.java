package com.itmasters.icon.common.domain.rule;

import com.itmasters.icon.common.domain.RuleType;
import com.itmasters.icon.common.domain.rule.condition.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * RuleField의 타입 정보를 기반으로 적절한 RuleCondition 구현체를 생성하는 팩토리
 */
public class RuleConditionFactory {
    
    // supports 메서드를 통해 검사할 RuleCondition 구현체들 (우선순위 순)
    @SuppressWarnings("unchecked")
    private static final List<Class<? extends RuleCondition>> CONDITION_CLASSES = List.of(
        BooleanCondition.class,        // IS_TRUE, IS_FALSE 우선 처리
        TimeRangeCondition.class,      // 시간 범위 조건
        TimeRangeWithMinutesCondition.class, // 정밀한 시간 범위
        DurationCondition.class,       // 기간 비교
        TimeDifferenceCondition.class, // 시간 차이 비교
        CountWithinCondition.class,    // 빈도 체크
        SumWithinCondition.class,      // 금액 합계 체크
        DistinctCountCondition.class,  // 고유값 개수 체크
        AmountCondition.class,         // 금액 비교
        ComparisonCondition.class      // 문자열 비교 (가장 범용적이므로 마지막)
    );
    
    // 시간 관련 연산자
    private static final Set<RuleOperator> TIME_DURATION_OPERATORS = Set.of(
        RuleOperator.WITHIN
    );
    
    // 시간 범위 필드
    private static final Set<String> TIME_RANGE_FIELDS = Set.of(
        "hour", "access_hour", "transaction_hour", "transaction_time"
    );
    
    /**
     * RuleType 기반으로 적절한 RuleCondition 생성 (새로운 메서드)
     * supports 패턴을 사용하여 각 구현체의 지원 여부를 확인
     */
    public static RuleCondition createCondition(RuleType ruleType, String fieldName, RuleOperator operator, Object value) {
        // 각 구현체의 supports 메서드를 통해 최적의 구현체 찾기
        for (Class<? extends RuleCondition> clazz : CONDITION_CLASSES) {
            if (callSupportsMethod(clazz, ruleType, fieldName, operator, value)) {
                return createConditionInstance(clazz, ruleType, fieldName, operator, value);
            }
        }
        
        // 적합한 구현체가 없으면 기존 로직으로 폴백
        return createCondition(fieldName, operator, value);
    }
    
    /**
     * 필드명과 연산자, 값을 기반으로 적절한 RuleCondition 생성 (기존 메서드)
     * 하위 호환성을 위해 유지
     */
    public static RuleCondition createCondition(String fieldName, RuleOperator operator, Object value) {
        // Boolean 연산자 처리를 가장 먼저 수행 (value 파라미터 무시)
        if (operator == RuleOperator.IS_TRUE || operator == RuleOperator.IS_FALSE) {
            return BooleanCondition.of(fieldName, operator);
        }

        // 연산자 기반 특수 처리를 가장 먼저 수행
        if (operator == RuleOperator.HOUR_RANGE || 
            operator == RuleOperator.TIME_RANGE || 
            operator == RuleOperator.MINUTE_RANGE) {
            return createTimeRangeCondition(fieldName, operator, value);
        }

        if (isTimeDifferenceCondition(operator)) {
            return createTimeDifferenceCondition(fieldName, value);
        }

        // 집계/이력 연산자 직접 생성 (ruleType 미지정 경로 호환)
        if (operator == RuleOperator.COUNT_WITHIN) {
            // value format: "minutes,count"
            String s = String.valueOf(value);
            String[] parts = s.split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException("COUNT_WITHIN requires 'minutes,count' but was: " + value);
            }
            int minutes = Integer.parseInt(parts[0].trim());
            int count = Integer.parseInt(parts[1].trim());
            return CountWithinCondition.of(fieldName, count, minutes);
        }

        if (operator == RuleOperator.SUM_WITHIN) {
            // value format: "minutes,amount"
            String s = String.valueOf(value);
            String[] parts = s.split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException("SUM_WITHIN requires 'minutes,amount' but was: " + value);
            }
            int minutes = Integer.parseInt(parts[0].trim());
            java.math.BigDecimal amount = new java.math.BigDecimal(parts[1].trim());
            return SumWithinCondition.of(fieldName, amount, minutes);
        }

        // 시퀀스/무이력 전용 처리
        if (operator == RuleOperator.NO_ACTIVITY_WITHIN) {
            int minutes = Integer.parseInt(value.toString().trim());
            return NoActivityWithinCondition.of(fieldName, minutes);
        }

        if (operator == RuleOperator.SEQUENCE_WITHIN) {
            String[] parts = value.toString().split(",");
            if (parts.length != 3) {
                throw new IllegalArgumentException("SEQUENCE_WITHIN requires 3 parameters (prev,next,minutes): " + value);
            }
            String prev = parts[0].trim();
            String next = parts[1].trim();
            int minutes = Integer.parseInt(parts[2].trim());
            return SequenceWithinCondition.of(fieldName, prev, next, minutes);
        }

        // RuleField 조회
        RuleField field = RuleField.fromFieldName(fieldName);
        
        // RuleField가 정의되지 않은 경우 기본 처리
        if (field == null) {
            return createDefaultCondition(fieldName, operator, value);
        }
        
        // 필드 타입에 따른 처리
        switch (field.getFieldType()) {
            case NUMBER:
                return createAmountCondition(fieldName, operator, value);
                
            case TIME:
                return createDateTimeCondition(fieldName, operator, value);
                
            case STRING:
            case BOOLEAN:
            case ANY:
            default:
                return createComparisonCondition(fieldName, operator, value);
        }
    }
    
    /**
     * 필드명과 값의 타입을 분석하여 적절한 RuleCondition 생성 (연산자 없이)
     */
    public static RuleCondition createConditionFromValue(String fieldName, Object value) {
        RuleField field = RuleField.fromFieldName(fieldName);
        
        if (field == null) {
            // 값의 타입을 보고 추론
            if (value instanceof Number || (value instanceof String && isNumeric((String) value))) {
                return AmountCondition.equals(fieldName, new BigDecimal(value.toString()));
            }
            return ComparisonCondition.equals(fieldName, value.toString());
        }
        
        switch (field.getFieldType()) {
            case NUMBER:
                return AmountCondition.equals(fieldName, new BigDecimal(value.toString()));
                
            case TIME:
                // 기본적으로 같은 시간인지 비교
                return ComparisonCondition.equals(fieldName, value.toString());
                
            case STRING:
            case BOOLEAN:
            case ANY:
            default:
                return ComparisonCondition.equals(fieldName, value.toString());
        }
    }
    
    private static RuleCondition createAmountCondition(String fieldName, RuleOperator operator, Object value) {
        // 복합 파라미터를 받는 연산자 처리
        if (operator.isAggregateOperator()) {
            return createAggregateCondition(fieldName, operator, value);
        }
        
        // BETWEEN은 특별 처리 (리스트 또는 콤마 구분 문자열)
        if (operator == RuleOperator.BETWEEN) {
            return createRangeCondition(fieldName, operator, value);
        }
        
        // 단일 값 처리
        BigDecimal amount = parseBigDecimal(value);
        
        switch (operator) {
            case GREATER_THAN_OR_EQUALS:
                return AmountCondition.greaterThanOrEqual(fieldName, amount);
            case LESS_THAN_OR_EQUALS:
                return AmountCondition.lessThanOrEqual(fieldName, amount);
            case EQUALS:
                return AmountCondition.equals(fieldName, amount);
            case NOT_EQUALS:
                return AmountCondition.notEquals(fieldName, amount);
            default:
                return ComparisonCondition.of(fieldName, operator, value.toString());
        }
    }
    
    private static RuleCondition createDateTimeCondition(String fieldName, RuleOperator operator, Object value) {
        if (TIME_DURATION_OPERATORS.contains(operator)) {
            // Duration 형식의 값 (예: "24h", "180d")
            return createDurationCondition(fieldName, operator, value.toString());
        }
        
        // 일반 날짜 비교
        return createComparisonCondition(fieldName, operator, value);
    }
    
    private static RuleCondition createDurationCondition(String fieldName, RuleOperator operator, String duration) {
        switch (operator) {
            case WITHIN:
                return DurationCondition.within(fieldName, duration);
            default:
                throw new IllegalArgumentException("Unsupported operator for duration: " + operator);
        }
    }
    
    private static RuleCondition createTimeRangeCondition(String fieldName, RuleOperator operator, Object value) {
        String valueString = value.toString();
        String[] parts = valueString.split(",");
        
        if (parts.length != 2) {
            throw new IllegalArgumentException("시간 범위는 두 개의 값이 필요합니다: " + value);
        }
        
        String firstValue = parts[0].trim();
        String secondValue = parts[1].trim();
        
        // HOUR_RANGE: 숫자 형식 (0-23) - 타입 세이프하게 연산자로 구분
        if (operator == RuleOperator.HOUR_RANGE) {
            // 숫자 형식이어야 함
            if (!isNumeric(firstValue) || !isNumeric(secondValue)) {
                throw new IllegalArgumentException("HOUR_RANGE는 숫자 형식이어야 합니다. 예: 10,11");
            }
            
            int startHour = Integer.parseInt(firstValue);
            int endHour = Integer.parseInt(secondValue);
            
            // 유효성 검증
            if (startHour < 0 || startHour > 23 || endHour < 0 || endHour > 23) {
                throw new IllegalArgumentException("시간은 0-23 사이여야 합니다. 입력값: " + value);
            }
            
            return TimeRangeCondition.between(fieldName, startHour, endHour);
        }
        
        // TIME_RANGE: HH:mm 형식 - 타입 세이프하게 연산자로 구분
        if (operator == RuleOperator.TIME_RANGE) {
            // HH:mm 형식이어야 함
            if (!firstValue.contains(":") || !secondValue.contains(":")) {
                throw new IllegalArgumentException("TIME_RANGE는 HH:mm 형식이어야 합니다. 예: 10:30,11:45");
            }
            
            // 시간과 분을 모두 포함한 정밀한 비교
            return TimeRangeWithMinutesCondition.between(fieldName, firstValue, secondValue);
        }
        
        // MINUTE_RANGE: 분 단위 (0-59) - 타입 세이프하게 연산자로 구분
        if (operator == RuleOperator.MINUTE_RANGE) {
            // 숫자 형식이어야 함
            if (!isNumeric(firstValue) || !isNumeric(secondValue)) {
                throw new IllegalArgumentException("MINUTE_RANGE는 숫자 형식이어야 합니다. 예: 15,45");
            }
            
            int startMinute = Integer.parseInt(firstValue);
            int endMinute = Integer.parseInt(secondValue);
            
            // 유효성 검증
            if (startMinute < 0 || startMinute > 59 || endMinute < 0 || endMinute > 59) {
                throw new IllegalArgumentException("분은 0-59 사이여야 합니다. 입력값: " + value);
            }
            
            // 분 범위는 별도 처리가 필요할 수 있음 (현재는 TimeRangeCondition 재사용)
            // TODO: MinuteRangeCondition 구현 고려
            return TimeRangeCondition.between(fieldName, startMinute, endMinute);
        }
        
        // TIME_BETWEEN (deprecated) - 하위 호환성을 위해 유지
//        if (operator == RuleOperator.TIME_BETWEEN) {
//            // 자동 감지 (하위 호환성)
//            if (isNumeric(firstValue) && isNumeric(secondValue)) {
//                // 숫자 형식
//                int startHour = Integer.parseInt(firstValue);
//                int endHour = Integer.parseInt(secondValue);
//
//                if (startHour < 0 || startHour > 23 || endHour < 0 || endHour > 23) {
//                    throw new IllegalArgumentException("시간은 0-23 사이여야 합니다. 입력값: " + value);
//                }
//
//                return TimeRangeCondition.between(fieldName, startHour, endHour);
//            } else if (firstValue.contains(":") && secondValue.contains(":")) {
//                // HH:mm 형식
//                int startHour = parseHourFromTime(firstValue);
//                int endHour = parseHourFromTime(secondValue);
//                return TimeRangeCondition.between(fieldName, startHour, endHour);
//            }
//        }
        
        throw new IllegalArgumentException("지원하지 않는 시간 범위 연산자입니다: " + operator);
    }
    
    private static RuleCondition createTimeDifferenceCondition(String fieldName, Object value) {
        // fieldName이 "field1,field2" 형식일 것으로 예상
        String[] fields = fieldName.split(",");
        if (fields.length != 2) {
            throw new IllegalArgumentException("Time difference requires two field names separated by comma");
        }
        
        Duration duration = parseDuration(value.toString());
        return TimeDifferenceCondition.within(fields[0].trim(), fields[1].trim(), duration);
    }
    
    private static RuleCondition createComparisonCondition(String fieldName, RuleOperator operator, Object value) {
        // IS_NULL, IS_NOT_NULL은 value가 필요 없음
        if (operator == RuleOperator.IS_NULL) {
            return ComparisonCondition.isNull(fieldName);
        }
        if (operator == RuleOperator.IS_NOT_NULL) {
            return ComparisonCondition.isNotNull(fieldName);
        }
        
        switch (operator) {
            case EQUALS:
                return ComparisonCondition.equals(fieldName, value.toString());
            case NOT_EQUALS:
                return ComparisonCondition.notEquals(fieldName, value.toString());
            case IN:
                if (value instanceof List<?> list) {
                    return ComparisonCondition.in(fieldName, list.stream().map(Object::toString).toList());
                }
                throw new IllegalArgumentException("IN operator requires a list value");
            case NOT_IN:
                if (value instanceof List<?> list) {
                    return ComparisonCondition.notIn(fieldName, list.stream().map(Object::toString).toList());
                }
                throw new IllegalArgumentException("NOT_IN operator requires a list value");
            case CONTAINS:
                return ComparisonCondition.contains(fieldName, value.toString());
            case NOT_CONTAINS:
                return ComparisonCondition.notContains(fieldName, value.toString());
            default:
                return ComparisonCondition.of(fieldName, operator, value.toString());
        }
    }
    
    private static RuleCondition createDefaultCondition(String fieldName, RuleOperator operator, Object value) {
        // 값의 타입을 분석하여 조건 생성
        if (value instanceof Number || (value instanceof String && isNumeric((String) value))) {
            return createAmountCondition(fieldName, operator, value);
        }
        
        return createComparisonCondition(fieldName, operator, value);
    }
    
    private static boolean isTimeRangeCondition(String fieldName, RuleOperator operator) {
        return TIME_RANGE_FIELDS.contains(fieldName) && 
               (operator == RuleOperator.HOUR_RANGE ||
                operator == RuleOperator.TIME_RANGE ||
                operator == RuleOperator.MINUTE_RANGE);
    }
    
    private static boolean isTimeDifferenceCondition(RuleOperator operator) {
        return operator == RuleOperator.WITHIN;
    }
    
    private static BigDecimal parseBigDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        if (value instanceof String) {
            return new BigDecimal((String) value);
        }
        throw new IllegalArgumentException("Cannot convert to BigDecimal: " + value);
    }
    
    private static int parseInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        throw new IllegalArgumentException("Cannot convert to Integer: " + value);
    }
    
    private static Duration parseDuration(String value) {
        // "24h", "180d", "30m", "1w" 형식 파싱
        if (value.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(value.substring(0, value.length() - 1)));
        }
        if (value.endsWith("d")) {
            return Duration.ofDays(Long.parseLong(value.substring(0, value.length() - 1)));
        }
        if (value.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(value.substring(0, value.length() - 1)));
        }
        if (value.endsWith("w")) {
            return Duration.ofDays(Long.parseLong(value.substring(0, value.length() - 1)) * 7);
        }
        throw new IllegalArgumentException("Invalid duration format: " + value + ". Expected format: <number>[m|h|d|w]");
    }
    
    private static boolean isNumeric(String str) {
        try {
            new BigDecimal(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * HH:mm 형식의 시간 문자열에서 시간(hour)만 추출
     * @param timeValue 시간 값 (HH:mm 형식 또는 시간 숫자)
     * @return 시간 (0-23)
     */
    private static int parseHourFromTime(Object timeValue) {
        String timeStr = String.valueOf(timeValue).trim();
        
        // HH:mm 형식인 경우
        if (timeStr.contains(":")) {
            String[] parts = timeStr.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid time format. Expected HH:mm but got: " + timeStr);
            }
            
            try {
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);
                
                // 유효성 검증
                if (hour < 0 || hour > 23) {
                    throw new IllegalArgumentException("Hour must be between 0 and 23: " + hour);
                }
                if (minute < 0 || minute > 59) {
                    throw new IllegalArgumentException("Minute must be between 0 and 59: " + minute);
                }
                
                return hour;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid time format. Expected HH:mm but got: " + timeStr);
            }
        }
        
        // HH:mm 형식이 아닌 경우 에러
        throw new IllegalArgumentException("Invalid time format. Expected HH:mm but got: " + timeStr);
    }
    
    /**
     * 집계 연산자(COUNT_WITHIN, SUM_WITHIN 등)를 위한 조건 생성
     * @param fieldName 필드명
     * @param operator 연산자
     * @param value 값 (콤마로 구분된 문자열 또는 리스트)
     * @return RuleCondition
     */
    private static RuleCondition createAggregateCondition(String fieldName, RuleOperator operator, Object value) {
        List<String> params = parseParameters(value);
        
        switch (operator) {
            case COUNT_WITHIN:
                // COUNT_WITHIN은 "시간윈도우(분),횟수" 형식
                if (params.size() != 2) {
                    throw new IllegalArgumentException(
                        "COUNT_WITHIN requires 2 parameters (timeWindow,count) but got: " + value
                    );
                }
                int timeWindow = Integer.parseInt(params.get(0).trim());
                int count = Integer.parseInt(params.get(1).trim());
                return CountWithinCondition.of(fieldName, count, timeWindow);
                
            case SUM_WITHIN:
                // SUM_WITHIN은 "시간윈도우(분),합계금액" 형식
                if (params.size() != 2) {
                    throw new IllegalArgumentException(
                        "SUM_WITHIN requires 2 parameters (timeWindow,amount) but got: " + value
                    );
                }
                int sumTimeWindow = Integer.parseInt(params.get(0).trim());
                BigDecimal sumAmount = parseBigDecimal(params.get(1).trim());
                return SumWithinCondition.of(fieldName, sumAmount, sumTimeWindow);
                
            case DISTINCT_COUNT:
                // DISTINCT_COUNT는 "시간윈도우(분),고유개수" 형식
                if (params.size() != 2) {
                    throw new IllegalArgumentException(
                        "DISTINCT_COUNT requires 2 parameters (timeWindow,count) but got: " + value
                    );
                }
                int distinctTimeWindow = Integer.parseInt(params.get(0).trim());
                int distinctCount = Integer.parseInt(params.get(1).trim());
                return DistinctCountCondition.of(fieldName, distinctCount, distinctTimeWindow);
                
            default:
                throw new IllegalArgumentException("Unsupported aggregate operator: " + operator);
        }
    }
    
    /**
     * 범위 연산자(BETWEEN)를 위한 조건 생성
     * @param fieldName 필드명
     * @param operator 연산자
     * @param value 값 (리스트 또는 콤마로 구분된 문자열)
     * @return RuleCondition
     */
    private static RuleCondition createRangeCondition(String fieldName, RuleOperator operator, Object value) {
        List<String> params = parseParameters(value);
        
        if (params.size() < 2) {
            throw new IllegalArgumentException(
                "BETWEEN requires at least 2 parameters (min,max) but got: " + value
            );
        }
        
        BigDecimal min = parseBigDecimal(params.get(0).trim());
        BigDecimal max = parseBigDecimal(params.get(1).trim());
        
        return AmountCondition.between(fieldName, min, max);
    }
    
    /**
     * 파라미터 파싱 - 리스트 또는 콤마 구분 문자열을 리스트로 변환
     * @param value 입력값
     * @return 파라미터 리스트
     */
    private static List<String> parseParameters(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        
        if (value instanceof String str) {
            return Arrays.asList(str.split(","));
        }
        
        // 단일 값인 경우
        return List.of(value.toString());
    }
    
    /**
     * 리플렉션을 통해 각 구현체의 supports 메서드 호출
     */
    private static boolean callSupportsMethod(Class<? extends RuleCondition> clazz, 
                                            RuleType ruleType, String fieldName, 
                                            RuleOperator operator, Object value) {
        try {
            Method supportsMethod = clazz.getMethod("supports", RuleType.class, String.class, RuleOperator.class, Object.class);
            return (Boolean) supportsMethod.invoke(null, ruleType, fieldName, operator, value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // supports 메서드가 없거나 호출 실패 시 false 반환
            return false;
        }
    }
    
    /**
     * 구현체별 인스턴스 생성
     */
    private static RuleCondition createConditionInstance(Class<? extends RuleCondition> clazz, 
                                                       RuleType ruleType, String fieldName, 
                                                       RuleOperator operator, Object value) {
        try {
            // 각 구현체별로 최적의 생성 방법 사용
            if (clazz == BooleanCondition.class) {
                return BooleanCondition.of(fieldName, operator);
            }
            
            if (clazz == ComparisonCondition.class) {
                return createComparisonCondition(fieldName, operator, value);
            }
            
            if (clazz == AmountCondition.class) {
                return createAmountCondition(fieldName, operator, value);
            }
            
            if (clazz == TimeRangeCondition.class) {
                return createTimeRangeCondition(fieldName, RuleOperator.BETWEEN, value);
            }
            
            if (clazz == TimeRangeWithMinutesCondition.class) {
                return createTimeRangeCondition(fieldName, RuleOperator.TIME_RANGE, value);
            }
            
            if (clazz == DurationCondition.class) {
                if (operator == RuleOperator.LESS_THAN_OR_EQUALS) {
                    return DurationCondition.within(fieldName, value.toString());
                } else {
                    return DurationCondition.before(fieldName, value.toString());
                }
            }
            
            if (clazz == TimeDifferenceCondition.class) {
                // value는 "compareField,duration" 형식
                String[] parts = value.toString().split(",");
                if (parts.length == 2) {
                    Duration duration = parseDuration(parts[1].trim());
                    return TimeDifferenceCondition.within(fieldName, parts[0].trim(), duration);
                }
            }
            
            if (clazz == CountWithinCondition.class) {
                String[] parts = value.toString().split(",");
                if (parts.length == 2) {
                    int timeWindow = Integer.parseInt(parts[0].trim());
                    int count = Integer.parseInt(parts[1].trim());
                    return CountWithinCondition.of(fieldName, count, timeWindow);
                }
            }
            
            if (clazz == SumWithinCondition.class) {
                String[] parts = value.toString().split(",");
                if (parts.length == 2) {
                    int timeWindow = Integer.parseInt(parts[0].trim());
                    BigDecimal amount = new BigDecimal(parts[1].trim());
                    return SumWithinCondition.of(fieldName, amount, timeWindow);
                }
            }
            
            if (clazz == DistinctCountCondition.class) {
                String[] parts = value.toString().split(",");
                if (parts.length == 2) {
                    int timeWindow = Integer.parseInt(parts[0].trim());
                    int count = Integer.parseInt(parts[1].trim());
                    return DistinctCountCondition.of(fieldName, count, timeWindow);
                }
            }
            
            // 기본 생성 방법으로 폴백
            return createComparisonCondition(fieldName, operator, value);
            
        } catch (Exception e) {
            // 생성 실패 시 ComparisonCondition으로 폴백
            return createComparisonCondition(fieldName, operator, value);
        }
    }
}

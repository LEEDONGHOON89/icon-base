package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.rule.RuleOperator;
import com.itmasters.icon.common.domain.RuleType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;

/**
 * 시간:분 단위의 정밀한 시간 범위 조건
 * 예: transaction_time BETWEEN 10:30 AND 11:45
 */
@Getter
public class TimeRangeWithMinutesCondition extends RuleCondition {

    private final RuleOperator operator;
    private final LocalTime startTime;
    private final LocalTime endTime;

    public TimeRangeWithMinutesCondition(String fieldName, LocalTime startTime, LocalTime endTime) {
        super(fieldName);
        this.operator = RuleOperator.TIME_RANGE;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public boolean evaluate(Map<String, Object> context) {
        Object value = context.get(fieldName);
        if (value == null) {
            return false;
        }
        
        try {
            LocalTime time = extractTime(value);
            // 같은 날 범위인 경우 (예: 10:30 ~ 11:45)
            if (startTime.isBefore(endTime) || startTime.equals(endTime)) {
                return !time.isBefore(startTime) && !time.isAfter(endTime);
            }
            // 자정을 넘는 범위인 경우 (예: 22:00 ~ 02:00)
            else {
                return !time.isBefore(startTime) || !time.isAfter(endTime);
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String toExpression() {
        return String.format("%s BETWEEN %s AND %s", fieldName, startTime, endTime);
    }
    
    @Override
    public boolean isValid() {
        return fieldName != null && !fieldName.isEmpty()
            && startTime != null
            && endTime != null;
    }
    
    private LocalTime extractTime(Object value) {
        if (value instanceof LocalTime) {
            return (LocalTime) value;
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toLocalTime();
        }
        if (value instanceof String) {
            return LocalTime.parse((String) value);
        }
        throw new IllegalArgumentException("Cannot convert to LocalTime: " + value);
    }
    
    // 정적 팩토리 메서드
    public static TimeRangeWithMinutesCondition between(String fieldName, LocalTime startTime, LocalTime endTime) {
        return new TimeRangeWithMinutesCondition(fieldName, startTime, endTime);
    }
    
    public static TimeRangeWithMinutesCondition between(String fieldName, String startTime, String endTime) {
        return new TimeRangeWithMinutesCondition(
            fieldName, 
            LocalTime.parse(startTime), 
            LocalTime.parse(endTime)
        );
    }


    public String getStartTimeString() {
        return startTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public String getEndTimeString() {
        return endTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
    
    @Override
    public Object getValue() {
        return getStartTimeString() + "," + getEndTimeString();
    }

    @Override
    public <T> T accept(RuleConditionVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    /**
     * TimeRangeWithMinutesCondition이 지원하는 조건들
     * - RuleType: TIME_BASED (정밀한 시간대 기반 룰)
     * - 지원 연산자: TIME_RANGE (시간:분 단위 범위 체크)
     */
    private static final Set<RuleType> SUPPORTED_RULE_TYPES = Set.of(
        RuleType.TIME_BASED
    );
    
    private static final Set<RuleOperator> SUPPORTED_OPERATORS = Set.of(
        RuleOperator.TIME_RANGE
    );
    
    /**
     * 지원 조건 확인
     * @param ruleType 룰 타입
     * @param fieldName 필드명 (시간 필드에 적합)
     * @param operator 연산자 (TIME_RANGE만 지원)
     * @param value 값 (시간:분 범위를 표현하는 문자열)
     * @return 지원 가능하면 true
     */
    public static boolean supports(RuleType ruleType, String fieldName, RuleOperator operator, Object value) {
        // 1. 지원하는 RuleType인지 확인
        if (!SUPPORTED_RULE_TYPES.contains(ruleType)) {
            return false;
        }
        
        // 2. 지원하는 연산자인지 확인
        if (!SUPPORTED_OPERATORS.contains(operator)) {
            return false;
        }
        
        // 3. 값이 시간:분 범위를 표현할 수 있는지 확인
        if (value == null) {
            return false;
        }
        
        // 4. 값이 HH:mm,HH:mm 형식으로 파싱 가능한지 확인
        try {
            if (value instanceof String) {
                String strValue = (String) value;
                // "10:30,11:45" 형식 확인
                if (strValue.contains(",")) {
                    String[] parts = strValue.split(",");
                    if (parts.length == 2) {
                        LocalTime.parse(parts[0].trim());
                        LocalTime.parse(parts[1].trim());
                        return true;
                    }
                }
            }
            return false;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
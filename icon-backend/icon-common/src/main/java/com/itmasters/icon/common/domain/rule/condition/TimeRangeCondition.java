package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.rule.RuleOperator;
import com.itmasters.icon.common.domain.RuleType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;

/**
 * 시간 범위 조건
 * 예: hour BETWEEN 0 AND 6
 */
@Getter
public class TimeRangeCondition extends RuleCondition {

    private final RuleOperator operator;
    private final int startHour;
    private final int endHour;
    

    public TimeRangeCondition(String fieldName, int startHour, int endHour) {
        super(fieldName);
        this.operator = RuleOperator.BETWEEN;
        this.startHour = startHour;
        this.endHour = endHour;
    }

    @Override
    public boolean evaluate(Map<String, Object> context) {
        Object value = context.get(fieldName);
        if (value == null) {
            return false;
        }
        
        try {
            int hour = extractHour(value);
            return hour >= startHour && hour <= endHour;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String toExpression() {
        return String.format("%s BETWEEN %d AND %d", fieldName, startHour, endHour);
    }
    
    @Override
    public boolean isValid() {
        return fieldName != null && !fieldName.isEmpty()
            && startHour >= 0 && startHour <= 23
            && endHour >= 0 && endHour <= 23
            && startHour <= endHour;
    }
    
    private int extractHour(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).getHour();
        }
        if (value instanceof LocalTime) {
            return ((LocalTime) value).getHour();
        }
        return Integer.parseInt(String.valueOf(value));
    }
    
    // 정적 팩토리 메서드
    public static TimeRangeCondition between(String fieldName, int startHour, int endHour) {
        return new TimeRangeCondition(fieldName, startHour, endHour);
    }

    public static TimeRangeCondition nightTime(String fieldName) {
        return new TimeRangeCondition(fieldName, 0, 6);
    }
    
    @Override
    public Object getValue() {
        return startHour + "," + endHour;
    }

    @Override
    public <T> T accept(RuleConditionVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    /**
     * TimeRangeCondition이 지원하는 조건들
     * - RuleType: TIME_BASED (시간대 기반 룰)
     * - 지원 연산자: BETWEEN (시간 범위 체크)
     */
    private static final Set<RuleType> SUPPORTED_RULE_TYPES = Set.of(
        RuleType.TIME_BASED
    );
    
    private static final Set<RuleOperator> SUPPORTED_OPERATORS = Set.of(
        RuleOperator.BETWEEN
    );
    
    /**
     * 지원 조건 확인
     * @param ruleType 룰 타입
     * @param fieldName 필드명 (시간 필드에 적합)
     * @param operator 연산자 (BETWEEN만 지원)
     * @param value 값 (시간 범위를 표현하는 문자열 또는 배열)
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
        
        // 3. 값이 시간 범위를 표현할 수 있는지 확인
        if (value == null) {
            return false;
        }
        
        // 4. 값이 시간 범위로 파싱 가능한지 확인
        try {
            if (value instanceof String) {
                String strValue = (String) value;
                // "10,11" 또는 "10-11" 형식 확인
                if (strValue.contains(",") || strValue.contains("-")) {
                    String[] parts = strValue.split("[,-]");
                    if (parts.length == 2) {
                        int start = Integer.parseInt(parts[0].trim());
                        int end = Integer.parseInt(parts[1].trim());
                        return start >= 0 && start <= 23 && end >= 0 && end <= 23;
                    }
                }
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
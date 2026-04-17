package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.rule.RuleOperator;
import com.itmasters.icon.common.domain.RuleType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * 두 시간 필드 간 차이 비교 조건
 * 예: time_diff < 1h (login_time과 transaction_time 차이)
 */
public class TimeDifferenceCondition extends RuleCondition {
    
    private final String compareFieldName;
    private final RuleOperator operator;
    private final Duration threshold;
    
    // Getter 추가
    public String getCompareFieldName() {
        return compareFieldName;
    }
    
    public RuleOperator getOperator() {
        return operator;
    }
    
    public Duration getThreshold() {
        return threshold;
    }
    
    public TimeDifferenceCondition(String fieldName, String compareFieldName, 
                                  RuleOperator operator, Duration threshold) {
        super(fieldName);
        this.compareFieldName = compareFieldName;
        this.operator = operator;
        this.threshold = threshold;
    }
    
    @Override
    public boolean evaluate(Map<String, Object> context) {
        Object value1 = context.get(fieldName);
        Object value2 = context.get(compareFieldName);
        
        if (value1 == null || value2 == null) {
            return false;
        }
        
        try {
            LocalDateTime time1 = parseDateTime(value1);
            LocalDateTime time2 = parseDateTime(value2);
            
            Duration diff = Duration.between(time1, time2).abs();
            
            return switch (operator) {
                case LESS_THAN_OR_EQUALS -> diff.compareTo(threshold) <= 0;
                case GREATER_THAN_OR_EQUALS -> diff.compareTo(threshold) >= 0;
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String toExpression() {
        return String.format("time_diff(%s, %s) %s %s", 
            fieldName, compareFieldName, getOperatorSymbol(), formatDuration());
    }
    
    @Override
    public boolean isValid() {
        return fieldName != null && !fieldName.isEmpty()
            && compareFieldName != null && !compareFieldName.isEmpty()
            && operator != null
            && threshold != null;
    }
    
    private LocalDateTime parseDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        return LocalDateTime.parse(String.valueOf(value));
    }
    
    private String getOperatorSymbol() {
        return switch (operator) {
            case LESS_THAN_OR_EQUALS -> "<=";
            case GREATER_THAN_OR_EQUALS -> ">=";
            default -> operator.name();
        };
    }
    
    private String formatDuration() {
        if (threshold.toHours() > 0) {
            return threshold.toHours() + "h";
        } else if (threshold.toMinutes() > 0) {
            return threshold.toMinutes() + "m";
        } else {
            return threshold.getSeconds() + "s";
        }
    }
    
    // 정적 팩토리 메서드
    public static TimeDifferenceCondition within(String field1, String field2, Duration duration) {
        return new TimeDifferenceCondition(field1, field2, RuleOperator.LESS_THAN_OR_EQUALS, duration);
    }
    
    @Override
    public Object getValue() {
        // TimeDifferenceCondition은 두 필드 간의 차이를 비교하므로 특별한 형식 필요
        return compareFieldName + "," + formatDuration();
    }

    @Override
    public <T> T accept(RuleConditionVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    /**
     * TimeDifferenceCondition이 지원하는 조건들
     * - RuleType: CERT_TIMING (인증 타이밍 체크 룰)
     * - 지원 연산자: LESS_THAN_OR_EQUALS, GREATER_THAN_OR_EQUALS
     */
    private static final Set<RuleType> SUPPORTED_RULE_TYPES = Set.of(
        RuleType.CERT_TIMING  // OTP 발급 후 180분 이내 같은 인증 타이밍 체크
    );
    
    private static final Set<RuleOperator> SUPPORTED_OPERATORS = Set.of(
        RuleOperator.LESS_THAN_OR_EQUALS,
        RuleOperator.GREATER_THAN_OR_EQUALS
    );
    
    /**
     * 지원 조건 확인
     * @param ruleType 룰 타입
     * @param fieldName 첫 번째 시간 필드명
     * @param operator 연산자
     * @param value 값 (두 번째 필드명,기간 형식 예: "otp_time,180m")
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
        
        // 3. 값이 두 번째 필드명과 기간을 포함하는지 확인
        if (value == null) {
            return false;
        }
        
        // 4. 값이 "field,duration" 형식으로 파싱 가능한지 확인
        try {
            if (value instanceof String) {
                String strValue = (String) value;
                // "otp_time,180m" 형식 확인
                if (strValue.contains(",")) {
                    String[] parts = strValue.split(",");
                    if (parts.length == 2) {
                        String field = parts[0].trim();
                        String duration = parts[1].trim();
                        // 필드명이 비어있지 않고, 기간이 올바른 형식인지 확인
                        return !field.isEmpty() && duration.matches("\\d+[hdms]");
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
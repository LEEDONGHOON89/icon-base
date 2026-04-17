package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.rule.RuleOperator;
import com.itmasters.icon.common.domain.RuleType;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 기간 비교 조건
 * 예: device_first_seen < 24h, last_transaction > 180d
 */
@Getter
public class DurationCondition extends RuleCondition {
    
    private final RuleOperator operator;
    private final Duration duration;
    private final String durationExpression;
    

    public DurationCondition(String fieldName, RuleOperator operator, String durationExpression) {
        super(fieldName);
        this.operator = operator;
        this.durationExpression = durationExpression;
        this.duration = parseDuration(durationExpression);
    }
    
    @Override
    public boolean evaluate(Map<String, Object> context) {
        Object value = context.get(fieldName);
        if (value == null) {
            return false;
        }
        
        try {
            LocalDateTime dateTime = parseDateTime(value);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime threshold = now.minus(duration);
            
            // device_first_seen < 24h => 24시간 이내 => dateTime이 threshold 이후
            // last_transaction > 180d => 180일 이전 => dateTime이 threshold 이전
            return switch (operator) {
                case LESS_THAN_OR_EQUALS -> !dateTime.isBefore(threshold);
                case GREATER_THAN_OR_EQUALS -> !dateTime.isAfter(threshold);
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String toExpression() {
        return String.format("%s %s %s", fieldName, getOperatorSymbol(), durationExpression);
    }
    
    @Override
    public boolean isValid() {
        return fieldName != null && !fieldName.isEmpty() 
            && operator != null 
            && duration != null;
    }
    
    private Duration parseDuration(String expression) {
        Pattern pattern = Pattern.compile("(\\d+)([hdmw])");
        Matcher matcher = pattern.matcher(expression.toLowerCase());
        
        if (matcher.matches()) {
            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            
            return switch (unit) {
                case "m" -> Duration.ofMinutes(amount);
                case "h" -> Duration.ofHours(amount);
                case "d" -> Duration.ofDays(amount);
                case "w" -> Duration.ofDays(amount * 7);
                default -> throw new IllegalArgumentException("Unsupported time unit: " + unit);
            };
        }
        throw new IllegalArgumentException("Invalid duration format: " + expression + ". Expected format: <number>[m|h|d|w]");
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
    
    // 정적 팩토리 메서드
    public static DurationCondition within(String fieldName, String duration) {
        return new DurationCondition(fieldName, RuleOperator.LESS_THAN_OR_EQUALS, duration);
    }
    
    public static DurationCondition before(String fieldName, String duration) {
        return new DurationCondition(fieldName, RuleOperator.GREATER_THAN_OR_EQUALS, duration);
    }
    
    public static DurationCondition after(String fieldName, String duration) {
        return new DurationCondition(fieldName, RuleOperator.LESS_THAN_OR_EQUALS, duration);
    }
    
    @Override
    public Object getValue() {
        return durationExpression;
    }

    @Override
    public <T> T accept(RuleConditionVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    /**
     * DurationCondition이 지원하는 조건들
     * - RuleType: DORMANT_ACCOUNT, ACCOUNT_AGE, DEVICE_HISTORY (기간 기반 룰 타입들)
     * - 지원 연산자: LESS_THAN_OR_EQUALS, GREATER_THAN_OR_EQUALS
     */
    private static final Set<RuleType> SUPPORTED_RULE_TYPES = Set.of(
        RuleType.DORMANT_ACCOUNT,  // 휴면 계좌 (12개월간 거래 없음)
        RuleType.ACCOUNT_AGE,      // 계좌 나이 (계좌 개설 후 X일)
        RuleType.DEVICE_HISTORY    // 기기 이력 (12개월간 없던 기기)
    );
    
    private static final Set<RuleOperator> SUPPORTED_OPERATORS = Set.of(
        RuleOperator.LESS_THAN_OR_EQUALS,
        RuleOperator.GREATER_THAN_OR_EQUALS
    );
    
    /**
     * 지원 조건 확인
     * @param ruleType 룰 타입
     * @param fieldName 필드명 (날짜/시간 필드에 적합)
     * @param operator 연산자
     * @param value 값 (기간 표현식, 예: "24h", "180d")
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
        
        // 3. 값이 기간 표현식인지 확인
        if (value == null) {
            return false;
        }
        
        // 4. 값이 기간 형식으로 파싱 가능한지 확인
        try {
            if (value instanceof String) {
                String strValue = (String) value;
                // "24h", "180d", "30m", "1w" 형식 확인
                Pattern pattern = Pattern.compile("\\d+[hdmw]");
                return pattern.matcher(strValue.toLowerCase()).matches();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
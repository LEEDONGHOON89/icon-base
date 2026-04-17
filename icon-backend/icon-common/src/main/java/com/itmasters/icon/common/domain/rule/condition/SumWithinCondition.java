package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.rule.RuleOperator;
import com.itmasters.icon.common.domain.RuleType;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * 지정된 시간 내 합계 금액 조건
 * 예: 10분 내 거래 합계가 1,000,000원 이상
 */
@Getter
public class SumWithinCondition extends RuleCondition {
    private final BigDecimal amount;
    private final int timeWindowMinutes;
    
    private SumWithinCondition(String fieldName, BigDecimal amount, int timeWindowMinutes) {
        super(fieldName);
        this.amount = amount;
        this.timeWindowMinutes = timeWindowMinutes;
    }
    
    public static SumWithinCondition of(String fieldName, BigDecimal amount, int timeWindowMinutes) {
        return new SumWithinCondition(fieldName, amount, timeWindowMinutes);
    }
    
    @Override
    public boolean evaluate(Map<String, Object> context) {
        // 실제 구현은 이벤트 스트림 처리 시스템에서 수행
        // 여기서는 조건의 구조만 정의
        Object value = context.get(fieldName + "_sum_within_" + timeWindowMinutes);
        if (value instanceof Number) {
            BigDecimal sum = new BigDecimal(value.toString());
            return sum.compareTo(amount) >= 0;
        }
        return false;
    }
    
    @Override
    public RuleOperator getOperator() {
        return RuleOperator.SUM_WITHIN;
    }

    @Override
    public boolean requiresHistory() { return true; }

    @Override
    public boolean matchesWithHistory(Map<String, Object> context, java.util.List<?> history) {
        if (history == null || history.isEmpty()) return false;

        java.math.BigDecimal sum = java.math.BigDecimal.ZERO;
        for (Object item : history) {
            Map<String, Object> ev = extractEventData(item);
            if (ev == null) continue;
            Object val = resolveAmount(ev, fieldName);
            if (val == null) continue;
            try {
                java.math.BigDecimal amt = new java.math.BigDecimal(val.toString());
                sum = sum.add(amt);
            } catch (NumberFormatException ignore) { }
        }
        return sum.compareTo(amount) >= 0;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractEventData(Object item) {
        try {
            if (item instanceof Map<?,?> map) return (Map<String, Object>) map;
            java.lang.reflect.Method m = item.getClass().getMethod("getEventData");
            Object data = m.invoke(item);
            if (data instanceof Map<?,?> map) return (Map<String, Object>) map;
        } catch (Exception ignore) { }
        return null;
    }

    private Object resolveAmount(Map<String, Object> ev, String field) {
        if (ev.containsKey(field)) return ev.get(field);
        if ("transaction_amount".equals(field)) {
            Object v = ev.get("transaction_amount");
            if (v == null) v = ev.get("TRX_AMT");
            return v;
        }
        return ev.get(field);
    }
    
    @Override
    public String toExpression() {
        return String.format("%s SUM_WITHIN %d minutes >= %s", fieldName, timeWindowMinutes, amount);
    }
    
    @Override
    public boolean isValid() {
        return fieldName != null && !fieldName.isEmpty() 
            && amount != null && amount.compareTo(BigDecimal.ZERO) > 0
            && timeWindowMinutes > 0;
    }
    
    @Override
    public Object getValue() {
        return timeWindowMinutes + "," + amount;
    }

    @Override
    public <T> T accept(RuleConditionVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    /**
     * SumWithinCondition이 지원하는 조건들
     * - RuleType: VELOCITY (금액 합계 빈도 체크 룰)
     * - 지원 연산자: SUM_WITHIN (시간 윈도우 내 금액 합계 체크)
     */
    private static final Set<RuleType> SUPPORTED_RULE_TYPES = Set.of(
        RuleType.VELOCITY
    );
    
    private static final Set<RuleOperator> SUPPORTED_OPERATORS = Set.of(
        RuleOperator.SUM_WITHIN
    );
    
    /**
     * 지원 조건 확인
     * @param ruleType 룰 타입
     * @param fieldName 필드명 (금액 필드)
     * @param operator 연산자 (SUM_WITHIN만 지원)
     * @param value 값 (시간윈도우,금액 형식)
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
        
        // 3. 값이 시간윈도우,금액 형식인지 확인
        if (value == null) {
            return false;
        }
        
        // 4. 값이 "timeWindow,amount" 형식으로 파싱 가능한지 확인
        try {
            if (value instanceof String) {
                String strValue = (String) value;
                // "30,1000000" 형식 확인 (30분 내 1,000,000원)
                if (strValue.contains(",")) {
                    String[] parts = strValue.split(",");
                    if (parts.length == 2) {
                        int timeWindow = Integer.parseInt(parts[0].trim());
                        new BigDecimal(parts[1].trim()); // 금액 파싱 검증
                        return timeWindow > 0;
                    }
                }
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

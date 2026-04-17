package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.rule.RuleOperator;
import com.itmasters.icon.common.domain.RuleType;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * 지정된 시간 내 발생 횟수 조건
 * 예: 10분 내 5회 이상 발생
 */
@Getter
public class CountWithinCondition extends RuleCondition {
    private final int count;
    private final int timeWindowMinutes;
    
    private CountWithinCondition(String fieldName, int count, int timeWindowMinutes) {
        super(fieldName);
        this.count = count;
        this.timeWindowMinutes = timeWindowMinutes;
    }
    
    public static CountWithinCondition of(String fieldName, int count, int timeWindowMinutes) {
        return new CountWithinCondition(fieldName, count, timeWindowMinutes);
    }
    
    @Override
    public boolean evaluate(Map<String, Object> context) {
        // 실제 구현은 이벤트 스트림 처리 시스템에서 수행
        // 여기서는 조건의 구조만 정의
        Object value = context.get(fieldName + "_count_within_" + timeWindowMinutes);
        if (value instanceof Number) {
            return ((Number) value).intValue() >= count;
        }
        return false;
    }
    
    @Override
    public RuleOperator getOperator() {
        return RuleOperator.COUNT_WITHIN;
    }

    @Override
    public boolean requiresHistory() {
        return true;
    }

    @Override
    public boolean matchesWithHistory(Map<String, Object> context, java.util.List<?> history) {
        if (history == null || history.isEmpty()) {
            return false;
        }

        // 기준값: 현재 컨텍스트의 동일 필드 값
        Object targetValue = context.get(fieldName);

        int matched = 0;
        for (Object item : history) {
            Map<String, Object> event = extractEventData(item);
            if (event == null) continue;

            Object eventFieldValue = resolveFieldValue(event, fieldName);
            if (targetValue == null) {
                // null 비교: 이벤트에도 해당 필드가 없거나 null이면 동일로 간주하지 않음
                continue;
            }
            if (targetValue.toString().equals(String.valueOf(eventFieldValue))) {
                matched++;
                if (matched >= count) return true;
            }
        }
        return matched >= count;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractEventData(Object item) {
        try {
            if (item instanceof Map<?,?> map) {
                return (Map<String, Object>) map;
            }
            // EngineEventStreamEntity#getEventData() 리플렉션 접근
            java.lang.reflect.Method m = item.getClass().getMethod("getEventData");
            Object data = m.invoke(item);
            if (data instanceof Map<?,?> map) {
                return (Map<String, Object>) map;
            }
        } catch (Exception ignore) { }
        return null;
    }

    private Object resolveFieldValue(Map<String, Object> event, String field) {
        if (event.containsKey(field)) return event.get(field);
        // 표준화 이전 필드 보정
        if ("transaction_type".equals(field)) {
            Object v = event.get("event_type");
            if (v == null) v = event.get("TRX_TYPE");
            return v;
        }
        if ("to_account".equals(field)) {
            Object v = event.get("to_account");
            if (v == null) v = event.get("RECEIVER");
            return v;
        }
        if ("from_account".equals(field)) {
            Object v = event.get("from_account");
            if (v == null) v = event.get("SENDER");
            return v;
        }
        return event.get(field);
    }
    
    @Override
    public String toExpression() {
        return String.format("%s COUNT_WITHIN %d minutes >= %d", fieldName, timeWindowMinutes, count);
    }
    
    @Override
    public boolean isValid() {
        return fieldName != null && !fieldName.isEmpty() 
            && count > 0 
            && timeWindowMinutes > 0;
    }
    
    @Override
    public Object getValue() {
        return timeWindowMinutes + "," + count;
    }

    @Override
    public <T> T accept(RuleConditionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    
    
    /**
     * CountWithinCondition이 지원하는 조건들
     * - RuleType: 빈도/횟수 체크가 필요한 룰들
     * - 지원 연산자: COUNT_WITHIN (시간 윈도우 내 횟수 체크)
     */
    private static final Set<RuleType> SUPPORTED_RULE_TYPES = Set.of(
        RuleType.VELOCITY,
        RuleType.LOGIN_FAILURE,         // 로그인 실패 횟수 체크
        RuleType.ATM_TRANSACTION,       // ATM 거래 횟수 체크
        RuleType.IP_SECURITY            // 동일 IP 접속 횟수 체크
    );
    
    private static final Set<RuleOperator> SUPPORTED_OPERATORS = Set.of(
        RuleOperator.COUNT_WITHIN
    );
    
    /**
     * 지원 조건 확인
     * @param ruleType 룰 타입
     * @param fieldName 필드명 (이벤트 필드)
     * @param operator 연산자 (COUNT_WITHIN만 지원)
     * @param value 값 (시간윈도우,횟수 형식)
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
        
        // 3. 값이 시간윈도우,횟수 형식인지 확인
        if (value == null) {
            return false;
        }
        
        // 4. 값이 "timeWindow,count" 형식으로 파싱 가능한지 확인
        try {
            if (value instanceof String) {
                String strValue = (String) value;
                // "30,5" 형식 확인 (30분 내 5회)
                if (strValue.contains(",")) {
                    String[] parts = strValue.split(",");
                    if (parts.length == 2) {
                        int timeWindow = Integer.parseInt(parts[0].trim());
                        int count = Integer.parseInt(parts[1].trim());
                        return timeWindow > 0 && count > 0;
                    }
                }
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

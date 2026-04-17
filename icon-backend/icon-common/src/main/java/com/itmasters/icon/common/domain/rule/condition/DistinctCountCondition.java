package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.rule.RuleOperator;
import com.itmasters.icon.common.domain.RuleType;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * 지정된 시간 내 고유 값 개수 조건
 * 예: 10분 내 서로 다른 IP 주소가 5개 이상
 */
@Getter
public class DistinctCountCondition extends RuleCondition {
    private final int count;
    private final int timeWindowMinutes;
    
    private DistinctCountCondition(String fieldName, int count, int timeWindowMinutes) {
        super(fieldName);
        this.count = count;
        this.timeWindowMinutes = timeWindowMinutes;
    }
    
    public static DistinctCountCondition of(String fieldName, int count, int timeWindowMinutes) {
        return new DistinctCountCondition(fieldName, count, timeWindowMinutes);
    }
    
    @Override
    public boolean evaluate(Map<String, Object> context) {
        // 실제 구현은 이벤트 스트림 처리 시스템에서 수행
        // 여기서는 조건의 구조만 정의
        Object value = context.get(fieldName + "_distinct_count_within_" + timeWindowMinutes);
        if (value instanceof Number) {
            return ((Number) value).intValue() >= count;
        }
        return false;
    }
    
    @Override
    public RuleOperator getOperator() {
        return RuleOperator.DISTINCT_COUNT;
    }
    
    @Override
    public String toExpression() {
        return String.format("%s DISTINCT_COUNT_WITHIN %d minutes >= %d", fieldName, timeWindowMinutes, count);
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
     * DistinctCountCondition이 지원하는 조건들
     * - RuleType: VELOCITY (고유값 개수 빈도 체크 룰)
     * - 지원 연산자: DISTINCT_COUNT (시간 윈도우 내 고유값 개수 체크)
     */
    private static final Set<RuleType> SUPPORTED_RULE_TYPES = Set.of(
        RuleType.VELOCITY
    );
    
    private static final Set<RuleOperator> SUPPORTED_OPERATORS = Set.of(
        RuleOperator.DISTINCT_COUNT
    );
    
    /**
     * 지원 조건 확인
     * @param ruleType 룰 타입
     * @param fieldName 필드명 (고유값을 세는 필드)
     * @param operator 연산자 (DISTINCT_COUNT만 지원)
     * @param value 값 (시간윈도우,개수 형식)
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
        
        // 3. 값이 시간윈도우,개수 형식인지 확인
        if (value == null) {
            return false;
        }
        
        // 4. 값이 "timeWindow,count" 형식으로 파싱 가능한지 확인
        try {
            if (value instanceof String) {
                String strValue = (String) value;
                // "30,5" 형식 확인 (30분 내 서로 다른 값 5개)
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
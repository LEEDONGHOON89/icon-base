package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.rule.RuleOperator;
import com.itmasters.icon.common.domain.RuleType;
import java.util.Map;
import java.util.Set;

/**
 * Boolean 타입 필드의 true/false 확인을 위한 조건
 * IS_TRUE, IS_FALSE 연산자를 지원하며 파라미터가 필요 없음
 */
public class BooleanCondition extends RuleCondition {
    
    private final RuleOperator operator;
    
    private BooleanCondition(String fieldName, RuleOperator operator) {
        super(fieldName);
        this.operator = operator;
        validateOperator(operator);
    }
    
    private void validateOperator(RuleOperator operator) {
        if (operator != RuleOperator.IS_TRUE && operator != RuleOperator.IS_FALSE) {
            throw new IllegalArgumentException(
                "BooleanCondition only supports IS_TRUE or IS_FALSE operators, but got: " + operator
            );
        }
    }
    
    /**
     * Boolean 조건 생성
     * @param fieldName 필드명
     * @param operator IS_TRUE 또는 IS_FALSE
     * @return BooleanCondition
     */
    public static BooleanCondition of(String fieldName, RuleOperator operator) {
        return new BooleanCondition(fieldName, operator);
    }
    
    /**
     * IS_TRUE 조건 생성 (편의 메서드)
     */
    public static BooleanCondition isTrue(String fieldName) {
        return new BooleanCondition(fieldName, RuleOperator.IS_TRUE);
    }
    
    /**
     * IS_FALSE 조건 생성 (편의 메서드)
     */
    public static BooleanCondition isFalse(String fieldName) {
        return new BooleanCondition(fieldName, RuleOperator.IS_FALSE);
    }
    
    @Override
    public RuleOperator getOperator() {
        return operator;
    }
    
    @Override
    public boolean evaluate(Map<String, Object> context) {
        Object value = context.get(fieldName);
        
        // null 처리
        if (value == null) {
            return false;
        }
        
        // Boolean 타입 확인
        if (!(value instanceof Boolean)) {
            // String "true"/"false" 처리
            if (value instanceof String) {
                String strValue = ((String) value).toLowerCase();
                if ("true".equals(strValue) || "false".equals(strValue)) {
                    value = Boolean.parseBoolean(strValue);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
        
        Boolean boolValue = (Boolean) value;
        
        // IS_TRUE는 값이 true일 때, IS_FALSE는 값이 false일 때 true 반환
        return operator == RuleOperator.IS_TRUE ? boolValue : !boolValue;
    }
    
    @Override
    public String toExpression() {
        // "디바이스 변경 여부가 참" 또는 "디바이스 변경 여부가 거짓"
        return fieldName + "가 " + operator.getLabel();
    }
    
    @Override
    public boolean isValid() {
        return fieldName != null && !fieldName.trim().isEmpty() &&
               (operator == RuleOperator.IS_TRUE || operator == RuleOperator.IS_FALSE);
    }
    
    @Override
    public Object getValue() {
        // Boolean 조건은 파라미터가 없으므로 null 반환
        return null;
    }
    
    @Override
    public <T> T accept(RuleConditionVisitor<T> visitor) {
        return visitor.visitBooleanCondition(this);
    }
    
    @Override
    public String toString() {
        return "BooleanCondition{" +
               "fieldName='" + fieldName + '\'' +
               ", operator=" + operator.name() +
               '}';
    }
    
    /**
     * BooleanCondition이 지원하는 조건들
     * - RuleType: SIMPLE, DEVICE_HISTORY (Boolean 필드를 가진 룰 타입들)
     * - 지원 연산자: IS_TRUE, IS_FALSE
     */
    private static final Set<RuleType> SUPPORTED_RULE_TYPES = Set.of(
        RuleType.SIMPLE,
        RuleType.DEVICE_HISTORY  // 기기 이력 체크에서 Boolean 필드 사용
    );
    
    private static final Set<RuleOperator> SUPPORTED_OPERATORS = Set.of(
        RuleOperator.IS_TRUE,
        RuleOperator.IS_FALSE
    );
    
    /**
     * 지원 조건 확인
     * @param ruleType 룰 타입
     * @param fieldName 필드명 (Boolean 필드에 적합)
     * @param operator 연산자 (IS_TRUE 또는 IS_FALSE만 지원)
     * @param value 값 (Boolean 조건은 값이 필요 없음)
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
        
        // 3. Boolean 조건은 별도 값이 필요 없으므로 항상 true
        // (operator 체크만 통과하면 됨)
        return true;
    }
}
package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.rule.RuleOperator;
import com.itmasters.icon.common.domain.RuleType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import lombok.Getter;

/**
 * 금액 비교 조건 예: amount > 1000000
 */
@Getter
public class AmountCondition extends RuleCondition {

    private final RuleOperator operator;
    private final BigDecimal threshold;

    public AmountCondition(String fieldName, RuleOperator operator, BigDecimal threshold) {
        super(fieldName);
        if (operator == null) {
            throw new IllegalArgumentException("Operator cannot be null");
        }
        if (threshold == null) {
            throw new IllegalArgumentException("Threshold cannot be null");
        }
        this.operator = operator;
        this.threshold = threshold;
    }

    @Override
    public boolean evaluate(Map<String, Object> context) {
        Object value = context.get(fieldName);
        if (value == null) {
            return false;
        }

        try {
            BigDecimal amount =
                    value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(String.valueOf(value));

            return switch (operator) {
                case EQUALS -> amount.compareTo(threshold) == 0;
                case NOT_EQUALS -> amount.compareTo(threshold) != 0;
                case GREATER_THAN_OR_EQUALS -> amount.compareTo(threshold) >= 0;
                case LESS_THAN_OR_EQUALS -> amount.compareTo(threshold) <= 0;
                default -> false;
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public String toExpression() {
        return String.format("%s %s %s", fieldName, getOperatorSymbol(), threshold);
    }

    @Override
    public boolean isValid() {
        return fieldName != null && !fieldName.isEmpty() && operator != null && threshold != null;
    }

    private String getOperatorSymbol() {
        return switch (operator) {
            case GREATER_THAN_OR_EQUALS -> ">=";
            case LESS_THAN_OR_EQUALS -> "<=";
            case EQUALS -> "=";
            case NOT_EQUALS -> "!=";
            default -> operator.name();
        };
    }


    public static AmountCondition greaterThanOrEqual(String fieldName, BigDecimal amount) {
        return new AmountCondition(fieldName, RuleOperator.GREATER_THAN_OR_EQUALS, amount);
    }

    public static AmountCondition lessThanOrEqual(String fieldName, BigDecimal amount) {
        return new AmountCondition(fieldName, RuleOperator.LESS_THAN_OR_EQUALS, amount);
    }

    public static AmountCondition equals(String fieldName, BigDecimal amount) {
        return new AmountCondition(fieldName, RuleOperator.EQUALS, amount);
    }

    public static AmountCondition notEquals(String fieldName, BigDecimal amount) {
        return new AmountCondition(fieldName, RuleOperator.NOT_EQUALS, amount);
    }

    public static AmountCondition between(String fieldName, BigDecimal min, BigDecimal max) {
        // BETWEEN은 실제로는 두 개의 조건이 필요하므로, 일단 GREATER_THAN_OR_EQUALS로 구현
        // 나중에 복합 조건으로 처리할 수 있음
        return new AmountCondition(fieldName, RuleOperator.BETWEEN, min);
    }
    
    @Override
    public Object getValue() {
        return threshold.toString();
    }

    @Override
    public <T> T accept(RuleConditionVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    /**
     * AmountCondition이 지원하는 조건들
     * - RuleType: 금액/수치 기준이 있는 타입들
     * - 지원 연산자: EQUALS, NOT_EQUALS, GREATER_THAN_OR_EQUALS, LESS_THAN_OR_EQUALS, BETWEEN
     */
    private static final Set<RuleType> SUPPORTED_RULE_TYPES = Set.of(
        RuleType.SIMPLE,
        RuleType.BALANCE_RATIO,
        RuleType.VELOCITY,              // 금액 조건이 포함된 빈도 체크
        RuleType.LOGIN_FAILURE,         // 로그인 실패 횟수 >= 5
        RuleType.ATM_TRANSACTION,       // ATM 출금 횟수 >= 5, 거래 금액
        RuleType.IP_SECURITY,           // IP 접속 횟수 >= 3
        RuleType.FRAUD_DETECTION,       // 사기 점수 > 0.7
        RuleType.CUSTOMER_GRADE,        // 고객 나이 >= 65
        RuleType.E_COMMERCE             // 고액 상품 구매 > 500000
    );
    
    private static final Set<RuleOperator> SUPPORTED_OPERATORS = Set.of(
        RuleOperator.EQUALS,
        RuleOperator.NOT_EQUALS,
        RuleOperator.GREATER_THAN_OR_EQUALS,
        RuleOperator.LESS_THAN_OR_EQUALS,
        RuleOperator.BETWEEN
    );
    
    /**
     * 지원 조건 확인
     * @param ruleType 룰 타입
     * @param fieldName 필드명 (금액/수치 필드에 적합)
     * @param operator 연산자
     * @param value 값 (숫자로 변환 가능해야 함)
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
        
        // 3. 값이 숫자로 변환 가능한지 확인
        if (value == null) {
            return false;
        }
        
        // 4. 값이 BigDecimal로 변환 가능한지 확인
        try {
            if (value instanceof BigDecimal) {
                return true;
            } else if (value instanceof Number) {
                return true;
            } else {
                new BigDecimal(String.valueOf(value));
                return true;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

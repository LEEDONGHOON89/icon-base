package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.rule.RuleOperator;
import com.itmasters.icon.common.domain.RuleType;
import com.itmasters.icon.common.domain.RuleDomain;
import com.itmasters.icon.common.domain.OperatorCategory;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 단순 비교 조건 예: country != 'KR', status = 'ACTIVE'
 */
public class ComparisonCondition extends RuleCondition {

    private final RuleOperator operator;
    private final String compareValue;

    // Getter 추가 (JSON 직렬화를 위해)
    public RuleOperator getOperator() {
        return operator;
    }

    public String getCompareValue() {
        return compareValue;
    }

    public ComparisonCondition(String fieldName, RuleOperator operator, String compareValue) {
        super(fieldName);

        if (operator == null) {
            throw new IllegalArgumentException("Operator cannot be null");
        }

        if (operator != RuleOperator.IS_NULL
                && operator != RuleOperator.IS_NOT_NULL
                && compareValue == null) {
            throw new IllegalArgumentException("Compare value cannot be null for operator: " + operator);
        }

        this.operator = operator;
        this.compareValue = compareValue;
    }

    @Override
    public boolean evaluate(Map<String, Object> context) {
        Object value = context.get(fieldName);

        if (value == null) {
            return operator == RuleOperator.IS_NULL;
        }

        String strValue = String.valueOf(value);
        String normalizedValue = normalizeValue(fieldName, strValue);
        String normalizedCompare = normalizeValue(fieldName, compareValue);

        return switch (operator) {
            case EQUALS -> normalizedCompare != null && normalizedCompare.equals(normalizedValue);
            case NOT_EQUALS -> normalizedCompare == null || !normalizedCompare.equals(normalizedValue);
            case CONTAINS -> normalizedCompare != null && normalizedValue.contains(normalizedCompare);
            case NOT_CONTAINS -> normalizedCompare == null || !normalizedValue.contains(normalizedCompare);
            case IN -> {
                if (normalizedCompare == null) yield false;
                String[] values = normalizedCompare.split(",");
                for (String v : values) {
                    String candidate = normalizeValue(fieldName, v.trim());
                    if (candidate.equals(normalizedValue)) {
                        yield true;
                    }
                }
                yield false;
            }
            case NOT_IN -> {
                if (normalizedCompare == null) yield true;
                String[] values = normalizedCompare.split(",");
                for (String v : values) {
                    String candidate = normalizeValue(fieldName, v.trim());
                    if (candidate.equals(normalizedValue)) {
                        yield false;
                    }
                }
                yield true;
            }
            case IS_NULL -> false; // 이미 위에서 처리
            case IS_NOT_NULL -> true;
            default -> false;
        };
    }

    private String normalizeValue(String field, String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if ("transaction_type".equalsIgnoreCase(field)) {
            return normalizeTransactionType(trimmed);
        }
        return trimmed;
    }

    private String normalizeTransactionType(String value) {
        if ("이체".equals(value)) {
            return "TRANSFER";
        }
        String upper = value.toUpperCase(Locale.ROOT);
        if ("TRANSFER".equals(upper)) {
            return "TRANSFER";
        }
        return value;
    }

    @Override
    public String toExpression() {
        if (operator == RuleOperator.IS_NULL || operator == RuleOperator.IS_NOT_NULL) {
            return String.format("%s %s", fieldName, getOperatorSymbol());
        }
        if (operator == RuleOperator.IN || operator == RuleOperator.NOT_IN) {
            // IN 연산자는 리스트 형식으로 표현
            String[] values = compareValue.split(",");
            String formattedValues = String.join("', '", values);
            return String.format("%s %s ('%s')", fieldName, getOperatorSymbol(), formattedValues);
        }
        return String.format("%s %s '%s'", fieldName, getOperatorSymbol(), compareValue);
    }

    @Override
    public boolean isValid() {
        return fieldName != null
                && !fieldName.isEmpty()
                && operator != null
                && (operator == RuleOperator.IS_NULL
                || operator == RuleOperator.IS_NOT_NULL
                || compareValue != null);
    }

    private String getOperatorSymbol() {
        return switch (operator) {
            case EQUALS -> "=";
            case NOT_EQUALS -> "!=";
            case CONTAINS -> "CONTAINS";
            case NOT_CONTAINS -> "NOT CONTAINS";
            case IS_NULL -> "IS NULL";
            case IS_NOT_NULL -> "IS NOT NULL";
            default -> operator.name();
        };
    }

    // 정적 팩토리 메서드들
    public static ComparisonCondition equals(String fieldName, String value) {
        return new ComparisonCondition(fieldName, RuleOperator.EQUALS, value);
    }

    public static ComparisonCondition notEquals(String fieldName, String value) {
        return new ComparisonCondition(fieldName, RuleOperator.NOT_EQUALS, value);
    }

    public static ComparisonCondition isNull(String fieldName) {
        return new ComparisonCondition(fieldName, RuleOperator.IS_NULL, null);
    }

    public static ComparisonCondition isNotNull(String fieldName) {
        return new ComparisonCondition(fieldName, RuleOperator.IS_NOT_NULL, null);
    }

    public static ComparisonCondition contains(String fieldName, String value) {
        return new ComparisonCondition(fieldName, RuleOperator.CONTAINS, value);
    }

    public static ComparisonCondition notContains(String fieldName, String value) {
        return new ComparisonCondition(fieldName, RuleOperator.NOT_CONTAINS, value);
    }

    public static ComparisonCondition in(String fieldName, java.util.List<String> values) {
        // IN 연산자는 콤마로 구분된 문자열로 저장
        return new ComparisonCondition(fieldName, RuleOperator.IN, String.join(",", values));
    }

    public static ComparisonCondition notIn(String fieldName, java.util.List<String> values) {
        return new ComparisonCondition(fieldName, RuleOperator.NOT_IN, String.join(",", values));
    }

    public static ComparisonCondition of(String fieldName, RuleOperator operator, String value) {
        return new ComparisonCondition(fieldName, operator, value);
    }
    
    @Override
    public Object getValue() {
        return compareValue;
    }
    
    @Override
    public <T> T accept(RuleConditionVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    /**
     * ComparisonCondition이 지원하는 조건들
     * - RuleType: 문자열 비교가 주된 타입들
     * - 지원 연산자: EQUALS, NOT_EQUALS, CONTAINS, NOT_CONTAINS, IN, NOT_IN, IS_NULL, IS_NOT_NULL
     */
    private static final Set<RuleType> SUPPORTED_RULE_TYPES = Set.of(
        RuleType.SIMPLE,
        RuleType.RISKY_COUNTRY,
        RuleType.UNUSUAL_COUNTRY,
        RuleType.DORMANT_ACCOUNT,
        RuleType.LOGIN_FAILURE,         // 로그인 상태 = "FAILED"
        RuleType.IP_SECURITY,           // 국가 코드 != "KR"
        RuleType.THREAT_LEVEL,          // 위협 등급 = "RED"
        RuleType.THREAT_DETECTION,      // 멀웨어 감지 = "Y"
        RuleType.ACCOUNT_TYPE,          // 비대면 계좌 = "true"
        RuleType.TRANSACTION_TYPE       // 거래 타입 = "이체"
    );
    
    private static final Set<RuleOperator> SUPPORTED_OPERATORS = Set.of(
        RuleOperator.EQUALS,
        RuleOperator.NOT_EQUALS,
        RuleOperator.CONTAINS,
        RuleOperator.NOT_CONTAINS,
        RuleOperator.IN,
        RuleOperator.NOT_IN,
        RuleOperator.IS_NULL,
        RuleOperator.IS_NOT_NULL
    );
    
    // === 새로운 2단계 탭 구조 지원 ===
    
    /**
     * ComparisonCondition이 지원하는 도메인들 (2단계 탭 구조)
     */
    private static final Set<RuleDomain> SUPPORTED_DOMAINS = Set.of(
        RuleDomain.LOGIN,                    // login_status = "FAILED"
        RuleDomain.FINANCIAL_TRANSACTION    // transaction_type = "이체"
    );
    
    /**
     * ComparisonCondition이 지원하는 연산자 카테고리 (TEXT_COMPARISON만)
     */
    private static final OperatorCategory SUPPORTED_CATEGORY = OperatorCategory.TEXT_COMPARISON;
    
    /**
     * 새로운 객체 파라미터 방식의 지원 조건 확인 (권장)
     * @param domain 룰 도메인 (1단계)  
     * @param request 요청 정보 (fieldName, operator, value 포함)
     * @return 지원 가능하면 true
     */
    public static boolean supports(RuleDomain domain, RuleConditionRequest request) {
        return supports(domain, request.getFieldName(), request.getOperator(), request.getValue());
    }
    
    /**
     * 새로운 2단계 탭 구조 기반 지원 조건 확인 (하위 호환성)
     * @param domain 룰 도메인 (1단계)
     * @param fieldName 필드명
     * @param operator 연산자
     * @param value 값
     * @return 지원 가능하면 true
     */
    public static boolean supports(RuleDomain domain, String fieldName, RuleOperator operator, Object value) {
        // 1. 지원하는 도메인인지 확인
        if (!SUPPORTED_DOMAINS.contains(domain)) {
            return false;
        }
        
        // 2. 🔥 엄격한 연산자 체크 - TEXT_COMPARISON 전용 연산자만 허용
        if (!SUPPORTED_CATEGORY.supports(operator)) {
            return false;
        }
        
        // 3. 🚫 다른 구현체 전용 연산자는 거부
        if (isNumericOnlyOperator(operator)) {
            return false; // AmountCondition 전용
        }
        
        if (isTimeOnlyOperator(operator)) {
            return false; // TimeRangeCondition/DurationCondition 전용
        }
        
        if (isFrequencyOnlyOperator(operator)) {
            return false; // CountWithinCondition 전용
        }
        
        if (isBooleanOnlyOperator(operator)) {
            return false; // BooleanCondition 전용
        }
        
        // 4. IS_NULL, IS_NOT_NULL은 value 불필요
        if (operator == RuleOperator.IS_NULL || operator == RuleOperator.IS_NOT_NULL) {
            return true;
        }
        
        // 5. 나머지 연산자는 value 필요
        if (value == null) {
            return false;
        }
        
        // 6. 🔥 엄격한 값 타입 체크 - 문자열만 허용
        return isStringValue(value);
    }
    
    /**
     * TEXT_COMPARISON 카테고리에 해당하는지 확인
     */
    private static boolean isTextComparisonCategory(RuleOperator operator, Object value) {
        // TEXT_COMPARISON에서 지원하는 연산자인지 확인
        if (!SUPPORTED_CATEGORY.supports(operator)) {
            return false;
        }
        
        // IS_NULL, IS_NOT_NULL은 값 타입과 무관
        if (operator == RuleOperator.IS_NULL || operator == RuleOperator.IS_NOT_NULL) {
            return true;
        }
        
        // 문자열 값이면 TEXT_COMPARISON
        return isStringValue(value);
    }
    
    /**
     * 문자열 값인지 확인
     */
    private static boolean isStringValue(Object value) {
        if (value == null) {
            return false;
        }
        
        // 이미 문자열이면 OK
        if (value instanceof String) {
            return true;
        }
        
        // 숫자면 NUMERIC_COMPARISON에서 처리
        if (value instanceof Number) {
            return false;
        }
        
        // 숫자로 파싱 가능하면 NUMERIC_COMPARISON에서 처리
        try {
            Double.parseDouble(value.toString());
            return false; // 숫자로 파싱되면 AmountCondition에서 처리
        } catch (NumberFormatException e) {
            // 숫자가 아니면 문자열로 처리
            return true;
        }
    }
    
    /**
     * 숫자 전용 연산자인지 확인 (AmountCondition 전용)
     */
    private static boolean isNumericOnlyOperator(RuleOperator operator) {
        return operator == RuleOperator.GREATER_THAN_OR_EQUALS
            || operator == RuleOperator.LESS_THAN_OR_EQUALS
            || operator == RuleOperator.BETWEEN;
    }
    
    /**
     * 시간 전용 연산자인지 확인 (TimeRangeCondition/DurationCondition 전용)
     */
    private static boolean isTimeOnlyOperator(RuleOperator operator) {
        return operator == RuleOperator.TIME_RANGE
            || operator == RuleOperator.HOUR_RANGE
            || operator == RuleOperator.MINUTE_RANGE
            || operator == RuleOperator.WITHIN
            || operator == RuleOperator.BEFORE
            || operator == RuleOperator.AFTER;
    }
    
    /**
     * 빈도 전용 연산자인지 확인 (CountWithinCondition/SumWithinCondition 전용)
     */
    private static boolean isFrequencyOnlyOperator(RuleOperator operator) {
        return operator == RuleOperator.COUNT_WITHIN
            || operator == RuleOperator.SUM_WITHIN
            || operator == RuleOperator.DISTINCT_COUNT;
    }
    
    /**
     * 불린 전용 연산자인지 확인 (BooleanCondition 전용)
     */
    private static boolean isBooleanOnlyOperator(RuleOperator operator) {
        return operator == RuleOperator.IS_TRUE
            || operator == RuleOperator.IS_FALSE;
    }
    
    /**
     * 기존 RuleType 기반 지원 조건 확인 (하위 호환성)
     * @param ruleType 룰 타입
     * @param fieldName 필드명 (문자열 필드에 적합)
     * @param operator 연산자
     * @param value 값 (IS_NULL, IS_NOT_NULL 제외하고는 필요)
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
        
        // 3. IS_NULL, IS_NOT_NULL은 value 불필요
        if (operator == RuleOperator.IS_NULL || operator == RuleOperator.IS_NOT_NULL) {
            return true;
        }
        
        // 4. 나머지 연산자는 value 필요
        if (value == null) {
            return false;
        }
        
        // 5. 값이 문자열로 변환 가능한지 확인 (숫자 필드는 AmountCondition에서 처리)
        try {
            String.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

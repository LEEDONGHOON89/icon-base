package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.rule.RuleOperator;

/**
 * RuleCondition 검증 요청 객체
 * supports 메서드에서 사용되는 파라미터들을 객체로 묶어서 관리
 */
public class RuleConditionRequest {
    
    private final String fieldName;
    private final RuleOperator operator;
    private final Object value;
    
    // 생성자
    public RuleConditionRequest(String fieldName, RuleOperator operator, Object value) {
        this.fieldName = fieldName;
        this.operator = operator;
        this.value = value;
    }
    
    // 정적 팩토리 메서드
    public static RuleConditionRequest of(String fieldName, RuleOperator operator, Object value) {
        return new RuleConditionRequest(fieldName, operator, value);
    }
    
    // Getter 메서드들
    public String getFieldName() {
        return fieldName;
    }
    
    public RuleOperator getOperator() {
        return operator;
    }
    
    public Object getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return String.format("RuleConditionRequest{fieldName='%s', operator=%s, value=%s}", 
                           fieldName, operator, value);
    }
}
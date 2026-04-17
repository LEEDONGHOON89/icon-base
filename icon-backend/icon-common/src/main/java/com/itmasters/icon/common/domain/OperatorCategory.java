package com.itmasters.icon.common.domain;

import com.itmasters.icon.common.domain.rule.RuleOperator;
import com.itmasters.icon.common.domain.rule.condition.RuleCondition;
import com.itmasters.icon.common.domain.rule.condition.*;

import java.util.List;

/**
 * 연산자 카테고리 정의 (2단계 분류)
 * 구현체와 직접 매핑되는 입력 방식별 분류
 */
public enum OperatorCategory {
    /**
     * 숫자 비교 (금액, 횟수, 점수 등)
     * 예: login_failure_count >= 5, atm_amount >= 1000000
     */
    NUMERIC_COMPARISON("숫자 비교", "금액, 횟수, 점수 등의 숫자 값 비교",
                       List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.LESS_THAN_OR_EQUALS,
                              RuleOperator.BETWEEN),
                       AmountCondition.class,
                       "number"),
    
    /**
     * 텍스트/상태 비교 (문자열, 상태값 등)  
     * 예: login_status = "FAILED", access_country != "KR"
     */
    TEXT_COMPARISON("텍스트 비교", "문자열, 상태값, 국가코드 등의 텍스트 비교",
                    List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS,
                           RuleOperator.CONTAINS, RuleOperator.NOT_CONTAINS,
                           RuleOperator.IN, RuleOperator.NOT_IN,
                           RuleOperator.IS_NULL, RuleOperator.IS_NOT_NULL),
                    ComparisonCondition.class,
                    "text"),
    
    /**
     * 시간 범위 (특정 시간대)
     * 예: transaction_time BETWEEN "10:00,23:00"
     */
    TIME_RANGE("시간 범위", "특정 시간대나 시간 범위 내의 활동",
               List.of(RuleOperator.TIME_RANGE, RuleOperator.HOUR_RANGE, RuleOperator.MINUTE_RANGE),
               TimeRangeCondition.class,
               "timeRange"),
    
    /**
     * 빈도/횟수 체크 (시간 윈도우 내 발생 횟수)
     * 예: COUNT_WITHIN "3,1h" (1시간 내 3회)
     */
    FREQUENCY_CHECK("빈도 체크", "일정 시간 내 발생 횟수 체크",
                    List.of(RuleOperator.COUNT_WITHIN, RuleOperator.SUM_WITHIN),
                    CountWithinCondition.class,
                    "frequency"),
    
    /**
     * 기간 체크 (경과 시간)
     * 예: account_age >= "30d" (계좌 개설 후 30일 경과)
     */
    DURATION_CHECK("기간 체크", "특정 이벤트로부터 경과된 시간",
                   List.of(RuleOperator.WITHIN, RuleOperator.BEFORE, RuleOperator.AFTER),
                   DurationCondition.class,
                   "duration"),
    
    /**
     * 불린 체크 (참/거짓 상태)
     * 예: device_changed = true, is_night_time = true
     */
    BOOLEAN_CHECK("불린 체크", "참/거짓 상태 확인",
                  List.of(RuleOperator.IS_TRUE, RuleOperator.IS_FALSE),
                  BooleanCondition.class,
                  "boolean");
    
    private final String displayName;
    private final String description;
    private final List<RuleOperator> supportedOperators;
    private final Class<? extends RuleCondition> implementationClass;
    private final String inputType; // UI에서 사용할 입력 타입
    
    OperatorCategory(String displayName, String description, List<RuleOperator> supportedOperators,
                    Class<? extends RuleCondition> implementationClass, String inputType) {
        this.displayName = displayName;
        this.description = description;
        this.supportedOperators = supportedOperators;
        this.implementationClass = implementationClass;
        this.inputType = inputType;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<RuleOperator> getSupportedOperators() {
        return supportedOperators;
    }
    
    public Class<? extends RuleCondition> getImplementationClass() {
        return implementationClass;
    }
    
    public String getInputType() {
        return inputType;
    }
    
    /**
     * 특정 연산자를 지원하는지 확인
     */
    public boolean supports(RuleOperator operator) {
        return supportedOperators.contains(operator);
    }
}
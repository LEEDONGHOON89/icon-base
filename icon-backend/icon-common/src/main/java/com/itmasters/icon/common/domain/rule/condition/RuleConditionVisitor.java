package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.rule.RuleOperator;

/**
 * RuleCondition에 대한 Visitor 패턴 인터페이스
 * 
 * 다양한 연산(직렬화, 검증, SQL 생성 등)을 수행하기 위한 Visitor 패턴 구현
 * 새로운 Condition 타입 추가 시 이 인터페이스에 visit 메서드를 추가해야 함
 * 
 * @param <T> Visitor가 반환하는 타입
 */
public interface RuleConditionVisitor<T> {
    T visit(ComparisonCondition condition);
    T visit(AmountCondition condition);
    T visit(DurationCondition condition);
    T visit(TimeRangeCondition condition);
    T visit(TimeDifferenceCondition condition);
    T visit(TimeRangeWithMinutesCondition condition);
    T visit(CountWithinCondition condition);
    T visit(SumWithinCondition condition);
    T visit(DistinctCountCondition condition);
    
    /**
     * BooleanCondition 방문 (IS_TRUE, IS_FALSE 연산자 처리)
     * @param condition Boolean 조건
     * @return 처리 결과
     */
    default T visitBooleanCondition(BooleanCondition condition) {
        // 기본 구현: ComparisonCondition처럼 처리
        return visit(ComparisonCondition.of(condition.getFieldName(), 
                                           condition.getOperator(), 
                                           condition.getOperator() == RuleOperator.IS_TRUE ? "true" : "false"));
    }
}
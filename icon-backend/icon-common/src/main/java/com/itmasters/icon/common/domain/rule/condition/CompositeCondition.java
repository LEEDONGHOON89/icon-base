package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.rule.RuleOperator;

import java.util.List;
import java.util.Map;

/**
 * 여러 RuleCondition을 AND로 결합하는 복합 조건
 * - condition_data가 JSON 배열([ {fieldName,operator,value}, ... ])인 경우 사용
 */
public class CompositeCondition extends RuleCondition {
    private final List<RuleCondition> conditions;

    public CompositeCondition(List<RuleCondition> conditions) {
        super("__composite__");
        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException("CompositeCondition requires at least one child condition");
        }
        this.conditions = List.copyOf(conditions);
    }

    @Override
    public RuleOperator getOperator() {
        // 특별한 연산자 정의가 없으므로 placeholder 반환
        return RuleOperator.EQUALS;
    }

    @Override
    public boolean evaluate(Map<String, Object> context) {
        for (RuleCondition c : conditions) {
            if (c.requiresHistory()) {
                // 이력이 필요한 경우, 기본 evaluate로는 true로 보지 않음
                // 엔진 상위에서 matchesWithHistory가 호출될 수 있음
                return false;
            }
            if (!c.matches(context)) return false;
        }
        return true;
    }

    @Override
    public boolean requiresHistory() {
        return conditions.stream().anyMatch(RuleCondition::requiresHistory);
    }

    @Override
    public boolean matchesWithHistory(Map<String, Object> context, List<?> history) {
        for (RuleCondition c : conditions) {
            boolean ok = c.requiresHistory() ? c.matchesWithHistory(context, history) : c.matches(context);
            if (!ok) return false;
        }
        return true;
    }

    @Override
    public String toExpression() {
        return String.join(" AND ", conditions.stream().map(RuleCondition::toExpression).toList());
    }

    @Override
    public boolean isValid() {
        return !conditions.isEmpty() && conditions.stream().allMatch(RuleCondition::isValid);
    }

    @Override
    public Object getValue() {
        return conditions.size();
    }

    @Override
    public <T> T accept(RuleConditionVisitor<T> visitor) {
        // Visitor에 Composite 타입이 없으므로, 첫 번째 조건으로 대체 직렬화
        return conditions.get(0).accept(visitor);
    }

    public List<RuleCondition> getConditions() {
        return conditions;
    }
}


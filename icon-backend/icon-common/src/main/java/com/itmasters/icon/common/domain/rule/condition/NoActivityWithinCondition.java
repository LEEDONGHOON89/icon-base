package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.rule.RuleOperator;

import java.util.Map;

/**
 * N분 이내 무이력(NO_ACTIVITY) 조건
 * - fieldName 축(예: account_id, device_id 등)에 대해 지정된 분(minute) 동안 활동(이벤트)이 없음을 의미
 * - 실제 엔진에서는 event_stream 조회로 판정하며, 여기서는 컨텍스트 힌트를 사용
 *   컨텍스트 키 컨벤션: "{fieldName}_no_activity_within_{minutes}"
 *   값이 Boolean(true) 이면 무이력으로 간주
 */
public class NoActivityWithinCondition extends RuleCondition {

    private final int minutes;

    public NoActivityWithinCondition(String fieldName, int minutes) {
        super(fieldName);
        this.minutes = minutes;
    }

    public static NoActivityWithinCondition of(String fieldName, int minutes) {
        return new NoActivityWithinCondition(fieldName, minutes);
    }

    @Override
    public RuleOperator getOperator() {
        return RuleOperator.NO_ACTIVITY_WITHIN;
    }

    @Override
    public boolean evaluate(Map<String, Object> context) {
        Object hint = context.get(fieldName + "_no_activity_within_" + minutes);
        if (hint instanceof Boolean b) {
            return b;
        }
        // 힌트가 없으면 기본적으로 불일치 처리 (엔진 측에서 조회해서 설정해야 함)
        return false;
    }

    @Override
    public boolean requiresHistory() {
        // 무이력 판정은 이력 조회가 필요함
        return true;
    }

    @Override
    public boolean matchesWithHistory(Map<String, Object> context, java.util.List<?> history) {
        // 지정된 윈도우 내 해당 그룹키의 이벤트가 없으면 무이력 → true
        // history는 이미 그룹키/시간 윈도우로 필터링되어 전달됨
        return history == null || history.isEmpty();
    }

    @Override
    public String toExpression() {
        return String.format("NO_ACTIVITY_WITHIN(%s,%dmin)", fieldName, minutes);
    }

    @Override
    public boolean isValid() {
        return fieldName != null && !fieldName.isEmpty() && minutes > 0;
    }

    @Override
    public Object getValue() {
        return minutes;
    }

    public int getMinutes() {
        return minutes;
    }

    @Override
    public <T> T accept(RuleConditionVisitor<T> visitor) {
        // 전용 Visitor가 없다면 기본 Comparison 계열로 처리하지 않고 null 반환 가능
        return null;
    }
}

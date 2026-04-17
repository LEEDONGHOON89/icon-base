package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.rule.RuleOperator;

import java.util.Map;
import java.util.List;

/**
 * 시퀀스 조건: 이벤트 A 이후 N분 이내 이벤트 B 발생
 * - prevEventKey, nextEventKey는 엔진에서 식별 가능한 이벤트 키(또는 shape) 명칭
 * - 실제 엔진에서는 event_stream 상관조회로 판정하며, 여기서는 컨텍스트 힌트를 사용
 *   컨텍스트 키 컨벤션: "sequence_" + prev + "_" + next + "_within_" + minutes
 *   값이 Boolean(true) 이면 조건 만족으로 간주
 * - fieldName은 의미상 비사용(호환 목적). 필요 시 "sequence" 등 고정값을 넣어 사용
 */
public class SequenceWithinCondition extends RuleCondition {

    private final String prevEventKey;
    private final String nextEventKey;
    private final int minutes;

    public SequenceWithinCondition(String fieldName, String prevEventKey, String nextEventKey, int minutes) {
        super(fieldName);
        this.prevEventKey = prevEventKey;
        this.nextEventKey = nextEventKey;
        this.minutes = minutes;
    }

    public static SequenceWithinCondition of(String fieldName, String prevEventKey, String nextEventKey, int minutes) {
        return new SequenceWithinCondition(fieldName, prevEventKey, nextEventKey, minutes);
    }

    @Override
    public RuleOperator getOperator() {
        return RuleOperator.SEQUENCE_WITHIN;
    }

    @Override
    public boolean requiresHistory() { return true; }

    @Override
    public boolean evaluate(Map<String, Object> context) {
        // 기본 구현은 힌트 키를 우선 사용 (하위호환)
        String key = String.format("sequence_%s_%s_within_%d", prevEventKey, nextEventKey, minutes);
        Object hint = context.get(key);
        if (hint instanceof Boolean b) return b;
        return false;
    }

    @Override
    public boolean matchesWithHistory(Map<String, Object> context, List<?> history) {
        if (history == null || history.isEmpty()) return false;
        // 현재 이벤트가 nextEventKey 인지 확인
        String currentType = resolveTypeFromMap(context);
        if (currentType == null || !nextEventKey.equalsIgnoreCase(currentType)) return false;
        // 이력 중 prevEventKey 존재 여부 확인 (시간 윈도우는 호출측에서 제한)
        for (Object item : history) {
            Map<String,Object> ev = extractEventData(item);
            if (ev == null) continue;
            String t = resolveTypeFromMap(ev);
            if (t != null && prevEventKey.equalsIgnoreCase(t)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> extractEventData(Object item) {
        try {
            if (item instanceof Map<?,?> map) return (Map<String,Object>) map;
            java.lang.reflect.Method m = item.getClass().getMethod("getEventData");
            Object data = m.invoke(item);
            if (data instanceof Map<?,?> map) return (Map<String,Object>) map;
        } catch (Exception ignore) { }
        return null;
    }

    private String resolveTypeFromMap(Map<String,Object> map) {
        Object v = map.get("event_type");
        if (v == null) v = map.get("TRX_TYPE");
        if (v == null) v = map.get("transaction_type");
        return v != null ? v.toString() : null;
    }

    @Override
    public String toExpression() {
        return String.format("SEQUENCE_WITHIN(%s→%s,%dmin)", prevEventKey, nextEventKey, minutes);
    }

    @Override
    public boolean isValid() {
        return prevEventKey != null && !prevEventKey.isEmpty()
                && nextEventKey != null && !nextEventKey.isEmpty()
                && minutes > 0;
    }

    @Override
    public Object getValue() {
        return prevEventKey + "," + nextEventKey + "," + minutes;
    }

    public String getPrevEventKey() {
        return prevEventKey;
    }

    public String getNextEventKey() {
        return nextEventKey;
    }

    public int getMinutes() {
        return minutes;
    }

    @Override
    public <T> T accept(RuleConditionVisitor<T> visitor) {
        return null;
    }
}

package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.rule.RuleOperator;

import java.util.List;
import java.util.Map;

/**
 * 규칙 조건의 추상 클래스
 * 각 조건 타입별로 구체적인 구현체를 만들어 사용
 * 
 * 중요: 모든 RuleCondition 구현체는 불변(immutable) 객체여야 함
 * - 모든 필드는 final로 선언
 * - setter 메서드 제공 금지
 * - 생성 후 상태 변경 불가
 */
public abstract class RuleCondition {
    
    protected final String fieldName;
    
    protected RuleCondition(String fieldName) {
        this.fieldName = fieldName;
    }

    public abstract RuleOperator getOperator();
    
    /**
     * 조건 평가 (matches로 변경 - 더 직관적인 이름)
     */
    public abstract boolean evaluate(Map<String, Object> context);
    
    /**
     * 조건 평가 - evaluate()와 동일하지만 더 직관적인 메서드명
     */
    public boolean matches(Map<String, Object> context) {
        return evaluate(context);
    }
    
    /**
     * 조건을 읽기 쉬운 문자열로 표현
     */
    public abstract String toExpression();
    
    /**
     * 조건 검증
     */
    public abstract boolean isValid();
    
    /**
     * 조건의 값을 반환 (DB 저장 및 API 응답용)
     * 각 구현체는 자신의 조건 값을 적절한 형식으로 반환해야 함
     * 예: TimeRangeCondition은 "10-11", TimeRangeWithMinutesCondition은 "10:30,11:45"
     */
    public abstract Object getValue();
    
    /**
     * 이력 데이터가 필요한 조건인지 확인
     * COUNT_WITHIN, SUM_WITHIN 등의 연산자는 이력 데이터가 필요함
     * 기본 구현에서는 false 반환 (대부분의 조건은 이력이 필요없음)
     */
    public boolean requiresHistory() {
        return false;
    }
    
    /**
     * 이력 데이터를 포함한 조건 평가
     * requiresHistory()가 true인 경우에만 호출됨
     * 기본 구현에서는 일반 평가로 대체
     * 
     * @param context 현재 데이터 컨텍스트
     * @param history 이력 이벤트 데이터 (TODO: 타입 정의 필요)
     * @return 조건 매칭 여부
     */
    public boolean matchesWithHistory(Map<String, Object> context, List<?> history) {
        // 기본적으로는 일반 평가를 수행
        return matches(context);
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    /**
     * Visitor 패턴을 위한 accept 메서드
     * 각 구체적인 Condition 클래스에서 구현해야 함
     * 
     * @param visitor 적용할 visitor
     * @param <T> visitor가 반환하는 타입
     * @return visitor 처리 결과
     */
    public abstract <T> T accept(RuleConditionVisitor<T> visitor);
    
}
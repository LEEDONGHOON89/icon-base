package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.RuleDomain;

/**
 * 룰 조건의 새로운 추상 클래스 (V2)
 * 도메인 중심의 1차 분류와 이력 데이터 처리를 지원
 * 
 * 설계 원칙:
 * 1. 도메인별 전용 구현체 (CustomerCondition, MoneyCondition 등)
 * 2. supports(domain)로 1차 분류
 * 3. evaluate(groupKey, ruleEntity)로 이력 데이터 처리
 * 
 * 예시 룰 조건들:
 * - C01: 만65세 이상 고객 → CustomerCondition
 * - M01: 10만원 이상 → MoneyCondition  
 * - TM01: 10분 이내 → TimeCondition
 * - F01: 2회 이상 → FrequencyCondition
 */
public abstract class RuleConditionV2 {
    
    /**
     * 도메인 기반 지원 여부 확인 (1차 분류)
     * 각 구현체는 단일 도메인만 지원해야 함
     * 
     * @param domain 룰 도메인 (CUSTOMER, DEVICE, TRANSACTION 등)
     * @return 해당 도메인을 지원하면 true
     */
    public abstract boolean supports(RuleDomain domain);
    
    /**
     * 룰 조건 평가 (이력 데이터 지원)
     * 
     * @param groupKey 상관관계 키 (고객ID, 계좌번호 등) - event_stream에서 이력 조회용
     * @param ruleEntity 룰 엔티티 (conditionData, 연산자 등 포함)
     * @return 조건 만족 여부
     */
    public abstract boolean evaluate(String groupKey, Object ruleEntity);
    
    /**
     * 조건 타입 식별자 반환
     * 디버깅 및 로깅용
     * 
     * @return 조건 타입 (예: "AGE_COMPARISON", "AMOUNT_RANGE", "TIME_WITHIN")
     */
    public abstract String getConditionType();
    
    /**
     * 조건을 읽기 쉬운 문자열로 표현
     * UI 표시 및 로깅용
     * 
     * @param ruleEntity 룰 엔티티
     * @return 사람이 읽기 쉬운 조건 표현 (예: "나이 >= 65세", "금액 >= 10만원")
     */
    public abstract String toHumanReadableString(Object ruleEntity);
    
    /**
     * 이 조건이 이력 데이터를 필요로 하는지 확인
     * 
     * @return 이력 데이터 필요 시 true (예: "최근 12개월 거래이력 없음")
     */
    public boolean requiresHistoryData() {
        return false; // 기본값은 false, 필요한 구현체에서 override
    }
    
    /**
     * 조건 검증
     * ruleEntity의 conditionData가 이 구현체에 적합한지 확인
     * 
     * @param ruleEntity 검증할 룰 엔티티
     * @return 유효한 조건이면 true
     */
    public abstract boolean isValidCondition(Object ruleEntity);
}
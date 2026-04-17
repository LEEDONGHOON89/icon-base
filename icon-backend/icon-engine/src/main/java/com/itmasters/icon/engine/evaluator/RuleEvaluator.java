package com.itmasters.icon.engine.evaluator;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineEventStreamEntity;

import java.util.List;
import java.util.Map;

/**
 * 룰 평가기 인터페이스
 * 각 룰 타입별로 이 인터페이스를 구현하여 평가 로직을 정의
 */
public interface RuleEvaluator {
    
    /**
     * 룰 평가 수행 (기본 방법 - 현재 데이터만 사용)
     * 
     * @param context 평가에 필요한 데이터 컨텍스트
     * @param ruleConfig 룰 설정 (JSON 문자열)
     * @return 룰 매칭 여부 (true: 탐지, false: 정상)
     */
    boolean evaluate(Map<String, Object> context, String ruleConfig);
    

    
    /**
     * 이 평가기가 이력 데이터를 사용하는지 여부
     * 
     * @return true: 이력 기반 평가 지원, false: 현재 데이터만 사용
     */
    default boolean requiresHistory() {
        return false;
    }
    
    /**
     * 평가기가 지원하는 룰 타입
     * 
     * @return 지원하는 룰 타입 이름
     */
    String getSupportedType();
}
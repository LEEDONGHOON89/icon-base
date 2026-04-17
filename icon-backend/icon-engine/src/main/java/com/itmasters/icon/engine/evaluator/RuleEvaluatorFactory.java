package com.itmasters.icon.engine.evaluator;

import com.itmasters.icon.common.domain.RuleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RuleType에 따라 적절한 RuleEvaluator를 제공하는 팩토리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleEvaluatorFactory {
    
    private final List<RuleEvaluator> evaluators;
    private final Map<String, RuleEvaluator> evaluatorMap = new HashMap<>();
    
    @PostConstruct
    public void init() {
        // 모든 Evaluator를 타입별로 매핑
        for (RuleEvaluator evaluator : evaluators) {
            String type = evaluator.getSupportedType();
            evaluatorMap.put(type, evaluator);
            log.info("Registered evaluator for type: {}", type);
        }
    }
    
    /**
     * RuleType에 해당하는 Evaluator 반환
     * 
     * @param ruleType 룰 타입
     * @return 해당하는 Evaluator, 없으면 null
     */
    public RuleEvaluator getEvaluator(RuleType ruleType) {
        return getEvaluator(ruleType.name());
    }
    
    /**
     * RuleType 문자열에 해당하는 Evaluator 반환
     * 
     * @param ruleTypeName 룰 타입 이름
     * @return 해당하는 Evaluator, 없으면 null
     */
    public RuleEvaluator getEvaluator(String ruleTypeName) {
        RuleEvaluator evaluator = evaluatorMap.get(ruleTypeName);
        
        if (evaluator == null) {
            log.warn("No evaluator found for rule type: {}", ruleTypeName);
        }
        
        return evaluator;
    }
    
    /**
     * 지원되는 룰 타입 목록
     * 
     * @return 지원되는 룰 타입 이름 목록
     */
    public java.util.Set<String> getSupportedTypes() {
        return evaluatorMap.keySet();
    }
    
    /**
     * 특정 룰 타입이 지원되는지 확인
     * 
     * @param ruleType 룰 타입
     * @return 지원 여부
     */
    public boolean isSupported(RuleType ruleType) {
        return evaluatorMap.containsKey(ruleType.name());
    }
    
    /**
     * 특정 룰 타입이 지원되는지 확인
     * 
     * @param ruleTypeName 룰 타입 이름
     * @return 지원 여부
     */
    public boolean isSupported(String ruleTypeName) {
        return evaluatorMap.containsKey(ruleTypeName);
    }
}
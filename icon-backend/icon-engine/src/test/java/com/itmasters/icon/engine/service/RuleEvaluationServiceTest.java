package com.itmasters.icon.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.common.domain.RuleDomain;
import com.itmasters.icon.common.domain.rule.RuleOperator;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineRuleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RuleEvaluationServiceTest {
    
    private RuleEvaluationService service;
    private EventStreamService eventStreamService;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // Mock EventStreamService
        eventStreamService = mock(EventStreamService.class);
        
        service = new RuleEvaluationService(eventStreamService, objectMapper);
    }
    
    @Test
    @DisplayName("새로운 RuleCondition 기반 룰 평가 테스트")
    void testEvaluateSimpleRule() {
        // Given
        EngineRuleEntity rule = EngineRuleEntity.builder()
            .ruleId("RULE_001")
            .ruleName("고액 이체 감지")
            .domain(RuleDomain.FINANCIAL_TRANSACTION)
            .operator(RuleOperator.GREATER_THAN_OR_EQUALS)
            .isActive(true)
            .build();
        
        Map<String, Object> context = new HashMap<>();
        context.put("transaction_amount", BigDecimal.valueOf(15000000));
        
        // When
        boolean result = service.evaluateRule(rule, context);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("텍스트 비교 룰 평가 테스트")
    void testEvaluateTextRule() {
        // Given
        EngineRuleEntity rule = EngineRuleEntity.builder()
            .ruleId("RULE_002")
            .ruleName("이체 거래 감지")
            .domain(RuleDomain.FINANCIAL_TRANSACTION)
            .operator(RuleOperator.EQUALS)
            .isActive(true)
            .build();
        
        Map<String, Object> context = new HashMap<>();
        context.put("transaction_type", "이체");
        
        // When
        boolean result = service.evaluateRule(rule, context);
        
        // Then
        assertThat(result).isTrue();
    }
    
    // TODO: 향후 더 많은 테스트 케이스 추가 예정
    // - 여러 룰 동시 평가
    // - 이력 기반 룰 평가  
    // - 오류 상황 처리 테스트
}
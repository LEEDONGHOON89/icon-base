package com.itmasters.icon.engine.evaluator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.common.domain.RuleType;
import com.itmasters.icon.engine.evaluator.impl.SimpleRuleEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleRuleEvaluatorTest {
    
    private SimpleRuleEvaluator evaluator;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        evaluator = new SimpleRuleEvaluator(objectMapper);
    }
    
    @Test
    @DisplayName("숫자 필드가 특정 값보다 큰 경우 감지")
    void testNumericGreaterThan() {
        // Given
        Map<String, Object> context = new HashMap<>();
        context.put("amount", BigDecimal.valueOf(1000000));
        
        String ruleConfig = """
            {
                "field": "amount",
                "operator": ">",
                "value": 500000
            }
            """;
        
        // When
        boolean result = evaluator.evaluate(context, ruleConfig);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("문자열 필드가 특정 값과 일치하는 경우 감지")
    void testStringEquals() {
        // Given
        Map<String, Object> context = new HashMap<>();
        context.put("country", "CN");
        
        String ruleConfig = """
            {
                "field": "country",
                "operator": "==",
                "value": "CN"
            }
            """;
        
        // When
        boolean result = evaluator.evaluate(context, ruleConfig);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("필드가 리스트에 포함된 경우 감지")
    void testInList() {
        // Given
        Map<String, Object> context = new HashMap<>();
        context.put("country", "VN");
        
        String ruleConfig = """
            {
                "field": "country",
                "operator": "in",
                "value": ["CN", "VN", "KH", "TH"]
            }
            """;
        
        // When
        boolean result = evaluator.evaluate(context, ruleConfig);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("숫자가 범위 내에 있는 경우 감지")
    void testBetween() {
        // Given
        Map<String, Object> context = new HashMap<>();
        context.put("age", 67);
        
        String ruleConfig = """
            {
                "field": "age",
                "operator": "between",
                "value": {
                    "min": 65,
                    "max": 100
                }
            }
            """;
        
        // When
        boolean result = evaluator.evaluate(context, ruleConfig);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("필드가 컨텍스트에 없는 경우 false 반환")
    void testFieldNotFound() {
        // Given
        Map<String, Object> context = new HashMap<>();
        context.put("amount", 1000);
        
        String ruleConfig = """
            {
                "field": "nonExistentField",
                "operator": ">",
                "value": 0
            }
            """;
        
        // When
        boolean result = evaluator.evaluate(context, ruleConfig);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("지원하는 룰 타입 확인")
    void testSupportedType() {
        // When
        String supportedType = evaluator.getSupportedType();
        
        // Then
        assertThat(supportedType).isEqualTo(RuleType.SIMPLE.name());
    }
}
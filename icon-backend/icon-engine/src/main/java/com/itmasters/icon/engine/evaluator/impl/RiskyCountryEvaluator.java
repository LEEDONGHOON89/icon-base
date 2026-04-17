package com.itmasters.icon.engine.evaluator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.common.domain.RuleType;
import com.itmasters.icon.engine.evaluator.RuleEvaluator;
import com.itmasters.icon.engine.util.FieldValueExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 위험 국가 접속 감지 룰 평가기
 * 위험 국가 목록에서의 접속 감지
 * 예: 중국, 베트남, 북한 등 위험국가에서 접속
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskyCountryEvaluator implements RuleEvaluator {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public boolean evaluate(Map<String, Object> context, String ruleConfig) {
        try {
            JsonNode config = objectMapper.readTree(ruleConfig);
            
            // 위험 국가 목록 추출
            Set<String> riskyCountries = new HashSet<>();
            JsonNode countriesNode = config.get("countries");
            
            if (countriesNode != null && countriesNode.isArray()) {
                for (JsonNode country : countriesNode) {
                    riskyCountries.add(country.asText().toUpperCase());
                }
            }
            
            // context에서 접속 국가 추출 (FieldValueExtractor 사용)
            String accessCountry = FieldValueExtractor.extractAccessCountry(context);

            if (accessCountry == null) {
                log.debug("Access country not found in context");
                return false;
            }
            
            // 위험 국가 체크
            boolean isRisky = riskyCountries.contains(accessCountry.toUpperCase());
            
            log.debug("Risky country check - country: {}, risky list: {}, result: {}", 
                     accessCountry, riskyCountries, isRisky);
            
            return isRisky;
            
        } catch (Exception e) {
            log.error("Error evaluating risky country rule: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getSupportedType() {
        return RuleType.RISKY_COUNTRY.name();
    }
}
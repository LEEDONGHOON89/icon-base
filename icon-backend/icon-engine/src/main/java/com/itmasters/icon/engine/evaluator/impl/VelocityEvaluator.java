package com.itmasters.icon.engine.evaluator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.common.domain.RuleType;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineEventStreamEntity;
import com.itmasters.icon.engine.evaluator.RuleEvaluator;
import com.itmasters.icon.engine.util.FieldValueExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 빈도 체크 룰 평가기
 * 일정 시간 내 발생 횟수만 체크 (금액 조건 없음)
 * 예: 30분 내 3회 이상 거래
 * 
 * 이력 데이터 필요: YES - Event Stream 데이터를 기반으로 시간 윈도우 내 이벤트 개수 계산
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VelocityEvaluator implements RuleEvaluator {

    private final ObjectMapper objectMapper;

    /**
     * Event Stream 기반 평가 필수
     * 이 Evaluator는 이력 데이터가 필요하므로 evaluateWithHistory()를 사용해야 합니다.
     */
    @Override
    public boolean evaluate(Map<String, Object> context, String ruleConfig) {
        log.warn("VelocityEvaluator requires history data. Use evaluateWithHistory() instead.");
        return false;
    }

    @Override
    public String getSupportedType() {
        return RuleType.VELOCITY.name();
    }
    
    /**
     * VELOCITY 평가기는 이력 데이터를 필요로 함
     */
    @Override
    public boolean requiresHistory() {
        return true;
    }
}
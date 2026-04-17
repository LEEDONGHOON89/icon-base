package com.itmasters.icon.engine.evaluator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.common.domain.RuleType;
import com.itmasters.icon.common.domain.type.StandardFieldType;
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
 * 인증 타이밍 체크 룰 평가기
 * 인증 발급 후 경과 시간만 체크 (금액/행동 조건 없음)
 * 예: OTP 발급 후 180분 이내
 * 
 * 이력 데이터 필요: YES - Event Stream 데이터를 기반으로 과거 인증 이벤트 검색
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertTimingEvaluator implements RuleEvaluator {


    /**
     * Event Stream 기반 평가 필수
     * 이 Evaluator는 이력 데이터가 필요하므로 evaluateWithHistory()를 사용해야 합니다.
     */
    @Override
    public boolean evaluate(Map<String, Object> context, String ruleConfig) {
        log.warn("CertTimingEvaluator requires history data. Use evaluateWithHistory() instead.");
        return false;
    }
    
    private String getEventTypeForCert(String certType) {
        if ("OTP".equalsIgnoreCase(certType)) {
            return "OTP_ISSUE";
        } else if ("CERT".equalsIgnoreCase(certType) || "CERTIFICATE".equalsIgnoreCase(certType)) {
            return "CERT_ISSUE";
        }
        return certType;  // 그대로 사용
    }
    
    /**
     * Raw 데이터에서 이벤트 타입 추출
     * Event Stream 설계 원칙: Raw 데이터 기반으로 타입 판단
     */
    private String extractEventTypeFromRawData(Map<String, Object> eventData) {
        // 이벤트 타입 추출 - 다양한 필드명 시도
        Object eventType = eventData.get("event_type");
        if (eventType == null) {
            eventType = eventData.get("EVENT_TYPE");
        }
        if (eventType == null) {
            eventType = eventData.get("transaction_type");
        }
        if (eventType == null) {
            eventType = eventData.get("TRX_TYPE");
        }
        
        return eventType != null ? eventType.toString() : null;
    }

    @Override
    public String getSupportedType() {
        return RuleType.CERT_TIMING.name();
    }
    
    /**
     * CERT_TIMING 평가기는 이력 데이터를 필요로 함
     */
    @Override
    public boolean requiresHistory() {
        return true;
    }

}
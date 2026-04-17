package com.itmasters.icon.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.adapter.out.persistence.entity.ContextRuleMappingEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectionContextEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectionEventEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.ContextRuleMappingRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.DetectionContextRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.DetectionEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 시나리오 엔진 서비스
 * 탐지 컨텍스트와 이벤트를 관리하고 시나리오 기반 탐지를 수행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioEngineService {

    private final DetectionContextRepository contextRepository;
    private final DetectionEventRepository eventRepository;
    private final ContextRuleMappingRepository mappingRepository;
    private final ObjectMapper objectMapper;

    // 컨텍스트 만료 시간 (설정 가능, 기본값: 24시간)
    @Value("${icon.engine.context.expire-hours:24}")
    private int contextExpireHours;
    
    /**
     * 탐지 이벤트 처리 - 컨텍스트 생성 또는 업데이트
     * 
     * @param correlationKey 상관 관계 키
     * @param profileId 프로파일 ID
     * @param ruleId 룰 ID
     * @param matchedData 매칭된 데이터
     * @param execDsMpId 실행 ID
     * @return 처리된 컨텍스트
     */
    @Transactional
    public DetectionContextEntity processDetectionEvent(
            String correlationKey,
            String profileId,
            String ruleId,
            Map<String, Object> matchedData,
            Long execDsMpId) {
        
        log.debug("탐지 이벤트 처리 시작 - correlationKey: {}, profileId: {}, ruleId: {}", 
                 correlationKey, profileId, ruleId);
        
        // 1. 활성 컨텍스트 조회 또는 생성
        DetectionContextEntity context = findOrCreateContext(correlationKey, profileId);
        
        // 2. 이벤트 생성 및 저장
        DetectionEventEntity event = createEvent(context.getDetectionContextId(), ruleId, matchedData, execDsMpId);
        
        // 3. 컨텍스트-룰 매핑 업데이트
        updateContextRuleMapping(context.getDetectionContextId(), ruleId);
        
        // 4. 컨텍스트 상태 업데이트
        updateContextState(context, event);
        
        // 5. 위험도 평가
        evaluateRiskLevel(context);
        
        log.info("탐지 이벤트 처리 완료 - contextId: {}, eventCount: {}, riskLevel: {}", 
                context.getDetectionContextId(), context.getEventCount(), context.getRiskLevel());
        
        return context;
    }
    
    /**
     * 컨텍스트 조회 또는 생성
     */
    private DetectionContextEntity findOrCreateContext(String correlationKey, String profileId) {
        // 활성 컨텍스트 조회
        Optional<DetectionContextEntity> existingContext = 
            contextRepository.findActiveByCorrelationKey(correlationKey);
        
        if (existingContext.isPresent()) {
            log.debug("기존 컨텍스트 사용 - contextId: {}", existingContext.get().getDetectionContextId());
            return existingContext.get();
        }
        
        // 새 컨텍스트 생성
        DetectionContextEntity newContext = DetectionContextEntity.builder()
                .correlationKey(correlationKey)  // detect_key 컬럼에 매핑
                .keyType("CUSTOMER")  // 기본 키 타입
                .profileId(profileId)
                .status("ACTIVE")
                .eventCount(0)
                .ruleMatchCount(0)
                .windowStart(LocalDateTime.now())
                .windowEnd(LocalDateTime.now().plusHours(contextExpireHours))
                .windowDurationMinutes(contextExpireHours * 60)
                .riskLevel("LOW")
                .contextData(new HashMap<>())
                .build();
        
        newContext = contextRepository.save(newContext);
        log.info("새 컨텍스트 생성 - contextId: {}, correlationKey: {}", 
                newContext.getDetectionContextId(), correlationKey);
        
        return newContext;
    }
    
    /**
     * 탐지 이벤트 생성
     */
    private DetectionEventEntity createEvent(Long contextId, String ruleId,
                                            Map<String, Object> matchedData, Long execDsMpId) {
        try {
            String eventDataJson = objectMapper.writeValueAsString(matchedData);
            
            DetectionEventEntity event = DetectionEventEntity.builder()
                    .contextId(contextId)
                    .ruleId(ruleId)
                    .ruleName(ruleId)
                    .eventType("RULE_MATCH")
                    .eventTimestamp(LocalDateTime.now())
                    .eventData(eventDataJson)
                    .matchedConditions("[]")
                    .execDsMpId(execDsMpId)
                    .severity("MEDIUM")  // 기본 심각도
                    .category("DETECTION")  // 기본 카테고리
                    .build();
            
            event = eventRepository.save(event);
            log.debug("이벤트 생성 - eventId: {}, contextId: {}, ruleId: {}", 
                     event.getEventId(), contextId, ruleId);
            
            return event;
        } catch (Exception e) {
            log.error("이벤트 생성 실패 - contextId: {}, ruleId: {}", contextId, ruleId, e);
            throw new RuntimeException("이벤트 생성 실패", e);
        }
    }
    
    /**
     * 컨텍스트-룰 매핑 업데이트
     */
    private void updateContextRuleMapping(Long contextId, String ruleId) {
        // 기존 매핑 확인
        Optional<ContextRuleMappingEntity> existingMapping = 
            mappingRepository.findByDetectionContextIdAndRuleId(contextId, ruleId);
        
        if (existingMapping.isPresent()) {
            // 기존 매핑이 있으면 시간 업데이트
            ContextRuleMappingEntity mapping = existingMapping.get();
            mapping.setDetectionTimestamp(LocalDateTime.now());
            mappingRepository.save(mapping);
        } else {
            // 새 매핑 생성
            ContextRuleMappingEntity newMapping = ContextRuleMappingEntity.builder()
                    .detectionContextId(contextId)
                    .ruleId(ruleId)
                    .detectionTimestamp(LocalDateTime.now())
                    .sequenceNumber(1)  // 순서 번호
                    .severity("MEDIUM")  // 기본 심각도
                    .isTrigger(false)
                    .alertSent(false)
                    .build();
            
            mappingRepository.save(newMapping);
            log.debug("컨텍스트-룰 매핑 생성 - contextId: {}, ruleId: {}", contextId, ruleId);
        }
    }
    
    /**
     * 컨텍스트 상태 업데이트
     */
    private void updateContextState(DetectionContextEntity context, DetectionEventEntity event) {
        // 이벤트 카운트 증가
        context.setEventCount(context.getEventCount() + 1);
        
        // 룰 매치 카운트 증가
        long distinctRuleCount = mappingRepository.findByDetectionContextId(context.getDetectionContextId()).size();
        context.setRuleMatchCount((int) distinctRuleCount);
        
        // 메타데이터 업데이트 (최근 이벤트 정보)
        Map<String, Object> contextData = context.getContextData();
        if (contextData == null) {
            contextData = new HashMap<>();
        }
        contextData.put("lastRuleId", event.getRuleId());
        contextData.put("lastEventId", event.getEventId());
        contextData.put("lastEventType", event.getEventType());
        context.setContextData(contextData);
        
        context.setUpdatedAt(LocalDateTime.now());
        contextRepository.save(context);
    }
    
    /**
     * 위험도 평가
     * 이벤트 수와 매칭된 룰의 수를 기반으로 위험도 계산
     */
    private void evaluateRiskLevel(DetectionContextEntity context) {
        int eventCount = context.getEventCount();
        int ruleMatchCount = context.getRuleMatchCount();
        
        String riskLevel;
        double anomalyScore = calculateAnomalyScore(eventCount, ruleMatchCount);
        
        if (anomalyScore >= 80) {
            riskLevel = "CRITICAL";
        } else if (anomalyScore >= 60) {
            riskLevel = "HIGH";
        } else if (anomalyScore >= 40) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }
        
        // 위험도가 변경된 경우 업데이트
        if (!riskLevel.equals(context.getRiskLevel())) {
            log.info("위험도 변경 - contextId: {}, {} -> {}, score: {}", 
                    context.getDetectionContextId(), context.getRiskLevel(), riskLevel, anomalyScore);
            
            context.setRiskLevel(riskLevel);
            context.setAnomalyScore(anomalyScore);
            
            // CRITICAL 레벨일 경우 트리거 시간 기록
            if ("CRITICAL".equals(riskLevel)) {
                context.setTriggeredAt(LocalDateTime.now());
                sendCriticalAlert(context);
            }
            
            contextRepository.save(context);
        }
    }
    
    /**
     * 이상 점수 계산
     * 
     * @param eventCount 이벤트 수
     * @param ruleMatchCount 서로 다른 룰 매칭 수
     * @return 이상 점수 (0-100)
     */
    private double calculateAnomalyScore(int eventCount, int ruleMatchCount) {
        // 기본 점수 계산
        double eventScore = Math.min(eventCount * 10, 50);  // 최대 50점
        double ruleScore = Math.min(ruleMatchCount * 15, 50);  // 최대 50점
        
        return eventScore + ruleScore;
    }
    
    /**
     * CRITICAL 알림 발송
     */
    private void sendCriticalAlert(DetectionContextEntity context) {
        log.warn("=== CRITICAL 위험도 알림 ===");
        log.warn("컨텍스트 ID: {}", context.getDetectionContextId());
        log.warn("상관 관계 키: {}", context.getCorrelationKey());
        log.warn("프로파일 ID: {}", context.getProfileId());
        log.warn("이벤트 수: {}", context.getEventCount());
        log.warn("룰 매치 수: {}", context.getRuleMatchCount());
        log.warn("이상 점수: {}", context.getAnomalyScore());
        log.warn("=========================");
        
        // TODO: 실제 알림 발송 구현 (이메일, Slack, Webhook 등)
    }
    
    /**
     * 만료된 컨텍스트 정리
     */
    @Transactional
    public void cleanupExpiredContexts() {
        LocalDateTime expiredBefore = LocalDateTime.now().minusHours(contextExpireHours);
        contextRepository.findExpiredContexts(expiredBefore).forEach(context -> {
            context.setStatus("EXPIRED");
            contextRepository.save(context);
            log.info("컨텍스트 만료 처리 - contextId: {}", context.getDetectionContextId());
        });
    }
}

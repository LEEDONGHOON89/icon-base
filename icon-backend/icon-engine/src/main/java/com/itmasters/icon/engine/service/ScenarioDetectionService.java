package com.itmasters.icon.engine.service;

import com.itmasters.icon.engine.adapter.out.persistence.entity.*;
import com.itmasters.icon.engine.adapter.out.persistence.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 시나리오 탐지 서비스
 * 개별 룰 매칭 결과를 기반으로 시나리오 조건을 평가하고 탐지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioDetectionService {
    
    private final EngineScenarioRepository scenarioRepository;
    private final ScenarioRuleRepository scenarioRuleRepository;
    private final DetectionContextRepository contextRepository;
    private final ContextRuleMappingRepository contextRuleMappingRepository;
    private final DetectScenarioRepository detectScenarioRepository;
    private final ObjectMapper objectMapper;
    private final DetectRuleRepository detectRuleRepository;
    
    /**
     * 시나리오 탐지 평가 - 메인 메서드
     * 프로파일에 속한 활성 시나리오들을 평가하고 탐지 결과를 처리
     * 
     * @param profileId 프로파일 ID
     * @param detectionContextId 탐지 컨텍스트 ID
     * @return 탐지된 시나리오 목록
     */
    @Transactional
    public List<ScenarioDetectionResult> evaluateScenarios(String profileId, Long detectionContextId) {
        log.info("시나리오 탐지 평가 시작 - profileId: {}, contextId: {}", profileId, detectionContextId);
        
        List<ScenarioDetectionResult> detectionResults = new ArrayList<>();
        
        // 1. 컨텍스트 조회
        DetectionContextEntity context = contextRepository.findById(detectionContextId)
            .orElseThrow(() -> new RuntimeException("Detection context not found: " + detectionContextId));
        
        // 2. 프로파일과 연관된 활성 시나리오 조회
        List<EngineScenarioEntity> activeScenarios = scenarioRepository.findActiveScenariosByProfileId(profileId);
        log.debug("프로파일 {}에 대한 활성 시나리오 {}개 발견", profileId, activeScenarios.size());
        
        // 3. 컨텍스트에서 매칭된 룰 ID들 조회
        Set<String> matchedRuleIds = getMatchedRuleIdsFromContext(detectionContextId);
        log.debug("컨텍스트에서 매칭된 룰 {}개: {}", matchedRuleIds.size(), matchedRuleIds);
        
        // 4. 각 시나리오별로 평가
        for (EngineScenarioEntity scenario : activeScenarios) {
            ScenarioDetectionResult result = evaluateScenario(scenario, matchedRuleIds, context);
            
            if (result.isDetected()) {
                log.info("시나리오 탐지됨 - scenarioId: {}, scenarioName: {}", 
                    scenario.getScenarioId(), scenario.getScenarioName());
                detectionResults.add(result);
                
                // 5. 시나리오 탐지 시 컨텍스트 업데이트
                updateContextForScenarioDetection(context, scenario, result);
            }
        }
        
        return detectionResults;
    }

    /**
     * execDsMpId 포함: 평가 + 결과 저장(중복 방지)까지 수행
     */
    @Transactional
    public List<ScenarioDetectionResult> evaluateScenarios(String profileId,
                                                           Long detectionContextId,
                                                           Long execDsMpId) {
        List<ScenarioDetectionResult> results = evaluateScenarios(profileId, detectionContextId);
        DetectionContextEntity context = contextRepository.findById(detectionContextId)
                .orElseThrow(() -> new RuntimeException("Detection context not found: " + detectionContextId));

        for (ScenarioDetectionResult r : results) {
            if (!r.isDetected()) continue;
            try {
                // 중복 방지 로직 제거: 룰 기반 탐지는 매번 새로운 평가 결과이므로 중복 체크 불필요
                // exec_id가 다르면 다른 실행이므로 저장해야 함

                // 탐지 실행 시간 사용 (거래 시간이 아닌 현재 탐지 시점)
                LocalDateTime detectedAt = LocalDateTime.now();

                DetectScenarioEntity entity = DetectScenarioEntity.builder()
                        .groupKey(context.getCorrelationKey())
                        .scenarioId(r.getScenarioId())
                        .detectedDt(detectedAt)  // 탐지 실행 시간
                        .eventDt(detectedAt)  // TODO: 실제 이벤트 발생 시간으로 수정 필요
                        .riskLevel(context.getRiskLevel())
                        .windowStart(null)
                        .windowEnd(detectedAt)   // 현재 시간 (탐지 종료 시점)
                        .build();
                detectScenarioRepository.save(entity);

                log.info("시나리오 탐지 저장 - scenarioId={}, groupKey={}, execDsMpId={}",
                         r.getScenarioId(), context.getCorrelationKey(), execDsMpId);
            } catch (Exception e) {
                log.error("시나리오 결과 저장 실패 - scenarioId: {}", r.getScenarioId(), e);
            }
        }
        return results;
    }
    
    /**
     * 개별 시나리오 평가
     * 시나리오에 정의된 룰들과 논리 연산자를 기반으로 조건 만족 여부 판단
     * 
     * @param scenario 시나리오 엔티티
     * @param matchedRuleIds 매칭된 룰 ID 집합
     * @param context 탐지 컨텍스트
     * @return 시나리오 탐지 결과
     */
    private ScenarioDetectionResult evaluateScenario(
            EngineScenarioEntity scenario, 
            Set<String> matchedRuleIds,
            DetectionContextEntity context) {
        
        String scenarioId = scenario.getScenarioId();
        log.debug("시나리오 평가 시작 - scenarioId: {}, scenarioName: {}", 
            scenarioId, scenario.getScenarioName());
        
        // 1. 시나리오에 속한 룰들 조회 (순서대로)
        List<EngineScenarioRuleEntity> scenarioRules = 
            scenarioRuleRepository.findByScenarioIdOrderByOrderNo(scenarioId);
        
        if (scenarioRules.isEmpty()) {
            log.warn("시나리오 {}에 정의된 룰이 없습니다", scenarioId);
            return ScenarioDetectionResult.notDetected(scenarioId);
        }
        
        // 2. 시나리오 조건 평가 (AND/OR 연산)
        boolean isDetected = evaluateScenarioCondition(scenarioRules, matchedRuleIds);
        
        // 3. 탐지 결과 생성
        ScenarioDetectionResult result = new ScenarioDetectionResult();
        result.setScenarioId(scenarioId);
        result.setScenarioName(scenario.getScenarioName());
        result.setDetected(isDetected);
        result.setDetectionTime(LocalDateTime.now());
        result.setMatchedRules(getMatchedRulesInScenario(scenarioRules, matchedRuleIds));
        result.setTotalRulesInScenario(scenarioRules.size());
        result.setCompletionRate(calculateCompletionRate(scenarioRules, matchedRuleIds));
        
        return result;
    }
    
    /**
     * 시나리오 조건 평가 로직 (AND/OR 연산 처리)
     * 
     * 평가 규칙:
     * 1. 순서대로 룰을 평가
     * 2. AND 연산자: 이전 결과와 현재 룰 매칭 여부를 AND 연산
     * 3. OR 연산자: 이전 결과와 현재 룰 매칭 여부를 OR 연산
     * 4. 첫 번째 룰은 연산자 무시하고 매칭 여부만 확인
     * 
     * @param scenarioRules 시나리오 룰 목록 (순서대로)
     * @param matchedRuleIds 매칭된 룰 ID 집합
     * @return 시나리오 조건 만족 여부
     */
    private boolean evaluateScenarioCondition(
            List<EngineScenarioRuleEntity> scenarioRules, 
            Set<String> matchedRuleIds) {
        
        if (scenarioRules.isEmpty()) {
            return false;
        }
        
        // 첫 번째 룰 평가
        boolean result = matchedRuleIds.contains(scenarioRules.get(0).getRuleId());
        log.debug("룰 평가 [0]: ruleId={}, matched={}", 
            scenarioRules.get(0).getRuleId(), result);
        
        // 두 번째 룰부터 연산자 적용
        for (int i = 1; i < scenarioRules.size(); i++) {
            EngineScenarioRuleEntity rule = scenarioRules.get(i);
            boolean isRuleMatched = matchedRuleIds.contains(rule.getRuleId());
            
            // 연산자에 따라 처리
            if (rule.getOperator() == EngineScenarioRuleEntity.ScenarioOperator.AND) {
                result = result && isRuleMatched;
                log.debug("룰 평가 [{}]: ruleId={}, operator=AND, matched={}, result={}", 
                    i, rule.getRuleId(), isRuleMatched, result);
            } else if (rule.getOperator() == EngineScenarioRuleEntity.ScenarioOperator.OR) {
                result = result || isRuleMatched;
                log.debug("룰 평가 [{}]: ruleId={}, operator=OR, matched={}, result={}", 
                    i, rule.getRuleId(), isRuleMatched, result);
            } else {
                // 연산자가 없으면 AND로 처리 (기본값)
                result = result && isRuleMatched;
                log.debug("룰 평가 [{}]: ruleId={}, operator=null(AND), matched={}, result={}", 
                    i, rule.getRuleId(), isRuleMatched, result);
            }
            
            // 조기 종료 최적화
            // AND 연산에서 false가 나오면 더 이상 평가할 필요 없음
            if (!result && rule.getOperator() == EngineScenarioRuleEntity.ScenarioOperator.AND) {
                log.debug("AND 조건 실패로 조기 종료");
                break;
            }
        }
        
        return result;
    }
    
    /**
     * 컨텍스트에서 매칭된 룰 ID들 조회
     */
    private Set<String> getMatchedRuleIdsFromContext(Long detectionContextId) {
        List<ContextRuleMappingEntity> mappings = 
            contextRuleMappingRepository.findByDetectionContextId(detectionContextId);
        
        return mappings.stream()
            .map(ContextRuleMappingEntity::getRuleId)
            .collect(Collectors.toSet());
    }
    
    /**
     * 시나리오에서 매칭된 룰들 추출
     */
    private List<String> getMatchedRulesInScenario(
            List<EngineScenarioRuleEntity> scenarioRules, 
            Set<String> matchedRuleIds) {
        
        return scenarioRules.stream()
            .map(EngineScenarioRuleEntity::getRuleId)
            .filter(matchedRuleIds::contains)
            .collect(Collectors.toList());
    }
    
    /**
     * 시나리오 완성도 계산 (매칭된 룰 수 / 전체 룰 수)
     */
    private double calculateCompletionRate(
            List<EngineScenarioRuleEntity> scenarioRules, 
            Set<String> matchedRuleIds) {
        
        if (scenarioRules.isEmpty()) {
            return 0.0;
        }
        
        long matchedCount = scenarioRules.stream()
            .map(EngineScenarioRuleEntity::getRuleId)
            .filter(matchedRuleIds::contains)
            .count();
        
        return (double) matchedCount / scenarioRules.size() * 100;
    }
    
    /**
     * 시나리오 탐지 시 컨텍스트 업데이트
     */
    private void updateContextForScenarioDetection(
            DetectionContextEntity context,
            EngineScenarioEntity scenario,
            ScenarioDetectionResult result) {
        
        // 컨텍스트 업데이트
        context.setScenarioType(scenario.getScenarioName());
        context.setScenarioStage("DETECTED");
        context.setTriggeredAt(LocalDateTime.now());
        
        // 위험도 상향 조정 (시나리오 탐지 시)
        if (!"HIGH".equals(context.getRiskLevel()) && !"CRITICAL".equals(context.getRiskLevel())) {
            context.setRiskLevel("HIGH");
        }
        
        // 컨텍스트 데이터에 시나리오 정보 추가
        Map<String, Object> contextData = context.getContextData();
        if (contextData == null) {
            contextData = new HashMap<>();
        }
        contextData.put("detectedScenarioId", scenario.getScenarioId());
        contextData.put("detectedScenarioName", scenario.getScenarioName());
        contextData.put("scenarioCompletionRate", result.getCompletionRate());
        context.setContextData(contextData);
        
        contextRepository.save(context);
    }
    


    /**
     * 탐지 결과 DTO 생성 (집계 기반)
     */
    private ScenarioDetectionResult createDetectionResult(EngineScenarioEntity scenario,
                                                          List<EngineScenarioRuleEntity> scenarioAggregates,
                                                          Set<String> passedAggregateIds) {
        ScenarioDetectionResult result = new ScenarioDetectionResult();
        result.setScenarioId(scenario.getScenarioId());
        result.setScenarioName(scenario.getScenarioName());
        result.setDetected(true);
        result.setDetectionTime(LocalDateTime.now());
        result.setMatchedRules(new ArrayList<>(passedAggregateIds));
        result.setTotalRulesInScenario(scenarioAggregates.size());

        double completionRate = scenarioAggregates.isEmpty() ? 0.0 :
            (double) passedAggregateIds.size() / scenarioAggregates.size();
        result.setCompletionRate(completionRate);

        return result;
    }
    

    /**
     * 시나리오 탐지 결과 DTO
     */
    public static class ScenarioDetectionResult {
        private String scenarioId;
        private String scenarioName;
        private boolean detected;
        private LocalDateTime detectionTime;
        private List<String> matchedRules;
        private int totalRulesInScenario;
        private double completionRate;
        
        public static ScenarioDetectionResult notDetected(String scenarioId) {
            ScenarioDetectionResult result = new ScenarioDetectionResult();
            result.setScenarioId(scenarioId);
            result.setDetected(false);
            return result;
        }
        
        // Getters and Setters
        public String getScenarioId() { return scenarioId; }
        public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }
        
        public String getScenarioName() { return scenarioName; }
        public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
        
        public boolean isDetected() { return detected; }
        public void setDetected(boolean detected) { this.detected = detected; }
        
        public LocalDateTime getDetectionTime() { return detectionTime; }
        public void setDetectionTime(LocalDateTime detectionTime) { this.detectionTime = detectionTime; }
        
        public List<String> getMatchedRules() { return matchedRules; }
        public void setMatchedRules(List<String> matchedRules) { this.matchedRules = matchedRules; }
        
        public int getTotalRulesInScenario() { return totalRulesInScenario; }
        public void setTotalRulesInScenario(int totalRulesInScenario) { this.totalRulesInScenario = totalRulesInScenario; }
        
        public double getCompletionRate() { return completionRate; }
        public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }
    }
}

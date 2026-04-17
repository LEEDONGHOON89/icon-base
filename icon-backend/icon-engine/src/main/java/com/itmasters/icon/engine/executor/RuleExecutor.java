package com.itmasters.icon.engine.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineEventStreamEntity;
import com.itmasters.icon.engine.service.EventStreamService;
import com.itmasters.icon.common.domain.rule.ParseCondition;
import com.itmasters.icon.common.domain.rule.condition.RuleCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Rule 실행 엔진 - Event Stream 데이터 기반 룰 실행
 * 
 * Raw 데이터 기반 룰 실행:
 * - Event Stream에 저장된 Raw 데이터에서 필요한 필드 동적 추출
 * - event_type 불필요: 데이터 자체가 충분한 정보 포함
 * - Rule JSON 조건을 파싱하여 실제 데이터와 비교
 * - 타임라인 기반 복합 조건 지원 (시간 윈도우, 패턴 매칭)
 * 
 * 처리 흐름:
 * 1. Rule JSON 조건 파싱 (fieldName, operator, value)
 * 2. Event Stream에서 group_key별 최근 데이터 조회
 * 3. Raw 데이터에서 rule.fieldName 필드 값 추출
 * 4. rule.operator로 rule.value와 비교
 * 5. 조건 만족 시 Detection Event 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleExecutor {
    
    private final EventStreamService eventStreamService;
    private final ParseCondition parseCondition;
    private final ObjectMapper objectMapper;
    
    /**
     * 단일 룰 실행 - group_key 기반 최근 이벤트 대상
     * 
     * @param ruleId 룰 ID
     * @param ruleConditionJson 룰 조건 JSON (fieldName, operator, value)
     * @param groupKey 대상 그룹 키 (예: CUS001)
     * @param timeWindowMinutes 검사할 시간 윈도우 (분)
     * @param limit 최대 검사할 이벤트 수
     * @return 룰 매칭 결과
     */
    public RuleExecutionResult executeRule(String ruleId, 
                                         String ruleConditionJson,
                                         String groupKey,
                                         int timeWindowMinutes,
                                         int limit) {
        
        log.info("Rule 실행 시작 - ruleId: {}, groupKey: {}, timeWindow: {}분", 
                ruleId, groupKey, timeWindowMinutes);
        
        try {
            // 1. Rule 조건 파싱
            RuleCondition condition = parseCondition.parseConditionWithRuleField(ruleConditionJson);
            log.debug("Rule 조건 파싱 완료 - fieldName: {}, operator: {}", 
                    condition.getFieldName(), condition.getOperator());
            
            // 2. Event Stream에서 최근 데이터 조회
            List<EngineEventStreamEntity> recentEvents = eventStreamService.getRecentEvents(
                    groupKey, timeWindowMinutes, limit);
            
            if (recentEvents.isEmpty()) {
                log.debug("검사할 이벤트 없음 - groupKey: {}", groupKey);
                return RuleExecutionResult.noEvents(ruleId, groupKey);
            }
            
            log.debug("검사할 이벤트 {} 건 조회됨", recentEvents.size());
            
            // 3. 각 이벤트에 대해 룰 조건 검사
            int matchedCount = 0;
            EngineEventStreamEntity firstMatchedEvent = null;
            
            for (EngineEventStreamEntity event : recentEvents) {
                if (evaluateRuleCondition(condition, event)) {
                    matchedCount++;
                    if (firstMatchedEvent == null) {
                        firstMatchedEvent = event;
                    }
                    
                    log.debug("룰 매칭됨 - eventId: {}, timestamp: {}", 
                            event.getEventStreamId(), event.getEventDt());
                }
            }
            
            // 4. 결과 생성
            if (matchedCount > 0) {
                log.info("Rule 실행 완료 - ruleId: {}, 매칭: {}/{} 건", 
                        ruleId, matchedCount, recentEvents.size());
                return RuleExecutionResult.matched(ruleId, groupKey, matchedCount, 
                        recentEvents.size(), firstMatchedEvent);
            } else {
                log.debug("Rule 매칭 실패 - ruleId: {}, 검사: {} 건", ruleId, recentEvents.size());
                return RuleExecutionResult.notMatched(ruleId, groupKey, recentEvents.size());
            }
            
        } catch (Exception e) {
            log.error("Rule 실행 실패 - ruleId: {}, groupKey: {}", ruleId, groupKey, e);
            return RuleExecutionResult.error(ruleId, groupKey, e.getMessage());
        }
    }
    
    /**
     * 단일 이벤트에 대한 룰 조건 검사
     * 
     * @param condition 파싱된 룰 조건
     * @param event Event Stream 이벤트
     * @return 조건 만족 여부
     */
    private boolean evaluateRuleCondition(RuleCondition condition, EngineEventStreamEntity event) {
        try {
            // Raw 데이터에서 필드 값 추출 - Map<String, Object> 타입으로 직접 사용
            Map<String, Object> eventData = event.getEventData();
            
            // RuleCondition으로 검사 (Map<String, Object> 전달)
            return condition.evaluate(eventData);
            
        } catch (Exception e) {
            log.warn("룰 조건 검사 실패 - eventId: {}, fieldName: {}", 
                    event.getEventStreamId(), condition.getFieldName(), e);
            return false;
        }
    }
    
    /**
     * 복수 그룹에 대한 룰 실행 (배치 처리)
     * 
     * @param ruleId 룰 ID
     * @param ruleConditionJson 룰 조건 JSON
     * @param groupKeys 대상 그룹 키 목록
     * @param timeWindowMinutes 검사할 시간 윈도우
     * @param limit 그룹당 최대 검사할 이벤트 수
     * @return 그룹별 룰 실행 결과 목록
     */
    public List<RuleExecutionResult> executeRuleForGroups(String ruleId,
                                                         String ruleConditionJson, 
                                                         List<String> groupKeys,
                                                         int timeWindowMinutes,
                                                         int limit) {
        
        log.info("배치 Rule 실행 시작 - ruleId: {}, 대상 그룹: {} 개", ruleId, groupKeys.size());
        
        return groupKeys.stream()
                .map(groupKey -> executeRule(ruleId, ruleConditionJson, groupKey, timeWindowMinutes, limit))
                .toList();
    }
    
    /**
     * Rule 실행 결과 DTO
     */
    public static class RuleExecutionResult {
        private final String ruleId;
        private final String groupKey;
        private final boolean matched;
        private final int matchedCount;
        private final int totalChecked;
        private final EngineEventStreamEntity firstMatchedEvent;
        private final String errorMessage;
        private final long executionTime;
        
        private RuleExecutionResult(String ruleId, String groupKey, boolean matched,
                                    int matchedCount, int totalChecked,
                                    EngineEventStreamEntity firstMatchedEvent, String errorMessage) {
            this.ruleId = ruleId;
            this.groupKey = groupKey;
            this.matched = matched;
            this.matchedCount = matchedCount;
            this.totalChecked = totalChecked;
            this.firstMatchedEvent = firstMatchedEvent;
            this.errorMessage = errorMessage;
            this.executionTime = System.currentTimeMillis();
        }
        
        public static RuleExecutionResult matched(String ruleId, String groupKey, 
                                                int matchedCount, int totalChecked,
                                                EngineEventStreamEntity firstMatchedEvent) {
            return new RuleExecutionResult(ruleId, groupKey, true, matchedCount, 
                    totalChecked, firstMatchedEvent, null);
        }
        
        public static RuleExecutionResult notMatched(String ruleId, String groupKey, int totalChecked) {
            return new RuleExecutionResult(ruleId, groupKey, false, 0, totalChecked, null, null);
        }
        
        public static RuleExecutionResult noEvents(String ruleId, String groupKey) {
            return new RuleExecutionResult(ruleId, groupKey, false, 0, 0, null, null);
        }
        
        public static RuleExecutionResult error(String ruleId, String groupKey, String errorMessage) {
            return new RuleExecutionResult(ruleId, groupKey, false, 0, 0, null, errorMessage);
        }
        
        // Getters
        public String getRuleId() { return ruleId; }
        public String getGroupKey() { return groupKey; }
        public boolean isMatched() { return matched; }
        public int getMatchedCount() { return matchedCount; }
        public int getTotalChecked() { return totalChecked; }
        public EngineEventStreamEntity getFirstMatchedEvent() { return firstMatchedEvent; }
        public String getErrorMessage() { return errorMessage; }
        public long getExecutionTime() { return executionTime; }
        public boolean hasError() { return errorMessage != null; }
    }
}
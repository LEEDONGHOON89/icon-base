package com.itmasters.icon.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.adapter.out.persistence.entity.ContextRuleMappingEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectionContextEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectionEventEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.ContextRuleMappingRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.DetectionContextRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.DetectionEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 원자룰 vs 시나리오 감지 로직 테스트
 * 
 * 테스트 범위:
 * - 단일 원자룰 탐지 (단순 룰 매칭)
 * - 시나리오 감지 (복합 룰 매칭 - AND 조합)
 * - 컨텍스트 생성 및 관리
 * - 위험도 평가 알고리즘
 * - 시나리오 단계별 진행
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("원자룰 vs 시나리오 감지 로직 테스트")
class ScenarioDetectionLogicTest {

    @Mock
    private DetectionContextRepository contextRepository;

    @Mock
    private DetectionEventRepository eventRepository;

    @Mock
    private ContextRuleMappingRepository mappingRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ScenarioEngineService scenarioEngineService;

    private String testCorrelationKey;
    private String testProfileId;
    private Map<String, Object> testMatchedData;
    private Long testExecDsMpId;

    @BeforeEach
    void setUp() {
        testCorrelationKey = "CUSTOMER_12345";
        testProfileId = "PROFILE_01";
        testExecDsMpId = 100L;
        
        testMatchedData = new HashMap<>();
        testMatchedData.put("user_id", "12345");
        testMatchedData.put("transaction_amount", 50000);
        testMatchedData.put("transaction_time", "14:30:00");
        testMatchedData.put("location", "Seoul");
    }

    @Test
    @DisplayName("단일 원자룰 탐지 - 첫 번째 룰 매칭")
    void testSingleAtomicRuleDetection_FirstRule() throws Exception {
        // given
        String ruleId = "RULE_LOGIN_01";
        
        // 기존 컨텍스트 없음
        when(contextRepository.findActiveByCorrelationKey(testCorrelationKey))
            .thenReturn(Optional.empty());
        
        // 새 컨텍스트 생성
        DetectionContextEntity newContext = createMockContext(1L);
        when(contextRepository.save(any(DetectionContextEntity.class)))
            .thenReturn(newContext);
        
        // 이벤트 생성
        DetectionEventEntity mockEvent = createMockEvent(1L, 1L, ruleId);
        when(eventRepository.save(any(DetectionEventEntity.class)))
            .thenReturn(mockEvent);
        
        // JSON 직렬화
        when(objectMapper.writeValueAsString(testMatchedData))
            .thenReturn("{\"user_id\":\"12345\",\"amount\":50000}");
        
        // 매핑 조회 (기존 매핑 없음)
        when(mappingRepository.findByDetectionContextIdAndRuleId(1L, ruleId))
            .thenReturn(Optional.empty());
        
        // 매핑 저장
        when(mappingRepository.save(any(ContextRuleMappingEntity.class)))
            .thenReturn(createMockMapping(1L, 1L, ruleId));
        
        // 룰 개수 조회 (1개)
        when(mappingRepository.findByDetectionContextId(1L))
            .thenReturn(List.of(createMockMapping(1L, 1L, ruleId)));

        // when
        DetectionContextEntity result = scenarioEngineService.processDetectionEvent(
            testCorrelationKey, testProfileId, ruleId, testMatchedData, testExecDsMpId
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDetectionContextId()).isEqualTo(1L);
        assertThat(result.getEventCount()).isEqualTo(1);
        assertThat(result.getRuleMatchCount()).isEqualTo(1);
        assertThat(result.getRiskLevel()).isEqualTo("LOW"); // 단일 룰은 LOW 레벨
        
        // 컨텍스트 생성 확인
        verify(contextRepository).save(any(DetectionContextEntity.class));
        // 이벤트 생성 확인
        verify(eventRepository).save(any(DetectionEventEntity.class));
        // 매핑 생성 확인
        verify(mappingRepository).save(any(ContextRuleMappingEntity.class));
    }

    @Test
    @DisplayName("시나리오 감지 - 두 번째 룰 매칭으로 시나리오 완성")
    void testScenarioDetection_SecondRuleMatching() throws Exception {
        // given
        String firstRuleId = "RULE_LOGIN_01";
        String secondRuleId = "RULE_ATM_01";
        
        // 기존 컨텍스트 있음 (첫 번째 룰 이미 매칭됨)
        DetectionContextEntity existingContext = createMockContext(1L);
        existingContext.setEventCount(1);
        existingContext.setRuleMatchCount(1);
        existingContext.setRiskLevel("LOW");
        when(contextRepository.findActiveByCorrelationKey(testCorrelationKey))
            .thenReturn(Optional.of(existingContext));
        
        // 두 번째 이벤트 생성
        DetectionEventEntity mockEvent = createMockEvent(2L, 1L, secondRuleId);
        when(eventRepository.save(any(DetectionEventEntity.class)))
            .thenReturn(mockEvent);
        
        // JSON 직렬화
        when(objectMapper.writeValueAsString(testMatchedData))
            .thenReturn("{\"user_id\":\"12345\",\"atm_location\":\"Seoul\"}");
        
        // 매핑 조회 (새로운 룰이므로 기존 매핑 없음)
        when(mappingRepository.findByDetectionContextIdAndRuleId(1L, secondRuleId))
            .thenReturn(Optional.empty());
        
        // 새 매핑 저장
        when(mappingRepository.save(any(ContextRuleMappingEntity.class)))
            .thenReturn(createMockMapping(2L, 1L, secondRuleId));
        
        // 룰 개수 조회 (2개 - 시나리오 완성)
        List<ContextRuleMappingEntity> mappings = Arrays.asList(
            createMockMapping(1L, 1L, firstRuleId),
            createMockMapping(2L, 1L, secondRuleId)
        );
        when(mappingRepository.findByDetectionContextId(1L))
            .thenReturn(mappings);
        
        // 업데이트된 컨텍스트 저장
        DetectionContextEntity updatedContext = createMockContext(1L);
        updatedContext.setEventCount(2);
        updatedContext.setRuleMatchCount(2);
        updatedContext.setRiskLevel("MEDIUM"); // 2개 룰 매칭으로 MEDIUM 레벨
        updatedContext.setAnomalyScore(50.0); // 2룰 * 15 + 2이벤트 * 10 = 50점
        when(contextRepository.save(any(DetectionContextEntity.class)))
            .thenReturn(updatedContext);

        // when
        DetectionContextEntity result = scenarioEngineService.processDetectionEvent(
            testCorrelationKey, testProfileId, secondRuleId, testMatchedData, testExecDsMpId
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getEventCount()).isEqualTo(2);
        assertThat(result.getRuleMatchCount()).isEqualTo(2);
        assertThat(result.getRiskLevel()).isEqualTo("MEDIUM"); // 시나리오 감지로 위험도 상승
        assertThat(result.getAnomalyScore()).isEqualTo(50.0);
        
        // 이벤트 생성 확인 (컨텍스트는 기존 것 사용)
        verify(eventRepository).save(any(DetectionEventEntity.class));
        verify(mappingRepository).save(any(ContextRuleMappingEntity.class));
        verify(contextRepository, times(2)).save(any(DetectionContextEntity.class)); // 상태 업데이트 + 위험도 평가
    }

    @Test
    @DisplayName("시나리오 감지 - 3개 룰 매칭으로 HIGH 위험도")
    void testScenarioDetection_ThreeRulesHighRisk() throws Exception {
        // given
        String thirdRuleId = "RULE_FINANCIAL_01";
        
        // 기존 컨텍스트 (2개 룰 이미 매칭됨)
        DetectionContextEntity existingContext = createMockContext(1L);
        existingContext.setEventCount(2);
        existingContext.setRuleMatchCount(2);
        existingContext.setRiskLevel("MEDIUM");
        when(contextRepository.findActiveByCorrelationKey(testCorrelationKey))
            .thenReturn(Optional.of(existingContext));
        
        // 세 번째 이벤트 생성
        DetectionEventEntity mockEvent = createMockEvent(3L, 1L, thirdRuleId);
        when(eventRepository.save(any(DetectionEventEntity.class)))
            .thenReturn(mockEvent);
        
        // JSON 직렬화
        when(objectMapper.writeValueAsString(testMatchedData))
            .thenReturn("{\"user_id\":\"12345\",\"transaction_amount\":1000000}");
        
        // 매핑 조회
        when(mappingRepository.findByDetectionContextIdAndRuleId(1L, thirdRuleId))
            .thenReturn(Optional.empty());
        
        // 룰 개수 조회 (3개)
        List<ContextRuleMappingEntity> mappings = Arrays.asList(
            createMockMapping(1L, 1L, "RULE_LOGIN_01"),
            createMockMapping(2L, 1L, "RULE_ATM_01"),
            createMockMapping(3L, 1L, thirdRuleId)
        );
        when(mappingRepository.findByDetectionContextId(1L))
            .thenReturn(mappings);
        
        // 업데이트된 컨텍스트
        DetectionContextEntity updatedContext = createMockContext(1L);
        updatedContext.setEventCount(3);
        updatedContext.setRuleMatchCount(3);
        updatedContext.setRiskLevel("HIGH"); // 3룰 매칭으로 HIGH 레벨
        updatedContext.setAnomalyScore(75.0); // 3룰 * 15 + 3이벤트 * 10 = 75점
        when(contextRepository.save(any(DetectionContextEntity.class)))
            .thenReturn(updatedContext);

        // when
        DetectionContextEntity result = scenarioEngineService.processDetectionEvent(
            testCorrelationKey, testProfileId, thirdRuleId, testMatchedData, testExecDsMpId
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getEventCount()).isEqualTo(3);
        assertThat(result.getRuleMatchCount()).isEqualTo(3);
        assertThat(result.getRiskLevel()).isEqualTo("HIGH");
        assertThat(result.getAnomalyScore()).isEqualTo(75.0);
    }

    @Test
    @DisplayName("시나리오 감지 - 4개 룰 매칭으로 CRITICAL 위험도 및 알림 발송")
    void testScenarioDetection_FourRulesCriticalRisk() throws Exception {
        // given
        String fourthRuleId = "RULE_DEVICE_01";
        
        // 기존 컨텍스트 (3개 룰 이미 매칭됨)
        DetectionContextEntity existingContext = createMockContext(1L);
        existingContext.setEventCount(3);
        existingContext.setRuleMatchCount(3);
        existingContext.setRiskLevel("HIGH");
        when(contextRepository.findActiveByCorrelationKey(testCorrelationKey))
            .thenReturn(Optional.of(existingContext));
        
        // 네 번째 이벤트 생성
        DetectionEventEntity mockEvent = createMockEvent(4L, 1L, fourthRuleId);
        when(eventRepository.save(any(DetectionEventEntity.class)))
            .thenReturn(mockEvent);
        
        // JSON 직렬화
        when(objectMapper.writeValueAsString(testMatchedData))
            .thenReturn("{\"user_id\":\"12345\",\"device_id\":\"unknown_device\"}");
        
        // 룰 개수 조회 (4개)
        List<ContextRuleMappingEntity> mappings = Arrays.asList(
            createMockMapping(1L, 1L, "RULE_LOGIN_01"),
            createMockMapping(2L, 1L, "RULE_ATM_01"),
            createMockMapping(3L, 1L, "RULE_FINANCIAL_01"),
            createMockMapping(4L, 1L, fourthRuleId)
        );
        when(mappingRepository.findByDetectionContextId(1L))
            .thenReturn(mappings);
        
        // 업데이트된 컨텍스트 (CRITICAL + 트리거 시간 설정)
        DetectionContextEntity updatedContext = createMockContext(1L);
        updatedContext.setEventCount(4);
        updatedContext.setRuleMatchCount(4);
        updatedContext.setRiskLevel("CRITICAL"); // 4룰 매칭으로 CRITICAL 레벨
        updatedContext.setAnomalyScore(100.0); // 4룰 * 15 + 4이벤트 * 10 = 100점 (최대)
        updatedContext.setTriggeredAt(LocalDateTime.now()); // CRITICAL 트리거 시간
        when(contextRepository.save(any(DetectionContextEntity.class)))
            .thenReturn(updatedContext);

        // when
        DetectionContextEntity result = scenarioEngineService.processDetectionEvent(
            testCorrelationKey, testProfileId, fourthRuleId, testMatchedData, testExecDsMpId
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getEventCount()).isEqualTo(4);
        assertThat(result.getRuleMatchCount()).isEqualTo(4);
        assertThat(result.getRiskLevel()).isEqualTo("CRITICAL");
        assertThat(result.getAnomalyScore()).isEqualTo(100.0);
        assertThat(result.getTriggeredAt()).isNotNull(); // CRITICAL 알림 트리거 시간 설정됨
    }

    @Test
    @DisplayName("기존 룰 재매칭 - 이벤트는 증가하지만 룰 수는 동일")
    void testExistingRuleRematching() throws Exception {
        // given
        String existingRuleId = "RULE_LOGIN_01";
        
        // 기존 컨텍스트
        DetectionContextEntity existingContext = createMockContext(1L);
        existingContext.setEventCount(1);
        existingContext.setRuleMatchCount(1);
        when(contextRepository.findActiveByCorrelationKey(testCorrelationKey))
            .thenReturn(Optional.of(existingContext));
        
        // 이벤트 생성
        DetectionEventEntity mockEvent = createMockEvent(2L, 1L, existingRuleId);
        when(eventRepository.save(any(DetectionEventEntity.class)))
            .thenReturn(mockEvent);
        
        // JSON 직렬화
        when(objectMapper.writeValueAsString(testMatchedData))
            .thenReturn("{\"user_id\":\"12345\",\"second_login\":true}");
        
        // 기존 매핑 찾음
        ContextRuleMappingEntity existingMapping = createMockMapping(1L, 1L, existingRuleId);
        when(mappingRepository.findByDetectionContextIdAndRuleId(1L, existingRuleId))
            .thenReturn(Optional.of(existingMapping));
        
        // 매핑 업데이트
        when(mappingRepository.save(any(ContextRuleMappingEntity.class)))
            .thenReturn(existingMapping);
        
        // 룰 개수 조회 (여전히 1개)
        when(mappingRepository.findByDetectionContextId(1L))
            .thenReturn(List.of(existingMapping));

        // when
        DetectionContextEntity result = scenarioEngineService.processDetectionEvent(
            testCorrelationKey, testProfileId, existingRuleId, testMatchedData, testExecDsMpId
        );

        // then
        assertThat(result.getEventCount()).isEqualTo(2); // 이벤트 수는 증가
        assertThat(result.getRuleMatchCount()).isEqualTo(1); // 룰 수는 동일
        
        // 기존 매핑 업데이트 확인 (새로 생성하지 않음)
        verify(mappingRepository).save(existingMapping);
    }

    @Test
    @DisplayName("위험도 계산 로직 - 이상 점수 기반 위험도 분류")
    void testRiskLevelCalculation() throws Exception {
        // 이 테스트는 ScenarioEngineService.calculateAnomalyScore와 evaluateRiskLevel 메서드를 간접 테스트
        
        // Case 1: 1 룰, 1 이벤트 = 25점 = LOW
        testRiskLevelScenario(1, 1, "LOW", 25.0);
        
        // Case 2: 2 룰, 2 이벤트 = 50점 = MEDIUM
        testRiskLevelScenario(2, 2, "MEDIUM", 50.0);
        
        // Case 3: 3 룰, 3 이벤트 = 75점 = HIGH  
        testRiskLevelScenario(3, 3, "HIGH", 75.0);
        
        // Case 4: 4+ 룰 또는 5+ 이벤트 = 100점 = CRITICAL
        testRiskLevelScenario(4, 4, "CRITICAL", 100.0);
    }

    private void testRiskLevelScenario(int ruleCount, int eventCount, 
                                     String expectedRiskLevel, double expectedScore) throws Exception {
        // given
        reset(contextRepository, eventRepository, mappingRepository, objectMapper);
        
        String ruleId = "RULE_TEST_" + ruleCount;
        
        DetectionContextEntity existingContext = createMockContext(1L);
        existingContext.setEventCount(eventCount - 1);
        existingContext.setRuleMatchCount(ruleCount - 1);
        when(contextRepository.findActiveByCorrelationKey(testCorrelationKey))
            .thenReturn(Optional.of(existingContext));
        
        DetectionEventEntity mockEvent = createMockEvent((long) eventCount, 1L, ruleId);
        when(eventRepository.save(any(DetectionEventEntity.class)))
            .thenReturn(mockEvent);
        
        when(objectMapper.writeValueAsString(testMatchedData))
            .thenReturn("{}");
        
        when(mappingRepository.findByDetectionContextIdAndRuleId(1L, ruleId))
            .thenReturn(Optional.empty());
        
        // ruleCount 개수만큼의 매핑 생성
        List<ContextRuleMappingEntity> mappings = new ArrayList<>();
        for (int i = 1; i <= ruleCount; i++) {
            mappings.add(createMockMapping((long) i, 1L, "RULE_TEST_" + i));
        }
        when(mappingRepository.findByDetectionContextId(1L))
            .thenReturn(mappings);
        
        DetectionContextEntity updatedContext = createMockContext(1L);
        updatedContext.setEventCount(eventCount);
        updatedContext.setRuleMatchCount(ruleCount);
        updatedContext.setRiskLevel(expectedRiskLevel);
        updatedContext.setAnomalyScore(expectedScore);
        if ("CRITICAL".equals(expectedRiskLevel)) {
            updatedContext.setTriggeredAt(LocalDateTime.now());
        }
        when(contextRepository.save(any(DetectionContextEntity.class)))
            .thenReturn(updatedContext);

        // when
        DetectionContextEntity result = scenarioEngineService.processDetectionEvent(
            testCorrelationKey, testProfileId, ruleId, testMatchedData, testExecDsMpId
        );

        // then
        assertThat(result.getRiskLevel()).isEqualTo(expectedRiskLevel);
        assertThat(result.getAnomalyScore()).isEqualTo(expectedScore);
        if ("CRITICAL".equals(expectedRiskLevel)) {
            assertThat(result.getTriggeredAt()).isNotNull();
        }
    }

    @Test
    @DisplayName("컨텍스트 만료 처리")
    void testContextExpiration() {
        // given
        LocalDateTime expiredTime = LocalDateTime.now().minusHours(25); // 24시간 + 1시간
        DetectionContextEntity expiredContext1 = createMockContext(1L);
        expiredContext1.setWindowEnd(expiredTime);
        expiredContext1.setStatus("ACTIVE");
        
        DetectionContextEntity expiredContext2 = createMockContext(2L);
        expiredContext2.setWindowEnd(expiredTime);
        expiredContext2.setStatus("ACTIVE");
        
        List<DetectionContextEntity> expiredContexts = Arrays.asList(expiredContext1, expiredContext2);
        when(contextRepository.findExpiredContexts(any(LocalDateTime.class)))
            .thenReturn(expiredContexts);

        // when
        scenarioEngineService.cleanupExpiredContexts();

        // then
        verify(contextRepository).findExpiredContexts(any(LocalDateTime.class));
        verify(contextRepository, times(2)).save(any(DetectionContextEntity.class));
        
        // 상태가 EXPIRED로 변경되었는지 확인
        assertThat(expiredContext1.getStatus()).isEqualTo("EXPIRED");
        assertThat(expiredContext2.getStatus()).isEqualTo("EXPIRED");
    }

    // Helper Methods
    
    private DetectionContextEntity createMockContext(Long contextId) {
        return DetectionContextEntity.builder()
            .detectionContextId(contextId)
            .correlationKey(testCorrelationKey)
            .keyType("CUSTOMER")
            .profileId(testProfileId)
            .status("ACTIVE")
            .eventCount(0)
            .ruleMatchCount(0)
            .windowStart(LocalDateTime.now())
            .windowEnd(LocalDateTime.now().plusHours(24))
            .windowDurationMinutes(1440)
            .riskLevel("LOW")
            .contextData(new HashMap<>())
            .createdAt(LocalDateTime.now())
            .build();
    }
    
    private DetectionEventEntity createMockEvent(Long eventId, Long contextId, String ruleId) {
        return DetectionEventEntity.builder()
            .eventId(eventId)
            .contextId(contextId)
            .ruleId(ruleId)
            .eventType("RULE_MATCH")
            .eventTimestamp(LocalDateTime.now())
            .eventData("{}")
            .execDsMpId(testExecDsMpId)
            .severity("MEDIUM")
            .category("DETECTION")
            .regDt(LocalDateTime.now())
            .build();
    }
    
    private ContextRuleMappingEntity createMockMapping(Long mappingId, Long contextId, String ruleId) {
        return ContextRuleMappingEntity.builder()
            .contextRuleMappingId(mappingId)
            .detectionContextId(contextId)
            .ruleId(ruleId)
            .detectionTimestamp(LocalDateTime.now())
            .sequenceNumber(1)
            .severity("MEDIUM")
            .isTrigger(false)
            .alertSent(false)
            .regDt(LocalDateTime.now())
            .build();
    }
}
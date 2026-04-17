package com.itmasters.icon.engine.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.adapter.out.persistence.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 시나리오 통합 테스트 베이스 클래스
 * 
 * 모든 시나리오 테스트의 공통 기능 제공:
 * - DB 접근 (JdbcTemplate)
 * - 공통 Repository
 * - 헬퍼 메서드
 * 
 * 사용법:
 * 1. 이 클래스를 extends
 * 2. @SpringBootTest와 @ActiveProfiles는 자동 상속됨
 * 3. 필요한 추가 Repository만 @Autowired
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public abstract class ScenarioTestBase {
    
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    // Repository는 필요시 테스트 클래스에서 직접 @Autowired
    // 대부분의 경우 JdbcTemplate로 충분
    
    /**
     * 테스트 시작 시간 (기준 시각)
     */
    protected LocalDateTime testBaseTime = LocalDateTime.of(2025, 8, 20, 10, 0, 0);
    
    /**
     * 테스트 고객 ID 생성
     */
    protected String getTestCustomerId(int index) {
        return String.format("CUS_TEST_%03d", index);
    }
    
    /**
     * 테스트 디바이스 UUID 생성
     */
    protected String getTestDeviceUuid(int index) {
        return String.format("TEST_DEVICE_%03d", index);
    }
    
    /**
     * LOGIN 이벤트 데이터 생성
     */
    protected Map<String, Object> createLoginEvent(String customerId, String deviceUuid, LocalDateTime loginTime) {
        Map<String, Object> event = new HashMap<>();
        event.put("customer_id", customerId);
        event.put("device_uuid", deviceUuid);
        event.put("last_login_dt", loginTime.toString());
        event.put("login_method", "PASSWORD");
        event.put("action_result", "SUCCESS");
        event.put("source_ip", "192.168.1.100");
        event.put("access_country", "KR");
        event.put("os_type", "Windows");
        event.put("browser_type", "Chrome");
        return event;
    }
    
    /**
     * FINANCIAL_TRANSACTION 이체 이벤트 생성
     */
    protected Map<String, Object> createTransferEvent(
            String customerId,
            String deviceUuid,
            LocalDateTime transactionTime,
            long amount,
            boolean isThirdParty) {
        // 기본 수취계좌로 오버로드
        return createTransferEvent(customerId, deviceUuid, transactionTime, amount, isThirdParty,
            isThirdParty ? "타인계좌" : "내계좌");
    }

    /**
     * FINANCIAL_TRANSACTION 이체 이벤트 생성 (수취계좌 지정)
     */
    protected Map<String, Object> createTransferEvent(
            String customerId,
            String deviceUuid,
            LocalDateTime transactionTime,
            long amount,
            boolean isThirdParty,
            String receiverAccount) {

        Map<String, Object> event = new HashMap<>();
        event.put("customer_id", customerId);
        event.put("device_uuid", deviceUuid);
        event.put("transaction_datetime", transactionTime.toString());
        event.put("transaction_type", "이체");
        event.put("transaction_amount", amount);
        event.put("is_third_party", isThirdParty);
        event.put("sender_account", "내계좌");
        event.put("receiver_account", receiverAccount);
        return event;
    }
    
    /**
     * 위험 국가 LOGIN 이벤트 생성
     */
    protected Map<String, Object> createRiskCountryLoginEvent(
            String customerId,
            String deviceUuid,
            LocalDateTime loginTime,
            String countryCode) {

        Map<String, Object> event = createLoginEvent(customerId, deviceUuid, loginTime);
        event.put("access_country", countryCode); // RU, CN 등
        return event;
    }

    /**
     * Event Stream에 이벤트 저장
     */
    protected void saveEventStream(String groupKey, Map<String, Object> eventData,
                                   LocalDateTime eventDt, Long mappedStorageId) {
        try {
            String eventDataJson = objectMapper.writeValueAsString(eventData);
            // H2 호환: ::jsonb 캐스트 제거 (H2는 VARCHAR로 저장)
            String sql = "INSERT INTO event_stream (group_key, event_data, event_dt, mapped_storage_id) " +
                         "VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(sql, groupKey, eventDataJson, eventDt, mappedStorageId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save event stream", e);
        }
    }
    
    /**
     * DB에서 시나리오 존재 여부 확인
     */
    protected boolean scenarioExists(String scenarioId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM scenarios WHERE scenario_id = ? AND is_active = true",
            Integer.class,
            scenarioId
        );
        return count != null && count > 0;
    }
    
    /**
     * DB에서 Aggregate 존재 여부 확인
     */
    protected boolean aggregateExists(String aggregateId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM aggregates WHERE aggregate_id = ? AND is_active = true",
            Integer.class,
            aggregateId
        );
        return count != null && count > 0;
    }
    
    /**
     * DB에서 Rule 존재 여부 확인
     */
    protected boolean ruleExists(String ruleId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM rules WHERE rule_id = ? AND is_active = true",
            Integer.class,
            ruleId
        );
        return count != null && count > 0;
    }
    
    /**
     * 테스트 데이터 정리 (트랜잭션 롤백 대신 수동 정리 시 사용)
     */
    protected void cleanupTestData(String customerId) {
        log.info("테스트 데이터 정리: customerId={}", customerId);
        
        // event_stream 삭제
        jdbcTemplate.update(
            "DELETE FROM event_stream WHERE event_data->>'customer_id' = ?",
            customerId
        );
        
        // detect_aggregates 삭제
        jdbcTemplate.update(
            "DELETE FROM detect_aggregates WHERE group_key = ?",
            customerId
        );
        
        // detect_scenario_results 삭제
        jdbcTemplate.update(
            "DELETE FROM detect_scenario_results WHERE correlation_key = ?",
            customerId
        );
    }
    
    /**
     * 로그 헬퍼: 섹션 구분선
     */
    protected void logSection(String title) {
        log.info("");
        log.info("========================================");
        log.info("  {}", title);
        log.info("========================================");
    }
    
    /**
     * 로그 헬퍼: 테스트 케이스 시작
     */
    protected void logTestStart(String testName) {
        log.info("");
        log.info("╔════════════════════════════════════════╗");
        log.info("║  TEST: {}", String.format("%-32s", testName) + "║");
        log.info("╚════════════════════════════════════════╝");
    }
    
    /**
     * 로그 헬퍼: 테스트 결과
     */
    protected void logTestResult(boolean success, String message) {
        String status = success ? "✅ SUCCESS" : "❌ FAILED";
        log.info("{}: {}", status, message);
    }
}

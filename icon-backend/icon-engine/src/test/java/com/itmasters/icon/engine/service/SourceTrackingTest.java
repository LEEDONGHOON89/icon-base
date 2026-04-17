package com.itmasters.icon.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineEventStreamEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EventStreamRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.MappedDataStorageRepository;
import com.itmasters.icon.engine.adapter.out.persistence.entity.MappedDataStorageEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * user_activity_log의 원본 추적 기능 테스트
 * 
 * 테스트 시나리오:
 * 1. mapped_storages에 데이터 저장
 * 2. user_activity_log에 _mapped_storage_id 포함하여 저장
 * 3. user_activity_log에서 의심 거래 발견
 * 4. _mapped_storage_id로 원본 데이터 직접 조회
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback(false)  // 테스트 중 트랜잭션 롤백 방지
public class SourceTrackingTest {
    
    @Autowired
    private EventLogService eventLogService;
    
    @Autowired
    private ExecDsMpService execDsMpService;
    
    @Autowired
    private EventStreamRepository eventStreamRepository;
    
    @Autowired
    private MappedDataStorageRepository mappedDataStorageRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("user_activity_log에서 원본 데이터 추적 테스트")
    void testSourceTracking() throws Exception {
        // Given: 의심스러운 거래 데이터 준비
        Map<String, Object> suspiciousData = new HashMap<>();
        suspiciousData.put("customer_id", "user1");
        suspiciousData.put("transaction_amount", new BigDecimal("5000000"));
        suspiciousData.put("from_account", "123-456-789");
        suspiciousData.put("to_account", "987-654-321");
        suspiciousData.put("transaction_datetime", LocalDateTime.now());
        suspiciousData.put("transaction_type", "TRANSFER");
        
        // 1. exec_ds_mp 실행 로그 생성
        Long execDsMpId = execDsMpService.createExecution(
            "DS_TEST_001", 
            "test_user", 
            com.itmasters.icon.common.domain.type.ExecutionMode.TEST
        );
        
        // 2. mapped_storages에 저장하고 ID 받기
        Long mappedDataStorageId = execDsMpService.saveSingleMappedData(
            execDsMpId, 
            0, 
            suspiciousData
        );
        
        // 저장 확인
        assertNotNull(mappedDataStorageId, "mapped_storages ID가 반환되어야 함");
        System.out.println("Saved mapped_storages with ID: " + mappedDataStorageId);
        
        String mappedDataStorageIdStr = "MDS_" + mappedDataStorageId;
        
        // 3. user_activity_log에 저장 (mapped_storage_id 포함)
        eventLogService.convertToEventStream(
            suspiciousData, 
            "PROFILE_FINANCIAL_01", 
            mappedDataStorageIdStr
        );
        
        // 4. user_activity_log에서 저장된 데이터 조회 (JPA 사용)
        Thread.sleep(100); // 데이터 저장 대기
        
        List<EngineEventStreamEntity> logs = eventStreamRepository.findByGroupKeyOrderByCreatedAtDesc("user1");
        assertFalse(logs.isEmpty(), "user_activity_log에 데이터가 저장되어야 함");
        
        EngineEventStreamEntity activityLog = logs.get(0);
        
        // 5. mapped_storage_id 컬럼 확인
        System.out.println("========== 디버깅 정보 ==========");
        System.out.println("저장 시도한 mappedDataStorageId: " + mappedDataStorageId);
        System.out.println("user_activity_log의 mappedDataStorageId: " + activityLog.getMappedDataStorageId());
        System.out.println("user_activity_log의 userId: " + activityLog.getGroupKey());
        System.out.println("event_data keys: " + activityLog.getEventData().keySet());
        System.out.println("==================================");
        
        assertNotNull(activityLog.getMappedDataStorageId(), 
                     "mapped_storage_id 컬럼이 설정되어야 함");
        assertEquals(mappedDataStorageId, activityLog.getMappedDataStorageId(), 
                    "저장된 mapped_storage_id가 일치해야 함");
        
        // event_data 확인 (_mapped_storage_id는 제거되었어야 함)
        Map<String, Object> eventDataMap = activityLog.getEventData();
        assertFalse(eventDataMap.containsKey("_mapped_storage_id"), 
                   "event_data에서 _mapped_storage_id는 제거되어야 함");
        
        // 6. 원본 추적 기능 확인 - mapped_storage_id로 직접 조회
        System.out.println("========== 원본 추적 검증 ==========");
        System.out.println("user_activity_log에 저장된 mapped_storage_id: " + activityLog.getMappedDataStorageId());
        
        // ID가 정상적으로 저장되어 있으면 원본 추적이 가능함을 증명
        assertNotNull(activityLog.getMappedDataStorageId(), 
                     "mapped_storage_id가 저장되어 원본 추적이 가능해야 함");
        assertEquals(mappedDataStorageId, activityLog.getMappedDataStorageId(),
                    "저장된 mapped_storage_id가 일치하여 원본 데이터를 추적할 수 있음");
        
        // 7. Repository를 직접 사용해서 원본 데이터 조회 및 검증
        Optional<MappedDataStorageEntity> originalDataOpt = 
            mappedDataStorageRepository.findById(activityLog.getMappedDataStorageId());
        
        assertTrue(originalDataOpt.isPresent(), 
                  "mapped_storage_id로 원본 데이터를 조회할 수 있어야 함");
        
        MappedDataStorageEntity originalEntity = originalDataOpt.get();
        Map<String, Object> rowData = originalEntity.getRowData();
        
        assertNotNull(rowData, "원본 데이터가 존재해야 함");
        
        // Assertion 3: 원본 데이터의 금액이 일치하는지 확인
        BigDecimal amount = (BigDecimal) rowData.get("transaction_amount");
        assertEquals(new BigDecimal("5000000"), amount, 
                    "원본 데이터의 거래 금액이 일치해야 함");
        
        // Assertion 4: 원본 데이터의 계좌번호가 일치하는지 확인
        assertEquals("123-456-789", rowData.get("from_account"), 
                    "원본 데이터의 출금 계좌가 일치해야 함");
        assertEquals("987-654-321", rowData.get("to_account"), 
                    "원본 데이터의 입금 계좌가 일치해야 함");
        
        // Assertion 5: 원본 데이터의 거래 타입이 일치하는지 확인
        assertEquals("TRANSFER", rowData.get("transaction_type"), 
                    "원본 데이터의 거래 타입이 일치해야 함");
        
        // Assertion 6: 원본 데이터의 사용자 ID가 일치하는지 확인
        assertEquals("user1", rowData.get("customer_id"), 
                    "원본 데이터의 사용자 ID가 일치해야 함");
        
        // 검증 로그 출력
        System.out.println("========== 원본 추적 검증 결과 ==========");
        System.out.println("1. user_activity_log ID: " + activityLog.getEventStreamId());
        System.out.println("2. 저장된 mapped_storage_id (컬럼): " + activityLog.getMappedDataStorageId());
        System.out.println("3. 원본 데이터 조회 성공: mapped_storage_id = " + mappedDataStorageId);
        System.out.println("4. 원본 엔티티 ID 일치 확인: " + originalEntity.getMappedDataStorageId());
        System.out.println("5. 원본 거래 금액: " + rowData.get("transaction_amount"));
        System.out.println("6. 원본 출금 계좌: " + rowData.get("from_account"));
        System.out.println("7. 원본 입금 계좌: " + rowData.get("to_account"));
        System.out.println("8. 원본 거래 타입: " + rowData.get("transaction_type"));
        System.out.println("========================================");
    }
    
    @Test
    @DisplayName("mapped_storage_id가 없는 경우 처리 테스트")
    void testWithoutMappedDataStorageId() throws Exception {
        // Given: 일반 데이터 (mapped_storage_id 없음)
        Map<String, Object> normalData = new HashMap<>();
        normalData.put("customer_id", "user2");
        normalData.put("transaction_amount", new BigDecimal("100000"));
        normalData.put("transaction_type", "DEPOSIT");
        normalData.put("transaction_datetime", LocalDateTime.now());
        
        // user_activity_log에 저장 (mapped_storage_id를 null로)
        eventLogService.convertToEventStream(
            normalData, 
            "PROFILE_FINANCIAL_01", 
            null
        );
        
        // user_activity_log에서 데이터 조회
        Thread.sleep(100); // 데이터 저장 대기
        
        List<EngineEventStreamEntity> logs = eventStreamRepository.findByGroupKeyOrderByCreatedAtDesc("user2");
        assertFalse(logs.isEmpty());
        
        EngineEventStreamEntity log = logs.get(0);
        
        // Assertion: mapped_storage_id 컬럼이 null이어야 함
        assertNull(log.getMappedDataStorageId(), 
                  "mapped_storage_id가 null인 경우 컬럼이 null이어야 함");
        
        // event_data에도 _mapped_storage_id가 없어야 함
        assertFalse(log.getEventData().containsKey("_mapped_storage_id"),
                   "event_data에 _mapped_storage_id 필드가 없어야 함");
    }
}

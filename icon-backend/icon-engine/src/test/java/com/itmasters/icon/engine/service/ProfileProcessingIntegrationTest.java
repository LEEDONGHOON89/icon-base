package com.itmasters.icon.engine.service;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineEventStreamEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineProfileEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.MappedDataStorageEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EventStreamRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.MappedDataStorageRepository;
import com.itmasters.icon.engine.dto.DetectionContext;
import com.itmasters.icon.engine.dto.DetectionResult;
import com.itmasters.icon.engine.dto.DataProcessingResultDto;
import com.itmasters.icon.common.domain.type.ExecutionMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProfileProcessingService의 소스 트래킹 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ProfileProcessingIntegrationTest {

    @Autowired
    private ProfileProcessingService profileProcessingService;

    @Autowired
    private ExecDsMpService execDsMpService;

    @Autowired
    private EventStreamRepository eventStreamRepository;

    @Autowired
    private MappedDataStorageRepository mappedDataStorageRepository;

    @Test
    @DisplayName("ProfileProcessingService가 mapped_storage_id를 올바르게 저장하는지 테스트")
    void testProfileProcessingSavesSourceTracking() throws Exception {
        // Given: 테스트 데이터 준비
        String testProfileId = "PROFILE_INT_TEST";
        String testDataSourceId = "DS_INT_TEST";

        // 실행 로그 생성
        Long execDsMpId = execDsMpService.createExecution(
                testDataSourceId,
                "integration-test",
                ExecutionMode.TEST
        );

        // 테스트 데이터
        Map<String, Object> testData = new HashMap<>();
        testData.put("customer_id", "INT_TEST_USER");
        testData.put("transaction_amount", new BigDecimal("3000000"));
        testData.put("transaction_type", "TRANSFER");
        testData.put("from_account", "999-888-777");
        testData.put("to_account", "111-222-333");
        testData.put("transaction_datetime", LocalDateTime.now());

        List<Map<String, Object>> dataList = Arrays.asList(testData);
        DataProcessingResultDto processedData = DataProcessingResultDto.success(dataList, dataList, null);

        // 테스트용 프로파일 생성
        EngineProfileEntity profile = EngineProfileEntity.builder()
                .profileId(testProfileId)
                .profileName("Integration Test Profile")
                .dataSourceId(testDataSourceId)
                .groupKey("customer_id")
                .timestampKey("transaction_datetime")
                .isActive(true)
                .displayOrder(1)
                .build();

        // 컨텍스트 생성
        DetectionContext context = DetectionContext.builder()
                .executionId(UUID.randomUUID().toString())
                .dataSourceId(testDataSourceId)
                .profileId(testProfileId)
                .startTime(LocalDateTime.now())
                .executionMode(ExecutionMode.TEST)
                .executedBy("integration-test")
                .build();

        // When: ProfileProcessingService 실행
        System.out.println("========== ProfileProcessingService 실행 ==========");
        DetectionResult result = profileProcessingService.processProfile(
                execDsMpId,
                processedData,
                profile,
                context,
                "integration-test"
        );

        // 데이터 저장 대기
        Thread.sleep(200);

        // Then: user_activity_log 검증
        List<EngineEventStreamEntity> logs = eventStreamRepository
                .findByGroupKeyOrderByCreatedAtDesc("INT_TEST_USER");

        System.out.println("========== 검증 시작 ==========");
        System.out.println("user_activity_log 개수: " + logs.size());

        assertFalse(logs.isEmpty(), "user_activity_log가 생성되어야 함");

        EngineEventStreamEntity log = logs.get(0);
        System.out.println("User ID: " + log.getGroupKey());
        System.out.println("Mapped Data Storage ID: " + log.getMappedDataStorageId());

        // 핵심 검증: mapped_storage_id가 설정되었는지
        assertNotNull(log.getMappedDataStorageId(),
                "mapped_storage_id가 설정되어야 함");

        // 원본 데이터 추적 검증
        Optional<MappedDataStorageEntity> originalOpt =
                mappedDataStorageRepository.findById(log.getMappedDataStorageId());

        assertTrue(originalOpt.isPresent(),
                "mapped_storage_id로 원본 데이터를 조회할 수 있어야 함");

        MappedDataStorageEntity original = originalOpt.get();
        Map<String, Object> originalData = original.getRowData();

        // 원본 데이터 내용 검증
        assertEquals("INT_TEST_USER", originalData.get("customer_id"),
                "원본 데이터의 customer_id가 일치해야 함");
        assertEquals("999-888-777", originalData.get("from_account"),
                "원본 데이터의 from_account가 일치해야 함");
        assertEquals(new BigDecimal("3000000"), originalData.get("transaction_amount"),
                "원본 데이터의 amount가 일치해야 함");

        System.out.println("========== 소스 트래킹 검증 성공 ==========");
        System.out.println("원본 데이터 추적 성공!");
        System.out.println("mapped_storage_id: " + log.getMappedDataStorageId());
        System.out.println("원본 customer_id: " + originalData.get("customer_id"));
        System.out.println("원본 amount: " + originalData.get("transaction_amount"));
    }
}

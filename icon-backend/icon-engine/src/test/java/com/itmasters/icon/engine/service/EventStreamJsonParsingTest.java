package com.itmasters.icon.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineEventStreamEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EventStreamRepository;
import com.itmasters.icon.engine.dto.EventStreamResult;
import com.itmasters.icon.engine.dto.MappedDataRow;
import com.itmasters.icon.engine.dto.Step1Result;
import com.itmasters.icon.engine.dto.StreamKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * EventStream JSON 파싱 테스트
 * 
 * 테스트 범위:
 * - 순수 JSON 저장 방식 검증
 * - 다양한 JSON 형식 파싱 테스트
 * - 도메인별 fieldDatetime 기반 시간 추출
 * - timestamp 형식 변환 로직
 * - JSON 데이터 무결성 검증
 * - 중복 제거 로직
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventStream JSON 파싱 테스트")
class EventStreamJsonParsingTest {

    @Mock
    private EventStreamRepository eventStreamRepository;

    @InjectMocks
    private EventStreamService eventStreamService;

    private ObjectMapper objectMapper;
    private final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("LOGIN 도메인 JSON 파싱 - login_datetime 기반")
    void testLoginDomainJsonParsing() throws Exception {
        // given - LOGIN 도메인 JSON 데이터 (fieldDatetime: login_datetime)
        Map<String, Object> loginJsonData = createLoginJsonData();
        
        Step1Result step1Result = createStep1Result("DS_LOGIN_01", List.of(
            createMappedDataRow(1L, loginJsonData)
        ));
        
        Set<StreamKey> streamKeys = Set.of(
            new StreamKey("user_id", "login_datetime") // LOGIN 도메인의 fieldDatetime
        );
        
        // Mock 설정
        when(eventStreamRepository.findByMappedDataStorageId(1L)).thenReturn(Optional.empty());
        when(eventStreamRepository.existsByGroupKeyAndCreatedAt(anyString(), any(LocalDateTime.class)))
            .thenReturn(false);
        
        EngineEventStreamEntity savedEntity = createMockEventStreamEntity(1L, "USER_12345", loginJsonData);
        when(eventStreamRepository.save(any(EngineEventStreamEntity.class)))
            .thenReturn(savedEntity);

        // when
        EventStreamResult result = eventStreamService.saveEventStream(step1Result, streamKeys, "test-executor");

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalEvents()).isEqualTo(1);
        assertThat(result.getProcessedRows()).isEqualTo(1);
        
        // JSON 데이터 검증
        verify(eventStreamRepository).save(argThat(entity -> {
            Map<String, Object> eventData = entity.getEventData();
            
            // 원본 JSON 데이터가 그대로 보존되는지 확인
            assertThat(eventData.get("user_id")).isEqualTo("USER_12345");
            assertThat(eventData.get("login_datetime")).isEqualTo("2025-08-29 02:30:00");
            assertThat(eventData.get("ip_address")).isEqualTo("192.168.1.100");
            assertThat(eventData.get("login_result")).isEqualTo("SUCCESS");
            assertThat(eventData.get("device_info")).isInstanceOf(Map.class);
            
            // login_datetime가 timestamp로 정확히 파싱되었는지 확인
            assertThat(entity.getEventDt()).isEqualTo(LocalDateTime.parse("2025-08-29T02:30:00"));
            
            return true;
        }));
    }

    @Test
    @DisplayName("ATM 도메인 JSON 파싱 - transaction_datetime 기반")
    void testATMDomainJsonParsing() throws Exception {
        // given - ATM 도메인 JSON 데이터 (fieldDatetime: transaction_datetime)
        Map<String, Object> atmJsonData = createATMJsonData();
        
        Step1Result step1Result = createStep1Result("DS_ATM_01", List.of(
            createMappedDataRow(2L, atmJsonData)
        ));
        
        Set<StreamKey> streamKeys = Set.of(
            new StreamKey("customer_id", "transaction_datetime") // ATM 도메인의 fieldDatetime
        );
        
        // Mock 설정
        when(eventStreamRepository.findByMappedDataStorageId(2L)).thenReturn(Optional.empty());
        when(eventStreamRepository.existsByGroupKeyAndCreatedAt(anyString(), any(LocalDateTime.class)))
            .thenReturn(false);
        
        EngineEventStreamEntity savedEntity = createMockEventStreamEntity(2L, "CUSTOMER_67890", atmJsonData);
        when(eventStreamRepository.save(any(EngineEventStreamEntity.class)))
            .thenReturn(savedEntity);

        // when
        EventStreamResult result = eventStreamService.saveEventStream(step1Result, streamKeys, "test-executor");

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalEvents()).isEqualTo(1);
        
        // ATM JSON 데이터 검증
        verify(eventStreamRepository).save(argThat(entity -> {
            Map<String, Object> eventData = entity.getEventData();
            
            // ATM 도메인 특화 필드들 검증
            assertThat(eventData.get("customer_id")).isEqualTo("CUSTOMER_67890");
            assertThat(eventData.get("transaction_datetime")).isEqualTo("2025-08-29 15:45:00");
            assertThat(eventData.get("atm_id")).isEqualTo("ATM_SEOUL_001");
            assertThat(eventData.get("transaction_amount")).isEqualTo(2000000);
            assertThat(eventData.get("transaction_type")).isEqualTo("WITHDRAWAL");
            assertThat(eventData.get("card_info")).isInstanceOf(Map.class);
            
            // transaction_datetime가 정확히 파싱되었는지 확인
            assertThat(entity.getEventDt()).isEqualTo(LocalDateTime.parse("2025-08-29T15:45:00"));
            
            return true;
        }));
    }

    @Test
    @DisplayName("FINANCIAL_TRANSACTION 도메인 JSON 파싱 - transaction_date 기반")
    void testFinancialTransactionDomainJsonParsing() throws Exception {
        // given - FINANCIAL_TRANSACTION 도메인 JSON 데이터 (fieldDatetime: transaction_date)
        Map<String, Object> financialJsonData = createFinancialTransactionJsonData();
        
        Step1Result step1Result = createStep1Result("DS_FINANCIAL_01", List.of(
            createMappedDataRow(3L, financialJsonData)
        ));
        
        Set<StreamKey> streamKeys = Set.of(
            new StreamKey("customer_id", "transaction_date") // FINANCIAL_TRANSACTION 도메인의 fieldDatetime
        );
        
        // Mock 설정
        when(eventStreamRepository.findByMappedDataStorageId(3L)).thenReturn(Optional.empty());
        when(eventStreamRepository.existsByGroupKeyAndCreatedAt(anyString(), any(LocalDateTime.class)))
            .thenReturn(false);
        
        EngineEventStreamEntity savedEntity = createMockEventStreamEntity(3L, "CUSTOMER_11111", financialJsonData);
        when(eventStreamRepository.save(any(EngineEventStreamEntity.class)))
            .thenReturn(savedEntity);

        // when
        EventStreamResult result = eventStreamService.saveEventStream(step1Result, streamKeys, "test-executor");

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalEvents()).isEqualTo(1);
        
        // FINANCIAL_TRANSACTION JSON 데이터 검증
        verify(eventStreamRepository).save(argThat(entity -> {
            Map<String, Object> eventData = entity.getEventData();
            
            // 금융거래 도메인 특화 필드들 검증
            assertThat(eventData.get("customer_id")).isEqualTo("CUSTOMER_11111");
            assertThat(eventData.get("transaction_date")).isEqualTo("2025-08-29 09:20:00");
            assertThat(eventData.get("transaction_amount")).isEqualTo(500000);
            assertThat(eventData.get("transaction_currency")).isEqualTo("USD");
            assertThat(eventData.get("transaction_country")).isEqualTo("US");
            assertThat(eventData.get("merchant_info")).isInstanceOf(Map.class);
            
            // transaction_date가 정확히 파싱되었는지 확인
            assertThat(entity.getEventDt()).isEqualTo(LocalDateTime.parse("2025-08-29T09:20:00"));
            
            return true;
        }));
    }

    @Test
    @DisplayName("복잡한 중첩 JSON 구조 파싱")
    void testComplexNestedJsonParsing() throws Exception {
        // given - 복잡한 중첩 구조를 가진 JSON 데이터
        Map<String, Object> complexJsonData = createComplexNestedJsonData();
        
        Step1Result step1Result = createStep1Result("DS_COMPLEX_01", List.of(
            createMappedDataRow(4L, complexJsonData)
        ));
        
        Set<StreamKey> streamKeys = Set.of(
            new StreamKey("user_id", "event_datetime")
        );
        
        // Mock 설정
        when(eventStreamRepository.findByMappedDataStorageId(4L)).thenReturn(Optional.empty());
        when(eventStreamRepository.existsByGroupKeyAndCreatedAt(anyString(), any(LocalDateTime.class)))
            .thenReturn(false);
        
        EngineEventStreamEntity savedEntity = createMockEventStreamEntity(4L, "USER_COMPLEX", complexJsonData);
        when(eventStreamRepository.save(any(EngineEventStreamEntity.class)))
            .thenReturn(savedEntity);

        // when
        EventStreamResult result = eventStreamService.saveEventStream(step1Result, streamKeys, "test-executor");

        // then
        assertThat(result.isSuccess()).isTrue();
        
        // 복잡한 중첩 JSON 구조 검증
        verify(eventStreamRepository).save(argThat(entity -> {
            Map<String, Object> eventData = entity.getEventData();
            
            // 최상위 필드 검증
            assertThat(eventData.get("user_id")).isEqualTo("USER_COMPLEX");
            assertThat(eventData.get("event_type")).isEqualTo("DEVICE_SECURITY");
            
            // 중첩 객체 검증
            @SuppressWarnings("unchecked")
            Map<String, Object> deviceInfo = (Map<String, Object>) eventData.get("device_info");
            assertThat(deviceInfo.get("device_id")).isEqualTo("DEVICE_12345");
            assertThat(deviceInfo.get("os")).isEqualTo("Android");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> location = (Map<String, Object>) deviceInfo.get("location");
            assertThat(location.get("latitude")).isEqualTo(37.5665);
            assertThat(location.get("longitude")).isEqualTo(126.9780);
            
            // 배열 구조 검증
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> permissions = (List<Map<String, Object>>) eventData.get("permissions");
            assertThat(permissions).hasSize(2);
            assertThat(permissions.get(0).get("permission")).isEqualTo("CAMERA");
            assertThat(permissions.get(0).get("granted")).isEqualTo(true);
            
            return true;
        }));
    }

    @Test
    @DisplayName("다양한 timestamp 형식 파싱 테스트")
    void testVariousTimestampFormats() throws Exception {
        // given - 다양한 timestamp 형식을 가진 데이터들
        List<MappedDataRow> diverseDataRows = createDiverseTimestampData();
        
        Step1Result step1Result = createStep1Result("DS_TIMESTAMP_TEST", diverseDataRows);
        
        Set<StreamKey> streamKeys = Set.of(
            new StreamKey("user_id", "event_time") // 공통 timestamp 필드
        );
        
        // Mock 설정
        when(eventStreamRepository.findByMappedDataStorageId(anyLong())).thenReturn(Optional.empty());
        when(eventStreamRepository.existsByGroupKeyAndCreatedAt(anyString(), any(LocalDateTime.class)))
            .thenReturn(false);
        
        // 각각 다른 엔티티로 저장될 것
        when(eventStreamRepository.save(any(EngineEventStreamEntity.class)))
            .thenReturn(createMockEventStreamEntity(1L, "USER_001", new HashMap<>()));

        // when
        EventStreamResult result = eventStreamService.saveEventStream(step1Result, streamKeys, "test-executor");

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalEvents()).isEqualTo(4); // 4가지 다른 timestamp 형식
        
        // 각 timestamp 형식이 올바르게 파싱되었는지 검증
        verify(eventStreamRepository, times(4)).save(any(EngineEventStreamEntity.class));
    }

    @Test
    @DisplayName("JSON 데이터 무결성 검증 - 필수 필드 누락")
    void testJsonDataIntegrity_MissingFields() throws Exception {
        // given - 필수 필드가 누락된 JSON 데이터
        Map<String, Object> incompleteJsonData = new HashMap<>();
        incompleteJsonData.put("user_id", "USER_INCOMPLETE");
        // timestamp 필드 누락
        incompleteJsonData.put("action", "login");
        
        Step1Result step1Result = createStep1Result("DS_INCOMPLETE_01", List.of(
            createMappedDataRow(5L, incompleteJsonData)
        ));
        
        Set<StreamKey> streamKeys = Set.of(
            new StreamKey("user_id", "missing_timestamp") // 존재하지 않는 timestamp 필드
        );
        
        // Mock 설정
        when(eventStreamRepository.findByMappedDataStorageId(5L)).thenReturn(Optional.empty());

        // when
        EventStreamResult result = eventStreamService.saveEventStream(step1Result, streamKeys, "test-executor");

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalEvents()).isEqualTo(0); // timestamp 누락으로 저장되지 않음
        assertThat(result.getFilteredRows()).isEqualTo(1); // 필터링됨
        
        // 저장이 시도되지 않았는지 확인
        verify(eventStreamRepository, never()).save(any(EngineEventStreamEntity.class));
    }

    @Test
    @DisplayName("JSON 데이터 무결성 검증 - group_key 누락")
    void testJsonDataIntegrity_MissingGroupKey() throws Exception {
        // given - group_key가 누락된 JSON 데이터
        Map<String, Object> missingGroupKeyData = new HashMap<>();
        // user_id 필드 누락 (group_key 역할)
        missingGroupKeyData.put("event_time", "2025-08-29 10:00:00");
        missingGroupKeyData.put("action", "login");
        
        Step1Result step1Result = createStep1Result("DS_MISSING_GROUP_KEY", List.of(
            createMappedDataRow(6L, missingGroupKeyData)
        ));
        
        Set<StreamKey> streamKeys = Set.of(
            new StreamKey("user_id", "event_time") // user_id가 group_key 역할
        );
        
        // Mock 설정
        when(eventStreamRepository.findByMappedDataStorageId(6L)).thenReturn(Optional.empty());

        // when
        EventStreamResult result = eventStreamService.saveEventStream(step1Result, streamKeys, "test-executor");

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalEvents()).isEqualTo(0); // group_key 누락으로 저장되지 않음
        assertThat(result.getFilteredRows()).isEqualTo(1); // 필터링됨
        
        // 저장이 시도되지 않았는지 확인
        verify(eventStreamRepository, never()).save(any(EngineEventStreamEntity.class));
    }

    @Test
    @DisplayName("중복 제거 로직(v4) - 동일 mapped_storage_id")
    void testDeduplicationLogic_V4_MappedStorageId() throws Exception {
        // given - 동일한 mapped_storage_id를 가진 중복 데이터(원본 중복 처리)
        Map<String, Object> duplicateData1 = new HashMap<>();
        duplicateData1.put("user_id", "USER_DUPLICATE");
        duplicateData1.put("event_time", "2025-08-29 12:00:00");
        duplicateData1.put("action", "login_attempt_1");

        Map<String, Object> duplicateData2 = new HashMap<>();
        duplicateData2.put("user_id", "USER_DUPLICATE");
        duplicateData2.put("event_time", "2025-08-29 12:00:00");
        duplicateData2.put("action", "login_attempt_2");

        // 두 로우 모두 동일한 원본 mapped_storage_id로 가정(중복)
        Step1Result step1Result = createStep1Result("DS_DUPLICATE_TEST", List.of(
            createMappedDataRow(7L, duplicateData1),
            createMappedDataRow(7L, duplicateData2)
        ));

        Set<StreamKey> streamKeys = Set.of(
            new StreamKey("user_id", "event_time")
        );

        // Mock 설정 - 첫 번째 조회는 없음, 두 번째 조회는 이미 처리됨으로 간주
        when(eventStreamRepository.findByMappedDataStorageId(anyLong()))
            .thenReturn(Optional.empty())  // 첫 번째 원본 처리 전
            .thenReturn(Optional.of(createMockEventStreamEntity(1L, "USER_DUPLICATE", duplicateData1))); // 두 번째는 중복

        when(eventStreamRepository.save(any(EngineEventStreamEntity.class)))
            .thenReturn(createMockEventStreamEntity(7L, "USER_DUPLICATE", duplicateData1));

        // when
        EventStreamResult result = eventStreamService.saveEventStream(step1Result, streamKeys, "test-executor");

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalEvents()).isEqualTo(1); // 첫 번째만 저장됨
        assertThat(result.getProcessedRows()).isEqualTo(2); // 2개 처리됨
        assertThat(result.getDuplicateRows()).isEqualTo(1);

        // 첫 번째만 저장되고 두 번째는 원본 중복으로 제외
        verify(eventStreamRepository, times(1)).save(any(EngineEventStreamEntity.class));
    }

    @Test
    @DisplayName("다중 StreamKey 존재 시 1회 저장 보장")
    void testSingleSaveWithMultipleStreamKeys() throws Exception {
        // given - 동일 원본 행, 서로 다른 timestamp 필드가 모두 존재
        Map<String, Object> data = new HashMap<>();
        data.put("user_id", "USER_MULTI_KEY");
        data.put("event_time", "2025-08-29 12:00:00");
        data.put("event_time_alt", "2025-08-29 12:05:00");

        Step1Result step1Result = createStep1Result("DS_MULTI_KEYS", List.of(
            createMappedDataRow(100L, data)
        ));

        // 두 개의 활성 StreamKey가 있다고 가정 (둘 다 유효)
        Set<StreamKey> streamKeys = Set.of(
            new StreamKey("user_id", "event_time"),
            new StreamKey("user_id", "event_time_alt")
        );

        // Mock: 아직 처리된 원본 없음 → 저장 1회만 수행되어야 하며, savedOnce로 루프 중단
        when(eventStreamRepository.findByMappedDataStorageId(100L)).thenReturn(Optional.empty());
        when(eventStreamRepository.save(any(EngineEventStreamEntity.class)))
            .thenReturn(createMockEventStreamEntity(10L, "USER_MULTI_KEY", data));

        // when
        EventStreamResult result = eventStreamService.saveEventStream(step1Result, streamKeys, "test-executor");

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalEvents()).isEqualTo(1); // 여러 키라도 1회 저장
        verify(eventStreamRepository, times(1)).save(any(EngineEventStreamEntity.class));
    }

    @Test
    @DisplayName("대용량 JSON 배치 처리 성능 테스트")
    void testLargeJsonBatchProcessing() throws Exception {
        // given - 대용량 JSON 데이터 (1000개)
        List<MappedDataRow> largeDataSet = createLargeJsonDataSet(1000);
        
        Step1Result step1Result = createStep1Result("DS_LARGE_BATCH", largeDataSet);
        
        Set<StreamKey> streamKeys = Set.of(
            new StreamKey("user_id", "event_time")
        );
        
        // Mock 설정
        when(eventStreamRepository.findByMappedDataStorageId(anyLong())).thenReturn(Optional.empty());
        when(eventStreamRepository.existsByGroupKeyAndCreatedAt(anyString(), any(LocalDateTime.class)))
            .thenReturn(false);
        
        when(eventStreamRepository.save(any(EngineEventStreamEntity.class)))
            .thenReturn(createMockEventStreamEntity(1L, "USER_BATCH", new HashMap<>()));

        // when
        long startTime = System.currentTimeMillis();
        EventStreamResult result = eventStreamService.saveEventStream(step1Result, streamKeys, "test-executor");
        long processingTime = System.currentTimeMillis() - startTime;

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTotalEvents()).isEqualTo(1000);
        assertThat(result.getProcessedRows()).isEqualTo(1000);
        
        // 성능 검증 (1000개 처리가 5초 이내에 완료되어야 함)
        assertThat(processingTime).isLessThan(5000);
        
        // 모든 데이터가 저장되었는지 확인
        verify(eventStreamRepository, times(1000)).save(any(EngineEventStreamEntity.class));
    }

    // Helper Methods

    private Map<String, Object> createLoginJsonData() {
        Map<String, Object> data = new HashMap<>();
        data.put("user_id", "USER_12345");
        data.put("login_datetime", "2025-08-29 02:30:00");
        data.put("login_time", "02:30:00");
        data.put("ip_address", "192.168.1.100");
        data.put("user_agent", "Chrome/91.0.4472.124");
        data.put("login_result", "SUCCESS");
        data.put("session_id", "SESSION_ABC123");
        
        // 중첩 객체
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("device_type", "desktop");
        deviceInfo.put("os", "Windows 10");
        deviceInfo.put("browser", "Chrome");
        data.put("device_info", deviceInfo);
        
        return data;
    }

    private Map<String, Object> createATMJsonData() {
        Map<String, Object> data = new HashMap<>();
        data.put("customer_id", "CUSTOMER_67890");
        data.put("transaction_datetime", "2025-08-29 15:45:00");
        data.put("transaction_time", "15:45:00");
        data.put("atm_id", "ATM_SEOUL_001");
        data.put("transaction_amount", 2000000);
        data.put("transaction_type", "WITHDRAWAL");
        data.put("balance_after", 500000);
        data.put("location", "Seoul, Gangnam");
        
        // 카드 정보 중첩 객체
        Map<String, Object> cardInfo = new HashMap<>();
        cardInfo.put("card_number", "**** **** **** 1234");
        cardInfo.put("card_type", "DEBIT");
        cardInfo.put("issuer", "KB");
        data.put("card_info", cardInfo);
        
        return data;
    }

    private Map<String, Object> createFinancialTransactionJsonData() {
        Map<String, Object> data = new HashMap<>();
        data.put("customer_id", "CUSTOMER_11111");
        data.put("transaction_date", "2025-08-29 09:20:00");
        data.put("transaction_time", "09:20:00");
        data.put("transaction_amount", 500000);
        data.put("transaction_currency", "USD");
        data.put("transaction_country", "US");
        data.put("country_code", "US");
        data.put("merchant_name", "Amazon.com");
        data.put("location", "New York");
        
        // 가맹점 정보 중첩 객체
        Map<String, Object> merchantInfo = new HashMap<>();
        merchantInfo.put("merchant_code", "AMZN_NYC_001");
        merchantInfo.put("category", "E-Commerce");
        merchantInfo.put("risk_level", "LOW");
        data.put("merchant_info", merchantInfo);
        
        return data;
    }

    private Map<String, Object> createComplexNestedJsonData() {
        Map<String, Object> data = new HashMap<>();
        data.put("user_id", "USER_COMPLEX");
        data.put("event_datetime", "2025-08-29 16:30:00");
        data.put("event_type", "DEVICE_SECURITY");
        
        // 3단계 중첩 구조
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("device_id", "DEVICE_12345");
        deviceInfo.put("os", "Android");
        deviceInfo.put("app_version", "2.1.5");
        
        // 위치 정보 중첩
        Map<String, Object> location = new HashMap<>();
        location.put("latitude", 37.5665);
        location.put("longitude", 126.9780);
        location.put("accuracy", 10);
        location.put("address", "Seoul, South Korea");
        deviceInfo.put("location", location);
        
        data.put("device_info", deviceInfo);
        
        // 배열 구조
        List<Map<String, Object>> permissions = new ArrayList<>();
        
        Map<String, Object> permission1 = new HashMap<>();
        permission1.put("permission", "CAMERA");
        permission1.put("granted", true);
        permission1.put("timestamp", "2025-08-29 16:25:00");
        permissions.add(permission1);
        
        Map<String, Object> permission2 = new HashMap<>();
        permission2.put("permission", "LOCATION");
        permission2.put("granted", false);
        permission2.put("timestamp", "2025-08-29 16:26:00");
        permissions.add(permission2);
        
        data.put("permissions", permissions);
        
        return data;
    }

    private List<MappedDataRow> createDiverseTimestampData() {
        List<MappedDataRow> dataRows = new ArrayList<>();
        
        // 형식 1: "yyyy-MM-dd HH:mm:ss"
        Map<String, Object> data1 = new HashMap<>();
        data1.put("user_id", "USER_001");
        data1.put("event_time", "2025-08-29 10:00:00");
        data1.put("format", "standard");
        dataRows.add(createMappedDataRow(11L, data1));
        
        // 형식 2: "yyyy-MM-ddTHH:mm:ss" (ISO format)
        Map<String, Object> data2 = new HashMap<>();
        data2.put("user_id", "USER_002");
        data2.put("event_time", "2025-08-29T11:00:00");
        data2.put("format", "iso");
        dataRows.add(createMappedDataRow(12L, data2));
        
        // 형식 3: LocalDateTime 객체 직접 (실제로는 드물지만 가능)
        Map<String, Object> data3 = new HashMap<>();
        data3.put("user_id", "USER_003");
        data3.put("event_time", LocalDateTime.of(2025, 8, 29, 12, 0, 0));
        data3.put("format", "object");
        dataRows.add(createMappedDataRow(13L, data3));
        
        // 형식 4: 잘못된 형식 (파싱 실패시 현재 시간 사용)
        Map<String, Object> data4 = new HashMap<>();
        data4.put("user_id", "USER_004");
        data4.put("event_time", "invalid-timestamp-format");
        data4.put("format", "invalid");
        dataRows.add(createMappedDataRow(14L, data4));
        
        return dataRows;
    }

    private List<MappedDataRow> createLargeJsonDataSet(int size) {
        List<MappedDataRow> dataRows = new ArrayList<>();
        
        for (int i = 0; i < size; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("user_id", "USER_" + String.format("%06d", i));
            data.put("event_time", "2025-08-29 " + String.format("%02d:%02d:%02d", 
                (i / 3600) % 24, (i / 60) % 60, i % 60));
            data.put("sequence", i);
            data.put("data_size", "large");
            
            // 일부 복잡한 중첩 구조 추가
            if (i % 10 == 0) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("batch_id", "BATCH_" + (i / 10));
                metadata.put("complexity", "high");
                data.put("metadata", metadata);
            }
            
            dataRows.add(createMappedDataRow((long) (1000 + i), data));
        }
        
        return dataRows;
    }

    private Step1Result createStep1Result(String dataSourceId, List<MappedDataRow> mappedDataRows) {
        return Step1Result.builder()
            .dataSourceId(dataSourceId)
            .mappedDataRows(mappedDataRows)
            .build();
    }

    private MappedDataRow createMappedDataRow(Long mappedDataStorageId, Map<String, Object> rawData) {
        return MappedDataRow.builder()
            .mappedDataStorageId(mappedDataStorageId)
            .rawData(rawData)
            .build();
    }

    private EngineEventStreamEntity createMockEventStreamEntity(Long eventStreamId, String groupKey, 
                                                               Map<String, Object> eventData) {
        return EngineEventStreamEntity.builder()
            .eventStreamId(eventStreamId)
            .groupKey(groupKey)
            .eventData(eventData)
            .eventDt(LocalDateTime.now())
            .mappedDataStorageId(eventStreamId)
            .build();
    }
}

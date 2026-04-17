package com.itmasters.icon.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EventStreamRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventLogServiceTest {
    
    @Mock
    private EventStreamRepository eventStreamRepository;
    
    @Mock
    private EngineProfileRepository profileRepository;

    @InjectMocks
    private EventLogService eventLogService;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        eventLogService = new EventLogService(objectMapper, profileRepository, eventStreamRepository);
    }
    
    @Test
    void testConvertTransferToActivityLog() {
        // Given
        Map<String, Object> mappedData = new HashMap<>();
        mappedData.put("CUS_ID", "CUS001");
        mappedData.put("TRX_TYPE", "이체");
        mappedData.put("TRX_AMT", new BigDecimal("5000000"));
        mappedData.put("SENDER", "내계좌");
        mappedData.put("RECEIVER", "타인계좌");
        mappedData.put("BAL_AMT", new BigDecimal("3000000"));
        mappedData.put("DEVICE_ID", "DEV001");
        mappedData.put("IP_ADDRESS", "192.168.1.100");
        mappedData.put("ACCESS_COUNTRY", "KR");
        mappedData.put("CUSTOMER_AGE", 35);
        mappedData.put("TRX_DT", "2025-01-19 10:30:00");
        
        // When
        eventLogService.convertToEventStream(mappedData, "PROFILE_FINANCIAL_01", null);
        
        // Then
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> paramsCaptor = ArgumentCaptor.forClass(Object[].class);
        
        verify(eventStreamRepository, times(1)).save(any());
    }
    
    @Test
    void testConvertOtpIssueToActivityLog() {
        // Given
        Map<String, Object> mappedData = new HashMap<>();
        mappedData.put("CUS_ID", "CUS002");
        mappedData.put("OTP_ISSUED_AT", "2025-01-19 08:00:00");
        mappedData.put("DEVICE_ID", "DEV002");
        mappedData.put("IP_ADDRESS", "192.168.1.101");
        mappedData.put("TRX_DT", "2025-01-19 08:00:00");
        
        // When
        eventLogService.convertToEventStream(mappedData, "PROFILE_FINANCIAL_01", null);
        
        // Then
        verify(eventStreamRepository, times(1)).save(any());
    }
    
    @Test
    void testConvertAtmWithdrawToActivityLog() {
        // Given
        Map<String, Object> mappedData = new HashMap<>();
        mappedData.put("CUS_ID", "CUS003");
        mappedData.put("TRX_TYPE", "ATM출금");
        mappedData.put("TRX_AMT", new BigDecimal("300000"));
        mappedData.put("ATM_WD_CNT", 5);
        mappedData.put("IS_NIGHT_TIME", "Y");
        mappedData.put("TRX_DT", "2025-01-19 01:30:00");
        
        // When
        eventLogService.convertToEventStream(mappedData, "PROFILE_FINANCIAL_01", null);
        
        // Then
        verify(eventStreamRepository, times(1)).save(any());
    }
    
    @Test
    void testHandleUnknownEventType() {
        // Given
        Map<String, Object> mappedData = new HashMap<>();
        mappedData.put("CUS_ID", "CUS004");
        mappedData.put("UNKNOWN_FIELD", "unknown_value");
        
        // When
        eventLogService.convertToEventStream(mappedData, "PROFILE_UNKNOWN", null);
        
        // Then
        verify(eventStreamRepository, times(1)).save(any());
    }
    
    @Test
    void testExtractUserIdWithVariousFields() {
        // Given - CUS_ID 있는 경우
        Map<String, Object> data1 = new HashMap<>();
        data1.put("CUS_ID", "CUS001");
        
        // Given - CUSTOMER_ID 있는 경우
        Map<String, Object> data2 = new HashMap<>();
        data2.put("CUSTOMER_ID", "CUST002");
        
        // Given - USER_ID 있는 경우
        Map<String, Object> data3 = new HashMap<>();
        data3.put("USER_ID", "USER003");
        
        // When & Then
        eventLogService.convertToEventStream(data1, "PROFILE_FINANCIAL_01", null);
        eventLogService.convertToEventStream(data2, "PROFILE_FINANCIAL_01", null);
        eventLogService.convertToEventStream(data3, "PROFILE_FINANCIAL_01", null);
        
        verify(eventStreamRepository, times(3)).save(any());
    }
}

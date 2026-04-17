package com.itmasters.icon.engine.service;
import com.itmasters.icon.common.domain.type.ExecutionMode;
import com.itmasters.icon.engine.adapter.out.persistence.entity.LandingRawRecordEntity;
import com.itmasters.icon.engine.dto.DataProcessingResultDto;
import com.itmasters.icon.engine.dto.DataProcessingStepResultDto;
import com.itmasters.icon.engine.pipeline.DataPipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataProcessingServiceTest {

    @InjectMocks
    private DataProcessingService dataProcessingService;

    @Mock
    private DataPipeline dataPipeline;

    @Mock
    private ExecDsMpService execDsMpService;

    private String testDataSourceId;
    private List<Map<String, Object>> testData;

    @BeforeEach
    void setUp() {
        testDataSourceId = "DS001";
        
        // 테스트 데이터 준비
        testData = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("field1", "value1");
        row1.put("field2", 100);
        testData.add(row1);
        
        Map<String, Object> row2 = new HashMap<>();
        row2.put("field1", "value2");
        row2.put("field2", 200);
        testData.add(row2);
    }

    @Test
    @DisplayName("processDataSourceStep1 - 정상 처리 및 데이터 저장 테스트")
    void testProcessDataSourceStep1_SuccessWithDataSave() {
        // Given
        long execDsMpId = 1L;
        DataProcessingResultDto successResult = DataProcessingResultDto.success(testData, testData, null);

        when(execDsMpService.createExecution(eq(testDataSourceId), eq("test-user"), eq(ExecutionMode.MANUAL)))
            .thenReturn(execDsMpId);
        when(dataPipeline.processDataSource(testDataSourceId))
            .thenReturn(successResult);
        when(execDsMpService.saveLandingData(eq(execDsMpId), anyList()))
            .thenReturn(List.of(mock(LandingRawRecordEntity.class), mock(LandingRawRecordEntity.class)));
        when(execDsMpService.persistMappedData(eq(execDsMpId), anyList(), anyList()))
            .thenReturn(List.of());
        
        // When
        DataProcessingStepResultDto result = 
            dataProcessingService.processDataSourceStep1(testDataSourceId, "test-user", ExecutionMode.MANUAL);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getExecDsMpId()).isEqualTo(execDsMpId);
        assertThat(result.getData()).hasSize(2);

        // Verify 호출 순서
        verify(execDsMpService).createExecution(eq(testDataSourceId), eq("test-user"), eq(ExecutionMode.MANUAL));
        verify(dataPipeline).processDataSource(testDataSourceId);
        verify(execDsMpService).saveLandingData(eq(execDsMpId), anyList());
        verify(execDsMpService).persistMappedData(eq(execDsMpId), anyList(), anyList());
        verify(execDsMpService, never()).failExecution(anyLong(), anyString());
    }

    @Test
    @DisplayName("processDataSourceStep1 - 데이터 처리 실패 테스트")
    void testProcessDataSourceStep1_DataProcessingFailed() {
        // Given
        long execDsMpId = 1L;
        DataProcessingResultDto failedResult = DataProcessingResultDto.failed("데이터 읽기 실패");
        
        when(execDsMpService.createExecution(eq(testDataSourceId), eq("test-user"), eq(ExecutionMode.MANUAL)))
            .thenReturn(execDsMpId);
        when(dataPipeline.processDataSource(testDataSourceId))
            .thenReturn(failedResult);
        
        // When
        DataProcessingStepResultDto result = 
            dataProcessingService.processDataSourceStep1(testDataSourceId, "test-user", ExecutionMode.MANUAL);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getExecDsMpId()).isEqualTo(execDsMpId);
        assertThat(result.getErrorMessage()).isEqualTo("데이터 읽기 실패");
        
        verify(execDsMpService).createExecution(eq(testDataSourceId), eq("test-user"), eq(ExecutionMode.MANUAL));
        verify(dataPipeline).processDataSource(testDataSourceId);
        verify(execDsMpService).failExecution(eq(execDsMpId), eq("데이터 읽기 실패"));
        verify(execDsMpService, never()).persistMappedData(anyLong(), anyList(), anyList());
    }

    @Test
    @DisplayName("processDataSourceStep1 - 예외 발생 테스트")
    void testProcessDataSourceStep1_ExceptionThrown() {
        // Given
        long execDsMpId = 1L;
        String errorMessage = "데이터베이스 연결 실패";
        
        when(execDsMpService.createExecution(eq(testDataSourceId), eq("test-user"), eq(ExecutionMode.MANUAL)))
            .thenReturn(execDsMpId);
        when(dataPipeline.processDataSource(testDataSourceId))
            .thenThrow(new RuntimeException(errorMessage));
        
        // When
        DataProcessingStepResultDto result = 
            dataProcessingService.processDataSourceStep1(testDataSourceId, "test-user", ExecutionMode.MANUAL);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getExecDsMpId()).isEqualTo(execDsMpId);
        assertThat(result.getErrorMessage()).isEqualTo(errorMessage);
        
        verify(execDsMpService).failExecution(eq(execDsMpId), eq(errorMessage));
        verify(execDsMpService, never()).persistMappedData(anyLong(), anyList(), anyList());
    }
}

package com.itmasters.icon.engine.service;

import com.itmasters.icon.common.domain.type.ExecutionMode;
import com.itmasters.icon.engine.adapter.out.persistence.entity.LandingRawRecordEntity;
import com.itmasters.icon.engine.dto.DataProcessingResultDto;
import com.itmasters.icon.engine.dto.DataProcessingStepResultDto;
import com.itmasters.icon.engine.pipeline.DataPipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 데이터 처리 서비스 - 트랜잭션 분리를 위한 별도 서비스
 * Step1: 데이터 읽기 및 실행 로그 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataProcessingService {
    
    private final DataPipeline dataPipeline;
    private final ExecDsMpService execDsMpService;
    
    /**
     * 기존 실행 데이터 로드 (테스트용)
     * mapped_storages 테이블에서 데이터를 읽어옴
     * 
     * @param execDsMpId 실행 ID
     * @param profileId 프로파일 ID
     * @return 처리 결과
     */
    @Transactional(readOnly = true)
    public DataProcessingResultDto loadExistingData(Long execDsMpId, String profileId) {
        log.info("기존 실행 데이터 로드 - execDsMpId: {}, profileId: {}", execDsMpId, profileId);
        
        try {
            // mapped_storages에서 데이터 조회
            List<Map<String, Object>> data = execDsMpService.loadMappedData(execDsMpId, profileId);
            log.info("데이터 로드 완료: {} 건", data.size());
            
            return DataProcessingResultDto.success(null, data, null);
            
        } catch (Exception e) {
            log.error("데이터 로드 실패", e);
            return DataProcessingResultDto.failed(e.getMessage());
        }
    }
    
    /**
     * Step1: 데이터소스 처리 및 실행 로그 생성
     * 별도 트랜잭션으로 실행되어 이후 단계 실패와 무관하게 로그 저장
     * 
     * @param dataSourceId 데이터소스 ID
     * @param executedBy 실행자
     * @param executionMode 실행 모드
     * @return 처리 결과 (execDsMpId, 데이터, 성공여부 포함)
     */
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DataProcessingStepResultDto processDataSourceStep1(String dataSourceId, 
                                                           String executedBy, 
                                                           ExecutionMode executionMode) {
        log.info("Step1 시작 - 데이터소스 처리 및 실행 로그 생성: dataSourceId={}", dataSourceId);
        
        // 1. 실행 로그 생성
        Long execDsMpId = execDsMpService.createExecution(dataSourceId, executedBy, executionMode);
        log.info("실행 로그 생성 완료 - execDsMpId: {}", execDsMpId);
        
        // 2. 데이터소스에서 데이터 읽기
        try {
            log.debug("데이터 파이프라인 시작 - dataSourceId: {}", dataSourceId);
            DataProcessingResultDto processedData = dataPipeline.processDataSource(dataSourceId);
            
            if (!processedData.isSuccess()) {
                String errorMsg = processedData.getErrorMessage() != null ? 
                    processedData.getErrorMessage() : "데이터 처리 실패";
                log.error("데이터 처리 실패: {}", errorMsg);
                
                // 실패 상태 업데이트 (별도 트랜잭션)
                execDsMpService.failExecution(execDsMpId, errorMsg);
                
                return DataProcessingStepResultDto.failed(execDsMpId, errorMsg);
            }
            
            List<Map<String, Object>> rawData = processedData.getRawData();
            List<Map<String, Object>> mappedData = processedData.getMappedData();
            int rawCount = rawData != null ? rawData.size() : 0;
            int mappedCount = mappedData != null ? mappedData.size() : 0;
            log.info("데이터 읽기 성공 - dataSourceId: {}, raw rows: {}, mapped rows: {}", dataSourceId, rawCount, mappedCount);

            List<LandingRawRecordEntity> landingRecords = List.of();
            if (rawCount > 0) {
                landingRecords = execDsMpService.saveLandingData(execDsMpId, rawData);
                log.info("Landing 저장 완료 - execDsMpId: {}, rows: {}", execDsMpId, landingRecords.size());
            }

            if (mappedCount > 0 && !landingRecords.isEmpty()) {
                execDsMpService.persistMappedData(execDsMpId, landingRecords, mappedData);
            }

            // 4. Step1 성공 상태 업데이트
            execDsMpService.completeExecution(execDsMpId, rawCount);
            log.info("Step1 완료 - execDsMpId: {}, rows: {}", execDsMpId, rawCount);

            return DataProcessingStepResultDto.success(execDsMpId, rawData != null ? rawData : List.of());
            
        } catch (Exception e) {
            log.error("데이터 읽기 중 오류 발생 - dataSourceId: {}", dataSourceId, e);
            
            // 실패 상태 업데이트 (별도 트랜잭션)
            execDsMpService.failExecution(execDsMpId, e.getMessage());
            
            return DataProcessingStepResultDto.failed(execDsMpId, e.getMessage());
        }
    }
}

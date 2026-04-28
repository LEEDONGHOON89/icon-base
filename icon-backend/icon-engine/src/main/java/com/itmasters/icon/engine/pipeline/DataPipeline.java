package com.itmasters.icon.engine.pipeline;

import com.itmasters.icon.engine.datasource.DataSourceReader;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDataSourceEntity;
import com.itmasters.icon.engine.dto.DataProcessingResultDto;
import com.itmasters.icon.engine.dto.SchemaValidationResult;
// import com.itmasters.icon.engine.mapper.FieldMappingEngine;
import com.itmasters.icon.engine.mapping.FieldMappingEngine;
// Field mapping will be handled directly with schemas
import com.itmasters.icon.engine.validator.SchemaValidator;
// import com.itmasters.icon.engine.service.EventLogService; // Stage 2로 이동
import com.itmasters.icon.engine.service.DerivedFieldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 데이터 파이프라인 (Stage 1 - 전처리)
 * 데이터 읽기 → 스키마 검증 → 필드 매핑까지 수행하여 매핑 결과를 반환합니다.
 * 저장(mapped_storages)은 Step1Processor에서 ExecDsMpService를 통해 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPipeline {
    private final DataSourceReader dataSourceReader;
    private final SchemaValidator schemaValidator;
    private final FieldMappingEngine fieldMappingEngine;
    private final DerivedFieldService derivedFieldService;


    /**
     * 데이터소스에서 데이터를 읽고 검증하고 DataSourceSchema 기반으로 매핑하는 전체 프로세스 실행
     * @param dataSourceId 데이터소스 ID
     */
    public DataProcessingResultDto processDataSource(String dataSourceId) {
        log.info("데이터 처리 시작 - DataSourceId: {}", dataSourceId);
        
        try {
            // 1. 데이터 읽기
            List<Map<String, Object>> rawData = dataSourceReader.readByDataSourceId(dataSourceId);
            if (rawData == null) {
                rawData = List.of();
            }
            log.info("데이터 읽기 완료 - {} 건", rawData.size());

            List<Map<String, Object>> rawSnapshot = rawData.stream()
                    .map(row -> row == null ? Map.<String, Object>of() : new java.util.LinkedHashMap<>(row))
                    .toList();

            // 2. 스키마 검증
            SchemaValidationResult validation = schemaValidator.validate(dataSourceId, rawData);
            if (!validation.isValid()) {
                log.warn("스키마 검증 실패: {}", validation.getMessage());
                return DataProcessingResultDto.failed(validation.getMessage());
            }
            log.info("스키마 검증 성공");

            // 3. DataSourceSchema 기반 필드 매핑
            List<Map<String, Object>> mappedData = fieldMappingEngine.mapByDataSource(rawData, dataSourceId);
            log.info("필드 매핑 완료 - {} 건", mappedData.size());

            // 4. 파생 필드 계산 (매핑된 데이터에 파생 필드 추가)
            try {
                log.info("파생 필드 계산 시작 - dataSourceId: {}", dataSourceId);
                derivedFieldService.computeFields(dataSourceId, mappedData);
                log.info("파생 필드 계산 완료");

                // DEBUG: 첫 번째 행 확인
                if (!mappedData.isEmpty()) {
                    Map<String, Object> firstRow = mappedData.get(0);
                    log.info("🔍 [DataPipeline] 첫 번째 행 키: {}", firstRow.keySet());
                    log.info("🔍 [DataPipeline] is_third_party: {}", firstRow.get("is_third_party"));
                }
            } catch (Exception e) {
                log.error("파생 필드 계산 실패", e);
                // 파생 필드 계산 실패는 전체 프로세스를 중단하지 않음
            }

            // Stage 1 결과 반환 (매핑된 데이터 + 파생 필드)
            return DataProcessingResultDto.success(rawSnapshot, mappedData, validation);
            
        } catch (Exception e) {
            log.error("데이터 처리 실패 - DataSourceId: {}", dataSourceId, e);
            return DataProcessingResultDto.failed(e.getMessage());
        }
    }


    /**
     * 도메인 객체 기반 처리 (테스트용)
     */
    public DataProcessingResultDto processDataSource(EngineDataSourceEntity dataSource) {
        log.info("데이터 처리 시작 - DataSource: {}", dataSource.getName());
        
        try {
            // 1. 데이터 읽기
            List<Map<String, Object>> rawData = dataSourceReader.read(dataSource);
            if (rawData == null) {
                rawData = List.of();
            }
            
            // 2. 스키마 검증
            SchemaValidationResult validation = schemaValidator.validate(dataSource.getDataSourceId(), rawData);
            
            if (!validation.isValid()) {
                return DataProcessingResultDto.failed(validation.getMessage());
            }
            
            List<Map<String, Object>> rawSnapshot = rawData.stream()
                    .map(row -> row == null ? Map.<String, Object>of() : new java.util.LinkedHashMap<>(row))
                    .toList();
            return DataProcessingResultDto.success(rawSnapshot, rawSnapshot, validation);
            
        } catch (Exception e) {
            log.error("데이터 처리 실패", e);
            return DataProcessingResultDto.failed(e.getMessage());
        }
    }

    /**
     * 외부에서 전달된 raw 데이터에 대해 검증/매핑만 수행 (단건/테스트 입력용)
     */
    public DataProcessingResultDto processInline(String dataSourceId, List<Map<String, Object>> rawRows) {
        try {
            List<Map<String, Object>> rawData = rawRows == null
                    ? List.of()
                    : rawRows.stream()
                    .map(row -> row == null ? Map.<String, Object>of() : new java.util.LinkedHashMap<>(row))
                    .toList();

            SchemaValidationResult validation = schemaValidator.validate(dataSourceId, rawData);
            // [2026-04-28] 검증 실패 시 무시하고 진행하던 버그 수정 - processDataSource()와 동일하게 failed() 반환
            if (!validation.isValid()) {
                log.warn("[Inline] 스키마 검증 실패: {}", validation.getMessage());
                return DataProcessingResultDto.failed(validation.getMessage());
            }

            List<Map<String, Object>> mappedData = fieldMappingEngine.mapByDataSource(rawData, dataSourceId);

            // 파생 필드 계산
            try {
                derivedFieldService.computeFields(dataSourceId, mappedData);
            } catch (Exception e) {
                log.error("[Inline] 파생 필드 계산 실패", e);
            }

            return DataProcessingResultDto.success(rawData, mappedData, validation);

        } catch (Exception e) {
            log.error("[Inline] 데이터 처리 실패 - DataSourceId: {}", dataSourceId, e);
            return DataProcessingResultDto.failed(e.getMessage());
        }
    }
}

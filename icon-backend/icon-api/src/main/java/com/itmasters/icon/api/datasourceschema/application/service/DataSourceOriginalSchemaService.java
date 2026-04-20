package com.itmasters.icon.api.datasourceschema.application.service;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DataSourceEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.DataSourceJpaRepository;
import com.itmasters.icon.api.datasourceschema.adapter.in.web.DataSourceOriginalSchemaDto;
import com.itmasters.icon.api.datasourceschema.adapter.out.persistence.entity.DataSourceOriginalSchemaEntity;
import com.itmasters.icon.api.datasourceschema.adapter.out.persistence.repository.DataSourceOriginalSchemaJpaRepository;
import com.itmasters.icon.api.parser.adapter.out.persistence.entity.ParserEntity;
import com.itmasters.icon.api.parser.adapter.out.persistence.repository.ParserJpaRepository;
import com.itmasters.icon.api.standardfield.adapter.out.persistence.entity.StandardFieldEntity;
import com.itmasters.icon.api.standardfield.adapter.out.persistence.repository.StandardFieldJpaRepository;
import com.itmasters.icon.common.domain.type.FieldDataType;
import com.itmasters.icon.common.domain.IdGenerator;
import com.itmasters.icon.common.domain.EntityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 데이터소스 원본 스키마 서비스
 * 데이터소스에서 실제 필드를 탐지하고 관리하는 기능을 제공
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DataSourceOriginalSchemaService {

    private final DataSourceOriginalSchemaJpaRepository originalSchemaRepository;
    private final DataSourceJpaRepository dataSourceRepository;
    private final StandardFieldJpaRepository standardFieldRepository;
    // [2026-04-20] 파서 연동
    private final ParserJpaRepository parserRepository;
    private final IdGenerator idGenerator;

    /**
     * 데이터소스의 모든 원본 스키마 조회 (비활성 포함)
     */
    public List<DataSourceOriginalSchemaDto.Response> getAllOriginalSchemas(String dataSourceId) {
        List<DataSourceOriginalSchemaEntity> schemas = originalSchemaRepository.findByDataSourceDataSourceId(dataSourceId);
        return schemas.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 원본 스키마 일괄 수정 (새 필드 생성 + 기존 필드 수정 통합)
     */
    @Transactional
    public List<DataSourceOriginalSchemaDto.Response> updateOriginalSchemasBulk(String dataSourceId, 
                                                                    List<DataSourceOriginalSchemaDto.BulkUpdateRequest.BulkUpdateItem> requests) {
        
        DataSourceEntity dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new IllegalArgumentException("데이터소스를 찾을 수 없습니다: " + dataSourceId));

        List<DataSourceOriginalSchemaEntity> allSchemas = new ArrayList<>();
        
        // 기존 스키마 ID만 추출 (null이 아니고 빈 문자열이 아닌 경우)
        List<String> existingSchemaIds = requests.stream()
                .map(DataSourceOriginalSchemaDto.BulkUpdateRequest.BulkUpdateItem::getSchemaId)
                .filter(StringUtils::hasText)
                .toList();
        
        // 기존 스키마들을 한번에 조회
        Map<String, DataSourceOriginalSchemaEntity> existingSchemaMap = new HashMap<>();
        if (!existingSchemaIds.isEmpty()) {
            List<DataSourceOriginalSchemaEntity> existingSchemas = originalSchemaRepository.findAllById(existingSchemaIds);
            
            if (existingSchemas.size() != existingSchemaIds.size()) {
                throw new IllegalArgumentException("일부 스키마를 찾을 수 없습니다");
            }
            
            existingSchemas.forEach(schema -> existingSchemaMap.put(schema.getDataSourceSchemaId(), schema));
        }
        
        // 모든 요청을 하나의 for문으로 처리
        for (DataSourceOriginalSchemaDto.BulkUpdateRequest.BulkUpdateItem request : requests) {
            if (StringUtils.hasText(request.getSchemaId())) {
                // 기존 필드 수정
                DataSourceOriginalSchemaEntity schema = existingSchemaMap.get(request.getSchemaId());
                if (schema == null) {
                    throw new IllegalArgumentException("스키마를 찾을 수 없습니다: " + request.getSchemaId());
                }
                
                // 변경된 필드만 업데이트
                if (request.getFieldName() != null) schema.setFieldName(request.getFieldName());
                if (request.getDataType() != null) schema.setDataType(request.getDataType());
                if (request.getIsRequired() != null) schema.setRequired(request.getIsRequired());
                if (request.getDefaultValue() != null) schema.setDefaultValue(request.getDefaultValue());
                if (request.getDescription() != null) schema.setDescription(request.getDescription());
                if (request.getFieldOrder() != null) schema.setFieldOrder(request.getFieldOrder());
                if (request.getIsActive() != null) schema.setActive(request.getIsActive());
                
                // 표준 필드 매핑 처리
                if (request.getStandardFieldId() != null) {
                    if (StringUtils.hasText(request.getStandardFieldId())) {
                        StandardFieldEntity standardField = standardFieldRepository.findById(request.getStandardFieldId())
                                .orElseThrow(() -> new IllegalArgumentException("표준 필드를 찾을 수 없습니다: " + request.getStandardFieldId()));
                        schema.mapToStandardField(standardField);
                    } else {
                        schema.unmapStandardField();
                    }
                }

                // [2026-04-20] 파서 연동 처리
                if (request.getParserId() != null) {
                    if (StringUtils.hasText(request.getParserId())) {
                        ParserEntity parser = parserRepository.findById(request.getParserId())
                                .orElseThrow(() -> new IllegalArgumentException("파서를 찾을 수 없습니다: " + request.getParserId()));
                        schema.setParser(parser);
                    } else {
                        schema.setParser(null);
                    }
                }

                allSchemas.add(schema);
            } else {
                // 새 필드 생성
                // 필드명 중복 확인
                if (originalSchemaRepository.existsByDataSourceDataSourceIdAndFieldName(dataSourceId, request.getFieldName())) {
                    throw new IllegalArgumentException("이미 존재하는 필드명입니다: " + request.getFieldName());
                }
                
                DataSourceOriginalSchemaEntity newSchema = DataSourceOriginalSchemaEntity.builder()
                        .dataSourceSchemaId(idGenerator.generateId(EntityType.DATA_SOURCE_SCHEMA))
                        .dataSource(dataSource)
                        .fieldName(request.getFieldName())
                        .dataType(request.getDataType())
                        .isRequired(request.getIsRequired() != null ? request.getIsRequired() : false)
                        .defaultValue(request.getDefaultValue())
                        .description(request.getDescription())
                        .fieldOrder(request.getFieldOrder() != null ? request.getFieldOrder() : 1)
                        .isActive(true)
                        .build();
                
                // 표준 필드 매핑 처리
                if (StringUtils.hasText(request.getStandardFieldId())) {
                    StandardFieldEntity standardField = standardFieldRepository.findById(request.getStandardFieldId())
                            .orElseThrow(() -> new IllegalArgumentException("표준 필드를 찾을 수 없습니다: " + request.getStandardFieldId()));
                    newSchema.setStandardField(standardField);
                }

                // [2026-04-20] 파서 연동 처리
                if (StringUtils.hasText(request.getParserId())) {
                    ParserEntity parser = parserRepository.findById(request.getParserId())
                            .orElseThrow(() -> new IllegalArgumentException("파서를 찾을 수 없습니다: " + request.getParserId()));
                    newSchema.setParser(parser);
                }

                allSchemas.add(newSchema);
            }
        }
        
        List<DataSourceOriginalSchemaEntity> saved = originalSchemaRepository.saveAll(allSchemas);
        return saved.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 개별 스키마의 표준 필드 매핑 업데이트
     */
    @Transactional
    public DataSourceOriginalSchemaDto.Response updateStandardFieldMapping(
            String schemaId, 
            DataSourceOriginalSchemaDto.StandardFieldMappingRequest request) {
        
        // 스키마 조회
        DataSourceOriginalSchemaEntity schema = originalSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new IllegalArgumentException("스키마를 찾을 수 없습니다: " + schemaId));
        
        // 표준 필드 매핑 처리
        if (StringUtils.hasText(request.getStandardFieldId())) {
            StandardFieldEntity standardField = standardFieldRepository.findById(request.getStandardFieldId())
                    .orElseThrow(() -> new IllegalArgumentException("표준 필드를 찾을 수 없습니다: " + request.getStandardFieldId()));
            schema.mapToStandardField(standardField);
        } else {
            schema.unmapStandardField();
        }
        
        // 활성화 상태 업데이트
        if (request.getIsActive() != null) {
            schema.updateActiveStatus(request.getIsActive());
        }

        // [2026-04-20] 파서 연동 — parserId가 있으면 파서 설정, 없으면 파서 해제
        if (StringUtils.hasText(request.getParserId())) {
            ParserEntity parser = parserRepository.findById(request.getParserId())
                    .orElseThrow(() -> new IllegalArgumentException("파서를 찾을 수 없습니다: " + request.getParserId()));
            schema.setParser(parser);
        } else if (request.getParserId() != null) {
            // 빈 문자열 or 명시적 null → 파서 해제
            schema.setParser(null);
        }

        DataSourceOriginalSchemaEntity saved = originalSchemaRepository.save(schema);
        return toResponse(saved);
    }

    /**
     * 엔티티를 DTO로 변환
     */
    private DataSourceOriginalSchemaDto.Response toResponse(DataSourceOriginalSchemaEntity entity) {
        return DataSourceOriginalSchemaDto.Response.builder()
                .schemaId(entity.getDataSourceSchemaId())
                .dataSourceId(entity.getDataSource().getDataSourceId())
                .fieldName(entity.getFieldName())
                .dataType(entity.getDataType())
                .isRequired(entity.isRequired())
                .defaultValue(entity.getDefaultValue())
                .description(entity.getDescription())
                .fieldOrder(entity.getFieldOrder())
                .isActive(entity.isActive())
                .standardFieldId(entity.getStandardField() != null ? entity.getStandardField().getStandardFieldId() : null)
                .standardFieldName(entity.getStandardField() != null ? entity.getStandardField().getStandardFieldId() : null)
                // [2026-04-20] 파서 연동
                .parserId(entity.getParser() != null ? entity.getParser().getParserId() : null)
                .parserName(entity.getParser() != null ? entity.getParser().getParserName() : null)
                .build();
    }

}

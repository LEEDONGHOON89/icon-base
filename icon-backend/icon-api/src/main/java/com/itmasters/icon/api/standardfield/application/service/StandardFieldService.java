package com.itmasters.icon.api.standardfield.application.service;

import com.itmasters.icon.api.standardfield.adapter.in.web.dto.StandardFieldDto;
import com.itmasters.icon.api.standardfield.adapter.out.persistence.mapper.StandardFieldMapper;
import com.itmasters.icon.api.standardfield.application.port.out.StandardFieldRepository;
import com.itmasters.icon.api.standardfield.domain.StandardField;
import com.itmasters.icon.common.domain.rule.FieldCategory;
import com.itmasters.icon.common.domain.type.FieldDataType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StandardFieldService {
    
    private final StandardFieldRepository standardFieldRepository;
    private final StandardFieldMapper standardFieldMapper;
    
    /**
     * 모든 표준 필드 조회
     */
    public List<StandardFieldDto.Response> getAllStandardFields() {
        List<StandardField> fields = standardFieldRepository.findAll();
        return fields.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 카테고리별 표준 필드 조회
     */
    public Map<FieldCategory, List<StandardFieldDto.Response>> getStandardFieldsByCategory() {
        List<StandardField> fields = standardFieldRepository.findAll();
        return fields.stream()
                .collect(Collectors.groupingBy(
                        StandardField::getCategory,
                        Collectors.mapping(this::toDto, Collectors.toList())
                ));
    }
    
    /**
     * 표준 필드 검색
     */
    public List<StandardFieldDto.Response> searchStandardFields(String keyword) {
        List<StandardField> fields = standardFieldRepository.searchByKeyword(keyword);
        return fields.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 표준 필드 조회
     */
    public StandardFieldDto.Response getStandardField(String fieldId) {
        StandardField field = standardFieldRepository.findById(fieldId)
                .orElseThrow(() -> new IllegalArgumentException("표준 필드를 찾을 수 없습니다: " + fieldId));
        return toDto(field);
    }
    
    /**
     * 데이터 타입별 표준 필드 조회
     */
    public List<StandardFieldDto.Response> getStandardFieldsByDataType(String dataType) {
        FieldDataType fieldDataType = FieldDataType.fromString(dataType);
        List<StandardField> fields = standardFieldRepository.findByDataType(fieldDataType);
        return fields.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * StandardField를 DTO로 변환
     */
    private StandardFieldDto.Response toDto(StandardField field) {
        return StandardFieldDto.Response.builder()
                .fieldId(field.getFieldId())
                .fieldName(field.getFieldName())
                .displayName(field.getDisplayName())
                .description(field.getDescription())
                .category(field.getCategory())
                .dataType(field.getDataType())
                .isActive(field.getIsActive())
                .build();
    }
}
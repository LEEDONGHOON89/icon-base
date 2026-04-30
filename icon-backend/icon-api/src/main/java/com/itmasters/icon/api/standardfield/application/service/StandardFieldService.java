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
    
    // [2026-04-20] 표준 필드 생성
    @Transactional
    public StandardFieldDto.Response createStandardField(StandardFieldDto.Create request) {
        if (standardFieldRepository.existsById(request.getFieldName())) {
            throw new IllegalArgumentException("이미 존재하는 필드 ID입니다: " + request.getFieldName());
        }

        String displayName = (request.getDisplayName() != null && !request.getDisplayName().isBlank())
                ? request.getDisplayName() : request.getFieldName();

        StandardField field = StandardField.createWithDetails(
                request.getFieldName(),
                request.getCategory(),
                displayName,
                request.getDataType(),
                request.getDescription()
        );
        field.assignId(request.getFieldName());

        StandardField saved = standardFieldRepository.save(field);
        return toDto(saved);
    }

    // [2026-04-20] 표준 필드 수정
    @Transactional
    public StandardFieldDto.Response updateStandardField(String fieldId, StandardFieldDto.Update request) {
        StandardField field = standardFieldRepository.findById(fieldId)
                .orElseThrow(() -> new IllegalArgumentException("표준 필드를 찾을 수 없습니다: " + fieldId));

        field.updateFieldInfo(request.getDisplayName(), request.getDescription());

        if (request.getCategory() != null) {
            field.updateCategory(request.getCategory());
        }
        if (request.getDataType() != null) {
            field.updateDataType(request.getDataType());
        }
        if (Boolean.TRUE.equals(request.getIsActive())) {
            field.activate();
        } else if (Boolean.FALSE.equals(request.getIsActive())) {
            field.deactivate();
        }

        StandardField saved = standardFieldRepository.save(field);
        return toDto(saved);
    }

    // [2026-04-20] 표준 필드 삭제
    @Transactional
    public void deleteStandardField(String fieldId) {
        if (!standardFieldRepository.existsById(fieldId)) {
            throw new IllegalArgumentException("표준 필드를 찾을 수 없습니다: " + fieldId);
        }
        standardFieldRepository.deleteById(fieldId);
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
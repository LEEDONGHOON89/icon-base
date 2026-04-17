package com.itmasters.icon.api.standardfield.adapter.out.persistence.mapper;

import com.itmasters.icon.common.domain.EntityType;
import com.itmasters.icon.common.domain.IdGenerator;
import com.itmasters.icon.api.standardfield.adapter.out.persistence.entity.StandardFieldEntity;
import com.itmasters.icon.api.standardfield.domain.StandardField;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

/**
 * 표준 필드 엔티티-도메인 매퍼
 */
@Component
@RequiredArgsConstructor
public class StandardFieldMapper {
    
    private final IdGenerator idGenerator;
    private final ModelMapper modelMapper;
    
    
    /**
     * 도메인을 엔티티로 변환
     */
    public StandardFieldEntity toEntity(StandardField domain) {
        if (domain == null) return null;
        
        if (domain.getFieldId() == null) {
            domain.assignId(idGenerator.generateId(EntityType.STANDARD_FIELD));
        }
        
        StandardFieldEntity entity = modelMapper.map(domain, StandardFieldEntity.class);
        // 필드명이 다르므로 수동 매핑
        entity.setStandardFieldId(domain.getFieldId());
        return entity;
    }
    
    /**
     * 엔티티를 도메인으로 변환
     */
    public StandardField toDomain(StandardFieldEntity entity) {
        if (entity == null) return null;

        // Entity에 fieldName이 없으므로 standardFieldId를 fieldName으로도 사용
        StandardField domain = StandardField.builder()
                .fieldName(entity.getStandardFieldId())  // standardFieldId를 fieldName으로 사용
                .fieldCategory(entity.getCategory())
                .displayName(entity.getDisplayName())
                .dataType(entity.getDataType())
                .description(entity.getDescription())
                .build();

        // fieldId 할당
        if (entity.getStandardFieldId() != null) {
            domain.assignId(entity.getStandardFieldId());
        }

        // isActive 반영
        if (entity.getIsActive() != null && !entity.getIsActive()) {
            domain.deactivate();
        }

        return domain;
    }
}

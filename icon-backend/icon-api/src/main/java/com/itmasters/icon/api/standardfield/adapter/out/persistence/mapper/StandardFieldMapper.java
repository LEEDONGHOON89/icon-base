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

        // [2026-04-20] modelMapper 대신 명시적 매핑 (fieldCategory→category 등 이름 불일치 방지)
        StandardFieldEntity entity = new StandardFieldEntity();
        entity.setStandardFieldId(domain.getFieldId());
        entity.setCategory(domain.getCategory());
        entity.setDisplayName(domain.getDisplayName());
        entity.setDataType(domain.getDataType());
        entity.setDescription(domain.getDescription());
        entity.setIsActive(domain.getIsActive() != null ? domain.getIsActive() : true);
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

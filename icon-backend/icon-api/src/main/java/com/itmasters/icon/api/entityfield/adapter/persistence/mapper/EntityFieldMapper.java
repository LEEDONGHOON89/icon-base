package com.itmasters.icon.api.entityfield.adapter.persistence.mapper;

import com.itmasters.icon.api.entityfield.adapter.persistence.entity.EntityFieldEntity;
import com.itmasters.icon.api.entityfield.domain.EntityField;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

/**
 * EntityField 도메인 ↔ Entity 변환 매퍼
 */
@Component
@RequiredArgsConstructor
public class EntityFieldMapper {
    private final ModelMapper modelMapper;

    public EntityFieldEntity toEntity(EntityField field) {
        if (field == null) {
            return null;
        }
        return modelMapper.map(field, EntityFieldEntity.class);
    }

    public EntityField toDomain(EntityFieldEntity entity) {
        if (entity == null) {
            return null;
        }
        return modelMapper.map(entity, EntityField.class);
    }
}

package com.itmasters.icon.api.entityattribute.adapter.out.persistence.repository;

import com.itmasters.icon.api.entityattribute.adapter.out.persistence.entity.ApiEntityAttributeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * entity_attributes 조회를 위한 비즈니스 Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class EntityAttributeReadRepositoryImpl implements EntityAttributeReadRepository {

    private final EntityAttributeJpaRepository jpaRepository;

    @Override
    public Optional<ApiEntityAttributeEntity> findByEntityTypeAndId(String entityType, String entityId) {
        return jpaRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    @Override
    public Optional<Map<String, Object>> findAttributesByEntity(String entityType, String entityId) {
        return jpaRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .map(ApiEntityAttributeEntity::getAttributes);
    }

    @Override
    public List<ApiEntityAttributeEntity> findByEntityType(String entityType) {
        return jpaRepository.findByEntityType(entityType);
    }

    @Override
    public List<ApiEntityAttributeEntity> findBatch(String entityType, List<String> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByEntityTypeAndEntityIdIn(entityType, entityIds);
    }
}

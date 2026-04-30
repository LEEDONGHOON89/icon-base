package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityAttributeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * EntityAttributeRepository 구현체
 * 
 * JpaEntityAttributeRepository를 사용하여 실제 데이터 접근 수행
 */
@Repository
@RequiredArgsConstructor
public class EntityAttributeRepositoryImpl implements EntityAttributeRepository {

    private final JpaEntityAttributeRepository jpaRepository;

    @Override
    public Optional<EntityAttributeEntity> findByEntityTypeAndEntityId(String entityType, String entityId) {
        return jpaRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    @Override
    public List<EntityAttributeEntity> findByEntityType(String entityType) {
        return jpaRepository.findByEntityType(entityType);
    }

    @Override
    public Optional<EntityAttributeEntity> findEntityByEntityTypeAndCustomerId(String entityType, String customerId) {
        return jpaRepository.findEntityByEntityTypeAndCustomerId(entityType, customerId);
    }

    @Override
    public EntityAttributeEntity save(EntityAttributeEntity entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public List<EntityAttributeEntity> saveAll(List<EntityAttributeEntity> entities) {
        return jpaRepository.saveAll(entities);
    }

    @Override
    public void delete(EntityAttributeEntity entity) {
        jpaRepository.delete(entity);
    }
}

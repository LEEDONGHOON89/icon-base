package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityAttributeEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 엔티티 속성 Repository
 */
public interface EntityAttributeRepository {

    /**
     * 엔티티 타입과 ID로 최신 속성 조회
     */
    Optional<EntityAttributeEntity> findByEntityTypeAndEntityId(String entityType, String entityId);

    /**
     * 엔티티 타입과 ID로 속성 맵 반환 (편의 메서드)
     */
    default Optional<Map<String, Object>> findAttributesByEntity(String entityType, String entityId) {
        return findByEntityTypeAndEntityId(entityType, entityId)
                .map(EntityAttributeEntity::getAttributes);
    }

    /**
     * 엔티티 타입과 customer_id로 속성 맵 반환
     * (ACCOUNT 엔티티를 customer_id로 검색할 때 사용)
     */
    default Optional<Map<String, Object>> findAttributesByEntityTypeAndCustomerId(String entityType, String customerId) {
        return findEntityByEntityTypeAndCustomerId(entityType, customerId)
                .map(EntityAttributeEntity::getAttributes);
    }

    /**
     * 엔티티 타입과 customer_id로 엔티티 조회
     */
    Optional<EntityAttributeEntity> findEntityByEntityTypeAndCustomerId(String entityType, String customerId);

    /**
     * 특정 엔티티 타입의 모든 속성 조회
     */
    List<EntityAttributeEntity> findByEntityType(String entityType);

    /**
     * 엔티티 속성 저장 (upsert)
     */
    EntityAttributeEntity save(EntityAttributeEntity entity);

    /**
     * 배치 저장
     */
    List<EntityAttributeEntity> saveAll(List<EntityAttributeEntity> entities);

    /**
     * 엔티티 속성 삭제
     */
    void delete(EntityAttributeEntity entity);
}

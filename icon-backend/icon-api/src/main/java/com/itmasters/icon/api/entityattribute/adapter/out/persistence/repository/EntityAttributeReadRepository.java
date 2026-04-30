package com.itmasters.icon.api.entityattribute.adapter.out.persistence.repository;

import com.itmasters.icon.api.entityattribute.adapter.out.persistence.entity.ApiEntityAttributeEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * entity_attributes 조회를 위한 비즈니스 Repository 인터페이스
 */
public interface EntityAttributeReadRepository {

    /**
     * 엔티티 타입과 ID로 entity_attributes 조회
     *
     * @param entityType 엔티티 타입 (CUSTOMER, ACCOUNT, DEVICE 등)
     * @param entityId   엔티티 ID (CUS001, ACC001 등)
     * @return ApiEntityAttributeEntity (없으면 Optional.empty())
     */
    Optional<ApiEntityAttributeEntity> findByEntityTypeAndId(String entityType, String entityId);

    /**
     * 엔티티 타입과 ID로 attributes만 조회
     *
     * @param entityType 엔티티 타입
     * @param entityId   엔티티 ID
     * @return attributes Map (없으면 Optional.empty())
     */
    Optional<Map<String, Object>> findAttributesByEntity(String entityType, String entityId);

    /**
     * 특정 타입의 모든 entity_attributes 조회
     *
     * @param entityType 엔티티 타입
     * @return 해당 타입의 모든 entity_attributes 목록
     */
    List<ApiEntityAttributeEntity> findByEntityType(String entityType);

    /**
     * 특정 타입과 ID 목록으로 배치 조회
     *
     * @param entityType 엔티티 타입
     * @param entityIds  엔티티 ID 목록
     * @return 해당하는 entity_attributes 목록
     */
    List<ApiEntityAttributeEntity> findBatch(String entityType, List<String> entityIds);
}

package com.itmasters.icon.api.entityattribute.adapter.out.persistence.repository;

import com.itmasters.icon.api.entityattribute.adapter.out.persistence.entity.ApiEntityAttributeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * entity_attributes 테이블에 대한 JPA Repository
 */
@Repository
public interface EntityAttributeJpaRepository extends JpaRepository<ApiEntityAttributeEntity, Long> {

    /**
     * 엔티티 타입과 ID로 entity_attributes 조회
     *
     * @param entityType 엔티티 타입 (CUSTOMER, ACCOUNT, DEVICE 등)
     * @param entityId   엔티티 ID (CUS001, ACC001 등)
     * @return ApiEntityAttributeEntity
     */
    Optional<ApiEntityAttributeEntity> findByEntityTypeAndEntityId(String entityType, String entityId);

    /**
     * 엔티티 ID로만 조회 (타입 불명시 - group_key로 조회 시 사용)
     *
     * @param entityId 엔티티 ID
     * @return ApiEntityAttributeEntity (여러 개일 경우 첫 번째)
     */
    Optional<ApiEntityAttributeEntity> findFirstByEntityId(String entityId);

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
    List<ApiEntityAttributeEntity> findByEntityTypeAndEntityIdIn(String entityType, List<String> entityIds);
}

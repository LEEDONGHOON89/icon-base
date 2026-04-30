package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityAttributeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spring Data JPA Repository for EntityAttributeEntity
 *
 * Provides CRUD operations for entity_attributes table
 * QueryDSL 기반 복잡한 쿼리는 JpaEntityAttributeRepositoryCustom에서 구현
 */
@Repository
public interface JpaEntityAttributeRepository extends JpaRepository<EntityAttributeEntity, Long>, JpaEntityAttributeRepositoryCustom {

    /**
     * 엔티티 타입과 ID로 속성 조회
     * 
     * @param entityType 엔티티 타입 (CUSTOMER, ACCOUNT, DEVICE)
     * @param entityId 엔티티 ID
     * @return Optional<EntityAttributeEntity>
     */
    Optional<EntityAttributeEntity> findByEntityTypeAndEntityId(String entityType, String entityId);

    /**
     * 엔티티의 속성 맵만 조회 - QueryDSL로 구현
     */
    // @Query 제거됨 - JpaEntityAttributeRepositoryCustom 인터페이스에서 선언, JpaEntityAttributeRepositoryCustomImpl에서 구현
    // Optional<Map<String, Object>> findAttributesByEntity(String entityType, String entityId);

    /**
     * 엔티티 타입별 모든 속성 조회
     * 
     * @param entityType 엔티티 타입
     * @return 해당 타입의 모든 엔티티 속성 목록
     */
    List<EntityAttributeEntity> findByEntityType(String entityType);

    /**
     * 엔티티 속성 저장 또는 업데이트
     * 
     * @param entity 저장할 엔티티 속성
     * @return 저장된 엔티티
     */
    @Override
    <S extends EntityAttributeEntity> S save(S entity);

    /**
     * 엔티티 타입과 ID로 삭제
     *
     * @param entityType 엔티티 타입
     * @param entityId 엔티티 ID
     */
    void deleteByEntityTypeAndEntityId(String entityType, String entityId);

    /**
     * 엔티티 타입과 attributes 내 customer_id로 엔티티 조회 - Native SQL로 구현
     * (ACCOUNT 엔티티를 customer_id로 검색할 때 사용)
     *
     * Note: PostgreSQL JSONB 연산자 사용으로 Native SQL 유지
     */
    // @Query 제거됨 - JpaEntityAttributeRepositoryCustom 인터페이스에서 선언, JpaEntityAttributeRepositoryCustomImpl에서 Native SQL로 구현
    // Optional<EntityAttributeEntity> findEntityByEntityTypeAndCustomerId(String entityType, String customerId);
}

package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityAttributeEntity;

import java.util.Map;
import java.util.Optional;

/**
 * EntityAttribute JPA 커스텀 Repository 인터페이스
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
public interface JpaEntityAttributeRepositoryCustom {

    /**
     * 엔티티의 속성 맵만 조회
     */
    Optional<Map<String, Object>> findAttributesByEntity(String entityType, String entityId);

    /**
     * 엔티티 타입과 attributes 내 customer_id로 엔티티 조회
     * (ACCOUNT 엔티티를 customer_id로 검색할 때 사용)
     *
     * Note: PostgreSQL JSONB 연산자를 사용하므로 Native SQL 유지
     */
    Optional<EntityAttributeEntity> findEntityByEntityTypeAndCustomerId(String entityType, String customerId);
}

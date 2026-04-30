package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityAttributeEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QEntityAttributeEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.Optional;

/**
 * EntityAttribute JPA 커스텀 Repository 구현체
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
@Repository
@RequiredArgsConstructor
public class JpaEntityAttributeRepositoryCustomImpl implements JpaEntityAttributeRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public Optional<Map<String, Object>> findAttributesByEntity(String entityType, String entityId) {
        QEntityAttributeEntity q = QEntityAttributeEntity.entityAttributeEntity;

        Map<String, Object> result = queryFactory
                .select(q.attributes)
                .from(q)
                .where(
                        q.entityType.eq(entityType),
                        q.entityId.eq(entityId)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<EntityAttributeEntity> findEntityByEntityTypeAndCustomerId(String entityType, String customerId) {
        // PostgreSQL JSONB 연산자를 사용하므로 Native SQL 유지
        String sql = "SELECT * FROM entity_attributes " +
                     "WHERE entity_type = :entityType " +
                     "AND attributes->>'customer_id' = :customerId " +
                     "LIMIT 1";

        try {
            EntityAttributeEntity result = (EntityAttributeEntity) entityManager
                    .createNativeQuery(sql, EntityAttributeEntity.class)
                    .setParameter("entityType", entityType)
                    .setParameter("customerId", customerId)
                    .getSingleResult();
            return Optional.of(result);
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }
}

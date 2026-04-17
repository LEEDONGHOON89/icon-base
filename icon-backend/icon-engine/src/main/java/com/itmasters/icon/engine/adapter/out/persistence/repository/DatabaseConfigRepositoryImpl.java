package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDsDatabaseConfigEntity;
import com.itmasters.icon.engine.datasource.repository.DatabaseConfigRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * [2026-03-13] DatabaseConfigRepository 구현체
 * EntityManager + JPQL 사용 (QueryDSL Q클래스 빌드 전 호환)
 */
@Repository
@RequiredArgsConstructor
public class DatabaseConfigRepositoryImpl implements DatabaseConfigRepository {

    private final EntityManager em;

    @Override
    public Optional<EngineDsDatabaseConfigEntity> findByDataSourceId(String dataSourceId) {
        List<EngineDsDatabaseConfigEntity> result = em.createQuery(
                        "SELECT e FROM EngineDsDatabaseConfigEntity e" +
                        " WHERE e.dataSourceId = :dsId AND e.isActive = true",
                        EngineDsDatabaseConfigEntity.class)
                .setParameter("dsId", dataSourceId)
                .setMaxResults(1)
                .getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public EngineDsDatabaseConfigEntity save(EngineDsDatabaseConfigEntity entity) {
        return em.merge(entity);
    }
}

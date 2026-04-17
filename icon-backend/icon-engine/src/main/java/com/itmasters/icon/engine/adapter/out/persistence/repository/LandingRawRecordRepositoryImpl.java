package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.LandingRawRecordEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QLandingRawRecordEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@Transactional
@RequiredArgsConstructor
public class LandingRawRecordRepositoryImpl implements LandingRawRecordRepository {

    private final EntityManager entityManager;
    private final JPAQueryFactory queryFactory;

    @Override
    public LandingRawRecordEntity save(LandingRawRecordEntity entity) {
        if (entity.getLandingRecordId() == null) {
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);
    }

    @Override
    public List<LandingRawRecordEntity> saveAll(List<LandingRawRecordEntity> entities) {
        for (LandingRawRecordEntity entity : entities) {
            if (entity.getLandingRecordId() == null) {
                entityManager.persist(entity);
            } else {
                entityManager.merge(entity);
            }
        }
        entityManager.flush();
        return entities;
    }

    @Override
    public Optional<LandingRawRecordEntity> findById(Long landingRecordId) {
        if (landingRecordId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(LandingRawRecordEntity.class, landingRecordId));
    }

    @Override
    public List<LandingRawRecordEntity> findByExecDsMpIdOrderByRowIndex(Long execDsMpId) {
        if (execDsMpId == null) {
            return List.of();
        }

        QLandingRawRecordEntity q = QLandingRawRecordEntity.landingRawRecordEntity;

        return queryFactory
                .selectFrom(q)
                .where(q.execDsMp.execDsMpId.eq(execDsMpId))
                .orderBy(q.rowIndex.asc())
                .fetch();
    }

    @Override
    public List<LandingRawRecordEntity> findByExecDsMpIdAndIngestionStatus(Long execDsMpId,
                                                                           LandingRawRecordEntity.IngestionStatus status) {
        if (execDsMpId == null) {
            return List.of();
        }

        QLandingRawRecordEntity q = QLandingRawRecordEntity.landingRawRecordEntity;

        return queryFactory
                .selectFrom(q)
                .where(
                        q.execDsMp.execDsMpId.eq(execDsMpId),
                        q.ingestionStatus.eq(status)
                )
                .orderBy(q.rowIndex.asc())
                .fetch();
    }
}

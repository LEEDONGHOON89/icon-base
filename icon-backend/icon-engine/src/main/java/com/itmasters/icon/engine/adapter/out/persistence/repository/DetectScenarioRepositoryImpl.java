package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectScenarioEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QDetectScenarioEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository("engineDetectScenarioRepositoryImpl")
@RequiredArgsConstructor
public class DetectScenarioRepositoryImpl implements DetectScenarioRepository {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public DetectScenarioEntity save(DetectScenarioEntity entity) {
        if (entity.getDetectScenarioId() == null) {
            entityManager.persist(entity);
            return entity;
        } else {
            return entityManager.merge(entity);
        }
    }

    @Override
    public boolean existsRecentByKeyAndScenario(String groupKey, String scenarioId, LocalDateTime since) {
        QDetectScenarioEntity q = QDetectScenarioEntity.detectScenarioEntity;

        Long count = queryFactory
                .select(q.count())
                .from(q)
                .where(
                        q.groupKey.eq(groupKey),
                        q.scenarioId.eq(scenarioId),
                        q.detectedDt.goe(since)
                )
                .fetchOne();

        return count != null && count > 0;
    }

    @Override
    public List<DetectScenarioEntity> findRecentByKey(String groupKey, int limit) {
        QDetectScenarioEntity q = QDetectScenarioEntity.detectScenarioEntity;

        return queryFactory
                .selectFrom(q)
                .where(q.groupKey.eq(groupKey))
                .orderBy(q.detectedDt.desc())
                .limit(Math.max(1, limit))
                .fetch();
    }


    
    @Override
    public List<DetectScenarioEntity> findByMappedStorageId(Long mappedStorageId) {
        QDetectScenarioEntity q = QDetectScenarioEntity.detectScenarioEntity;

        return queryFactory
                .selectFrom(q)
                .where(q.mappedStorageId.eq(mappedStorageId))
                .orderBy(q.detectedDt.desc())
                .fetch();
    }

    @Override
    public List<DetectScenarioEntity> findByTransactionId(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            return List.of();
        }

        QDetectScenarioEntity q = QDetectScenarioEntity.detectScenarioEntity;

        return queryFactory
                .selectFrom(q)
                .where(q.transactionId.eq(transactionId))
                .orderBy(q.detectedDt.asc())
                .fetch();
    }

    @Override
    public Integer countByTransactionId(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            return 0;
        }

        QDetectScenarioEntity q = QDetectScenarioEntity.detectScenarioEntity;

        Long count = queryFactory
                .select(q.count())
                .from(q)
                .where(q.transactionId.eq(transactionId))
                .fetchOne();

        return count != null ? count.intValue() : 0;
    }
}

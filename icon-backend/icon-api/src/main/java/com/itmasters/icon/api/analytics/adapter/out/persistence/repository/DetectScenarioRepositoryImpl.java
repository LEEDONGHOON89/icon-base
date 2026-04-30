package com.itmasters.icon.api.analytics.adapter.out.persistence.repository;

import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiDetectScenarioEntity;
import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.QApiDetectScenarioEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository("apiDetectScenarioRepositoryImpl")
@RequiredArgsConstructor
public class DetectScenarioRepositoryImpl implements DetectScenarioRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ApiDetectScenarioEntity> findRecentByGroupKey(String groupKey, int limit) {
        QApiDetectScenarioEntity q = QApiDetectScenarioEntity.apiDetectScenarioEntity;
        return queryFactory.selectFrom(q)
                .where(q.groupKey.eq(groupKey))
                .orderBy(q.detectedDt.desc())
                .limit(limit > 0 ? limit : 50)
                .fetch();
    }

    @Override
    public List<ApiDetectScenarioEntity> findByGroupKeyBetween(String groupKey, LocalDateTime startInclusive, LocalDateTime endInclusive) {
        QApiDetectScenarioEntity q = QApiDetectScenarioEntity.apiDetectScenarioEntity;
        return queryFactory.selectFrom(q)
                .where(q.groupKey.eq(groupKey)
                        .and(q.detectedDt.goe(startInclusive))
                        .and(q.detectedDt.loe(endInclusive)))
                .orderBy(q.detectedDt.desc())
                .fetch();
    }

    @Override
    public List<ApiDetectScenarioEntity> findRecent(int limit) {
        QApiDetectScenarioEntity q = QApiDetectScenarioEntity.apiDetectScenarioEntity;
        return queryFactory.selectFrom(q)
                .orderBy(q.detectedDt.desc())
                .limit(limit > 0 ? limit : 50)
                .fetch();
    }

    @Override
    public List<ApiDetectScenarioEntity> findBetween(LocalDateTime startInclusive, LocalDateTime endInclusive, int limit) {
        QApiDetectScenarioEntity q = QApiDetectScenarioEntity.apiDetectScenarioEntity;
        return queryFactory.selectFrom(q)
                .where(q.detectedDt.goe(startInclusive)
                        .and(q.detectedDt.loe(endInclusive)))
                .orderBy(q.detectedDt.desc())
                .limit(limit > 0 ? limit : 200)
                .fetch();
    }

    @Override
    public ApiDetectScenarioEntity findOne(String groupKey, String scenarioId, LocalDateTime detectedAt) {
        QApiDetectScenarioEntity q = QApiDetectScenarioEntity.apiDetectScenarioEntity;
        return queryFactory.selectFrom(q)
                .where(q.groupKey.eq(groupKey)
                        .and(q.scenarioId.eq(scenarioId))
                        .and(q.detectedDt.eq(detectedAt)))
                .fetchOne();
    }


    @Override
    public List<ApiDetectScenarioEntity> findByExecIdAndGroupKeys(Long mappedStorageId, java.util.Collection<String> groupKeys) {
        if (groupKeys == null || groupKeys.isEmpty()) {
            return List.of();
        }
        QApiDetectScenarioEntity q = QApiDetectScenarioEntity.apiDetectScenarioEntity;
        return queryFactory.selectFrom(q)
                .where(q.mappedStorageId.eq(mappedStorageId)
                        .and(q.groupKey.in(groupKeys)))
                .orderBy(q.detectedDt.desc())
                .fetch();
    }

    @Override
    public long countByExecIdAndGroupKeys(Long mappedStorageId, java.util.Collection<String> groupKeys) {
        if (groupKeys == null || groupKeys.isEmpty()) {
            return 0L;
        }
        QApiDetectScenarioEntity q = QApiDetectScenarioEntity.apiDetectScenarioEntity;
        Long count = queryFactory.select(q.detectScenarioId.count())
                .from(q)
                .where(q.mappedStorageId.eq(mappedStorageId)
                        .and(q.groupKey.in(groupKeys)))
                .fetchOne();
        return count != null ? count : 0L;
    }
}

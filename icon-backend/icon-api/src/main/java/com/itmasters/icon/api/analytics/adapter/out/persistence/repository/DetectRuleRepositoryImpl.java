package com.itmasters.icon.api.analytics.adapter.out.persistence.repository;

import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiDetectRuleEntity;
import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.QApiDetectRuleEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository("apiDetectRuleRepositoryImpl")
@RequiredArgsConstructor
public class DetectRuleRepositoryImpl implements DetectRuleRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ApiDetectRuleEntity> findByGroupKeyBetween(String groupKey, LocalDateTime startInclusive, LocalDateTime endInclusive) {
        QApiDetectRuleEntity q = QApiDetectRuleEntity.apiDetectRuleEntity;
        return queryFactory.selectFrom(q)
                .where(q.groupKey.eq(groupKey)
                        .and(q.detectedDt.goe(startInclusive))
                        .and(q.detectedDt.loe(endInclusive)))
                .orderBy(q.detectedDt.desc())
                .fetch();
    }

    @Override
    public List<ApiDetectRuleEntity> findBetween(LocalDateTime startInclusive, LocalDateTime endInclusive, int limit) {
        QApiDetectRuleEntity q = QApiDetectRuleEntity.apiDetectRuleEntity;
        return queryFactory.selectFrom(q)
                .where(q.detectedDt.goe(startInclusive)
                        .and(q.detectedDt.loe(endInclusive)))
                .orderBy(q.detectedDt.desc())
                .limit(limit > 0 ? limit : 200)
                .fetch();
    }

    @Override
    public ApiDetectRuleEntity findOne(String groupKey, String ruleId, LocalDateTime detectedAt) {
        QApiDetectRuleEntity q = QApiDetectRuleEntity.apiDetectRuleEntity;
        return queryFactory.selectFrom(q)
                .where(q.groupKey.eq(groupKey)
                        .and(q.ruleId.eq(ruleId))
                        .and(q.detectedDt.eq(detectedAt)))
                .fetchOne();
    }

    @Override
    public List<ApiDetectRuleEntity> findByExecIdAndRuleIds(Long mappedStorageId, List<String> ruleIds) {
        if (ruleIds == null || ruleIds.isEmpty()) return Collections.emptyList();
        QApiDetectRuleEntity q = QApiDetectRuleEntity.apiDetectRuleEntity;
        return queryFactory.selectFrom(q)
                .where(q.mappedStorageId.eq(mappedStorageId)
                        .and(q.ruleId.in(ruleIds)))
                .orderBy(q.detectedDt.desc())
                .fetch();
    }

    @Override
    public List<ApiDetectRuleEntity> findByGroupKeyAndRuleIds(String groupKey, List<String> ruleIds) {
        if (ruleIds == null || ruleIds.isEmpty()) return Collections.emptyList();
        QApiDetectRuleEntity q = QApiDetectRuleEntity.apiDetectRuleEntity;
        return queryFactory.selectFrom(q)
                .where(q.groupKey.eq(groupKey)
                        .and(q.ruleId.in(ruleIds)))
                .orderBy(q.detectedDt.desc())
                .fetch();
    }


    @Override
    public List<ApiDetectRuleEntity> findByMappedStorageId(Long mappedStorageId) {
        if (mappedStorageId == null) {
            return List.of();
        }
        QApiDetectRuleEntity q = QApiDetectRuleEntity.apiDetectRuleEntity;
        return queryFactory.selectFrom(q)
                .where(q.mappedStorageId.eq(mappedStorageId))
                .orderBy(q.detectedDt.desc(), q.detectRuleId.desc())
                .fetch();
    }

    @Override
    public Map<Long, Long> countByMappedStorageIds(Collection<Long> mappedStorageIds) {
        if (mappedStorageIds == null || mappedStorageIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QApiDetectRuleEntity q = QApiDetectRuleEntity.apiDetectRuleEntity;
        List<com.querydsl.core.Tuple> rows = queryFactory.select(q.mappedStorageId, q.detectRuleId.count())
                .from(q)
                .where(q.mappedStorageId.in(mappedStorageIds))
                .groupBy(q.mappedStorageId)
                .fetch();

        Map<Long, Long> result = new HashMap<>();
        for (com.querydsl.core.Tuple row : rows) {
            Long mappedId = row.get(q.mappedStorageId);
            Long count = row.get(q.detectRuleId.count());
            if (mappedId != null && count != null) {
                result.put(mappedId, count);
            }
        }
        return result;
    }

    @Override
    public Map<Long, Set<String>> groupKeysByMappedStorageIds(Collection<Long> mappedStorageIds) {
        if (mappedStorageIds == null || mappedStorageIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QApiDetectRuleEntity q = QApiDetectRuleEntity.apiDetectRuleEntity;
        List<com.querydsl.core.Tuple> rows = queryFactory.select(q.mappedStorageId, q.groupKey)
                .from(q)
                .where(q.mappedStorageId.in(mappedStorageIds)
                        .and(q.groupKey.isNotNull()))
                .distinct()
                .fetch();

        Map<Long, Set<String>> result = new HashMap<>();
        for (com.querydsl.core.Tuple row : rows) {
            Long mappedId = row.get(q.mappedStorageId);
            String groupKey = row.get(q.groupKey);
            if (mappedId == null || groupKey == null) {
                continue;
            }
            result.computeIfAbsent(mappedId, k -> {
                Set<String> set = new LinkedHashSet<>();
                set.add(groupKey);
                return set;
            });
        }
        return result;
    }
}

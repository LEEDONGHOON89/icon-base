package com.itmasters.icon.api.analytics.adapter.out.persistence.repository;

import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiDetectRuleEntity;
import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.QApiDetectRuleEntity;
import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.QApiLandingRawRecordEntity;
import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.QApiMappedDataStorageEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DetectRuleReadRepositoryImpl implements DetectRuleReadRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ApiDetectRuleEntity> findByExecDsMpId(Long execDsMpId) {
        QApiDetectRuleEntity rule = QApiDetectRuleEntity.apiDetectRuleEntity;
        QApiMappedDataStorageEntity mapped = QApiMappedDataStorageEntity.apiMappedDataStorageEntity;
        QApiLandingRawRecordEntity landing = QApiLandingRawRecordEntity.apiLandingRawRecordEntity;

        return queryFactory.selectFrom(rule)
                .join(mapped).on(rule.mappedStorageId.eq(mapped.mappedStorageId))
                .join(landing).on(mapped.landingRecordId.eq(landing.landingRecordId))
                .where(landing.execDsMp.execDsMpId.eq(execDsMpId))
                .orderBy(rule.detectedDt.desc(), rule.detectRuleId.desc())
                .fetch();
    }

    @Override
    public long countByExecDsMpId(Long execDsMpId) {
        QApiDetectRuleEntity rule = QApiDetectRuleEntity.apiDetectRuleEntity;
        QApiMappedDataStorageEntity mapped = QApiMappedDataStorageEntity.apiMappedDataStorageEntity;
        QApiLandingRawRecordEntity landing = QApiLandingRawRecordEntity.apiLandingRawRecordEntity;

        Long count = queryFactory.select(rule.detectRuleId.count())
                .from(rule)
                .join(mapped).on(rule.mappedStorageId.eq(mapped.mappedStorageId))
                .join(landing).on(mapped.landingRecordId.eq(landing.landingRecordId))
                .where(landing.execDsMp.execDsMpId.eq(execDsMpId))
                .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public List<ApiDetectRuleEntity> findByMappedStorageId(Long mappedStorageId) {
        if (mappedStorageId == null) {
            return List.of();
        }
        QApiDetectRuleEntity rule = QApiDetectRuleEntity.apiDetectRuleEntity;
        return queryFactory.selectFrom(rule)
                .where(rule.mappedStorageId.eq(mappedStorageId))
                .orderBy(rule.detectedDt.desc(), rule.detectRuleId.desc())
                .fetch();
    }

    @Override
    public List<ApiDetectRuleEntity> findByMappedStorageIds(List<Long> mappedStorageIds) {
        if (mappedStorageIds == null || mappedStorageIds.isEmpty()) {
            return List.of();
        }
        QApiDetectRuleEntity rule = QApiDetectRuleEntity.apiDetectRuleEntity;
        return queryFactory.selectFrom(rule)
                .where(rule.mappedStorageId.in(mappedStorageIds))
                .fetch();
    }

    @Override
    public java.util.Map<Long, Long> countByMappedStorageIds(java.util.Collection<Long> mappedStorageIds) {
        if (mappedStorageIds == null || mappedStorageIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        QApiDetectRuleEntity rule = QApiDetectRuleEntity.apiDetectRuleEntity;
        List<com.querydsl.core.Tuple> rows = queryFactory.select(rule.mappedStorageId, rule.detectRuleId.count())
                .from(rule)
                .where(rule.mappedStorageId.in(mappedStorageIds))
                .groupBy(rule.mappedStorageId)
                .fetch();

        java.util.Map<Long, Long> result = new java.util.HashMap<>();
        for (com.querydsl.core.Tuple row : rows) {
            Long mappedId = row.get(rule.mappedStorageId);
            Long count = row.get(rule.detectRuleId.count());
            if (mappedId != null && count != null) {
                result.put(mappedId, count);
            }
        }
        return result;
    }

    @Override
    public java.util.Map<Long, java.util.Set<String>> groupKeysByMappedStorageIds(java.util.Collection<Long> mappedStorageIds) {
        if (mappedStorageIds == null || mappedStorageIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        QApiDetectRuleEntity rule = QApiDetectRuleEntity.apiDetectRuleEntity;
        List<com.querydsl.core.Tuple> rows = queryFactory.select(rule.mappedStorageId, rule.groupKey)
                .from(rule)
                .where(rule.mappedStorageId.in(mappedStorageIds)
                        .and(rule.groupKey.isNotNull()))
                .distinct()
                .fetch();

        java.util.Map<Long, java.util.Set<String>> result = new java.util.HashMap<>();
        for (com.querydsl.core.Tuple row : rows) {
            Long mappedId = row.get(rule.mappedStorageId);
            String groupKey = row.get(rule.groupKey);
            if (mappedId == null || groupKey == null) {
                continue;
            }
            result.computeIfAbsent(mappedId, k -> new java.util.LinkedHashSet<>()).add(groupKey);
        }
        return result;
    }

    @Override
    public List<ApiDetectRuleEntity> findByGroupKeyBetween(String groupKey, LocalDateTime startInclusive, LocalDateTime endInclusive) {
        QApiDetectRuleEntity rule = QApiDetectRuleEntity.apiDetectRuleEntity;
        return queryFactory.selectFrom(rule)
                .where(rule.groupKey.eq(groupKey)
                        .and(rule.detectedDt.goe(startInclusive))
                        .and(rule.detectedDt.loe(endInclusive)))
                .orderBy(rule.detectedDt.desc())
                .fetch();
    }
}

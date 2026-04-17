package com.itmasters.icon.api.analytics.adapter.out.persistence.repository;

import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiDetectRuleEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface DetectRuleRepository {
    List<ApiDetectRuleEntity> findByGroupKeyBetween(String groupKey, LocalDateTime startInclusive, LocalDateTime endInclusive);
    List<ApiDetectRuleEntity> findBetween(LocalDateTime startInclusive, LocalDateTime endInclusive, int limit);
    ApiDetectRuleEntity findOne(String groupKey, String ruleId, LocalDateTime anchor);
    List<ApiDetectRuleEntity> findByExecIdAndRuleIds(Long mappedStorageId, java.util.List<String> ruleIds);
    List<ApiDetectRuleEntity> findByGroupKeyAndRuleIds(String groupKey, java.util.List<String> ruleIds);

    List<ApiDetectRuleEntity> findByMappedStorageId(Long mappedStorageId);

    java.util.Map<Long, Long> countByMappedStorageIds(java.util.Collection<Long> mappedStorageIds);

    java.util.Map<Long, java.util.Set<String>> groupKeysByMappedStorageIds(java.util.Collection<Long> mappedStorageIds);
}

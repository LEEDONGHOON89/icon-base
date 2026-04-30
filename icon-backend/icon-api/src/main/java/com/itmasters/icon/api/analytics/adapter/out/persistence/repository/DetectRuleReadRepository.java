package com.itmasters.icon.api.analytics.adapter.out.persistence.repository;

import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiDetectRuleEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface DetectRuleReadRepository {

    List<ApiDetectRuleEntity> findByExecDsMpId(Long execDsMpId);

    long countByExecDsMpId(Long execDsMpId);

    List<ApiDetectRuleEntity> findByMappedStorageId(Long mappedStorageId);

    List<ApiDetectRuleEntity> findByMappedStorageIds(List<Long> mappedStorageIds);

    java.util.Map<Long, Long> countByMappedStorageIds(java.util.Collection<Long> mappedStorageIds);

    java.util.Map<Long, java.util.Set<String>> groupKeysByMappedStorageIds(java.util.Collection<Long> mappedStorageIds);

    List<ApiDetectRuleEntity> findByGroupKeyBetween(String groupKey, LocalDateTime startInclusive, LocalDateTime endInclusive);
}

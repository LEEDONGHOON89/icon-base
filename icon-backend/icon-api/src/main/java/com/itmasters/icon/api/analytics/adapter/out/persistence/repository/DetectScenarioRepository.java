package com.itmasters.icon.api.analytics.adapter.out.persistence.repository;

import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiDetectScenarioEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface DetectScenarioRepository {
    List<ApiDetectScenarioEntity> findRecentByGroupKey(String groupKey, int limit);
    List<ApiDetectScenarioEntity> findByGroupKeyBetween(String groupKey, LocalDateTime startInclusive, LocalDateTime endInclusive);
    List<ApiDetectScenarioEntity> findRecent(int limit);
    List<ApiDetectScenarioEntity> findBetween(LocalDateTime startInclusive, LocalDateTime endInclusive, int limit);
    ApiDetectScenarioEntity findOne(String groupKey, String scenarioId, LocalDateTime detectedAt);

    List<ApiDetectScenarioEntity> findByExecIdAndGroupKeys(Long mappedStorageId, java.util.Collection<String> groupKeys);

    long countByExecIdAndGroupKeys(Long mappedStorageId, java.util.Collection<String> groupKeys);
}

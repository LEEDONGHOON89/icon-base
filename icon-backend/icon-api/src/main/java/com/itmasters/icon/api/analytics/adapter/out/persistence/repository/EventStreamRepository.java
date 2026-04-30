package com.itmasters.icon.api.analytics.adapter.out.persistence.repository;

import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiEventStreamEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface EventStreamRepository {
    List<ApiEventStreamEntity> findByGroupKeyBetween(String groupKey, LocalDateTime startInclusive, LocalDateTime endInclusive, int limit);
}


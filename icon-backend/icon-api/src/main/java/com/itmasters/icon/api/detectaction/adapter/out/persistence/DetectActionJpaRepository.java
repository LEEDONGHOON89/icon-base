package com.itmasters.icon.api.detectaction.adapter.out.persistence;

import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiDetectActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 탐지 조치 JPA Repository
 */
public interface DetectActionJpaRepository extends JpaRepository<ApiDetectActionEntity, Long> {
}

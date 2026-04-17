package com.itmasters.icon.api.analytics.adapter.out.persistence.repository;

import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiLandingRawRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LandingRawRecordJpaRepository extends JpaRepository<ApiLandingRawRecordEntity, Long> {
}


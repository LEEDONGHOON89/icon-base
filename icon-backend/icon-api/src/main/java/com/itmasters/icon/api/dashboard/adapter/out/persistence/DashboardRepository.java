package com.itmasters.icon.api.dashboard.adapter.out.persistence;

import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiMappedDataStorageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 대시보드 JPA Repository
 * - 기본적인 CRUD만 제공
 * - 복잡한 통계 쿼리는 DashboardQueryRepository에서 처리
 */
@Repository
public interface DashboardRepository extends JpaRepository<ApiMappedDataStorageEntity, Long> {
}

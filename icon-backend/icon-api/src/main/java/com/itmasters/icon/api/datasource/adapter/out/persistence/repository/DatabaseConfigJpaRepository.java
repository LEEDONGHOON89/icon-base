package com.itmasters.icon.api.datasource.adapter.out.persistence.repository;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DatabaseConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatabaseConfigJpaRepository extends JpaRepository<DatabaseConfigEntity, String> {
    Optional<DatabaseConfigEntity> findByDataSourceId(String dataSourceId);
    List<DatabaseConfigEntity> findAllByDataSourceId(String dataSourceId);
    // [2026-04-21] 에이전트 ID로 모든 데이터베이스 설정 조회 (스냅샷 생성용)
    List<DatabaseConfigEntity> findAllByAgentId(String agentId);
}


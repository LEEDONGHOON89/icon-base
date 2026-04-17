package com.itmasters.icon.engine.datasource.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDsDatabaseConfigEntity;

import java.util.Optional;

/**
 * [2026-03-13] DATABASE 데이터소스 설정 Repository 포트
 */
public interface DatabaseConfigRepository {

    Optional<EngineDsDatabaseConfigEntity> findByDataSourceId(String dataSourceId);

    EngineDsDatabaseConfigEntity save(EngineDsDatabaseConfigEntity entity);
}

package com.itmasters.icon.engine.datasource.repository;


import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDsFileSystemConfigEntity;

import java.util.List;
import java.util.Optional;

/**
 * 파일 시스템 설정 Repository 포트 (엔티티 직접 반환)
 */
public interface FileSystemConfigRepository {

    EngineDsFileSystemConfigEntity save(EngineDsFileSystemConfigEntity config);

    Optional<EngineDsFileSystemConfigEntity> findById(String id);

    Optional<EngineDsFileSystemConfigEntity> findByDataSourceId(String dataSourceId);

    List<EngineDsFileSystemConfigEntity> findAllByDataSourceId(String dataSourceId);

    List<EngineDsFileSystemConfigEntity> findActiveConfigs();

    Optional<EngineDsFileSystemConfigEntity> findActiveByDataSourceId(String dataSourceId);

    void deleteById(String id);

    List<EngineDsFileSystemConfigEntity> findAll();
}

package com.itmasters.icon.api.datasource.application.port.out;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DataSourceEntity;
import com.itmasters.icon.common.domain.type.DataSourceType;

import java.util.List;
import java.util.Optional;

/**
 * DataSource Repository Port
 */
public interface DataSourceRepository {
    
    // 조회
    Optional<DataSourceEntity> findById(String id);
    List<DataSourceEntity> findAll();
    List<DataSourceEntity> findBySourceType(DataSourceType sourceType);
    Optional<DataSourceEntity> findByName(String name);
    
    // 저장
    DataSourceEntity save(DataSourceEntity dataSourceEntity);
    
    // 삭제
    void deleteById(String id);
    void delete(DataSourceEntity dataSourceEntity);
    
    // 카운트
    boolean existsById(String id);
    boolean existsByName(String name);
}
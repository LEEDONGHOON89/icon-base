package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.common.domain.type.DataSourceType;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDataSourceEntity;

import java.util.List;
import java.util.Optional;

/**
 * 룰 엔진용 데이터소스 레파지토리 인터페이스
 */
public interface EngineDataSourceRepository {
    
    /**
     * ID로 데이터소스 조회
     */
    Optional<EngineDataSourceEntity> findById(String dataSourceId);
    
    /**
     * 활성화된 데이터소스 목록 조회
     */
    List<EngineDataSourceEntity> findActiveDataSources();
    
    /**
     * 특정 타입의 활성화된 데이터소스 조회
     */
    List<EngineDataSourceEntity> findActiveDataSourcesByType(DataSourceType sourceType);
}
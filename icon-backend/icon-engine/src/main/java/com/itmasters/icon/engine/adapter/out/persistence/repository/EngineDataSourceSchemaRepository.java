package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDataSourceSchemaEntity;

import java.util.List;

/**
 * 룰 엔진용 데이터소스 스키마 레파지토리 인터페이스
 */
public interface EngineDataSourceSchemaRepository {
    
    /**
     * 데이터소스 ID로 스키마 목록 조회
     */
    List<EngineDataSourceSchemaEntity> findByDataSourceId(String dataSourceId);
    
    /**
     * 데이터소스 ID와 필드명으로 스키마 조회
     */
    EngineDataSourceSchemaEntity findByDataSourceIdAndFieldName(String dataSourceId, String fieldName);
}
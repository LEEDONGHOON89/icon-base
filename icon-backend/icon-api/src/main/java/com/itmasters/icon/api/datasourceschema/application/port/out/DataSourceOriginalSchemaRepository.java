package com.itmasters.icon.api.datasourceschema.application.port.out;

import com.itmasters.icon.api.datasourceschema.adapter.out.persistence.entity.DataSourceOriginalSchemaEntity;

import java.util.List;
import java.util.Optional;

/**
 * DataSourceOriginalSchema Repository Port
 * 데이터소스 원본 스키마에 대한 저장소 인터페이스
 */
public interface DataSourceOriginalSchemaRepository {
    
    // 조회
    Optional<DataSourceOriginalSchemaEntity> findById(String schemaId);
    List<DataSourceOriginalSchemaEntity> findAllById(List<String> schemaIds);
    List<DataSourceOriginalSchemaEntity> findByDataSourceId(String dataSourceId);

    // 저장
    List<DataSourceOriginalSchemaEntity> saveAll(List<DataSourceOriginalSchemaEntity> schemas);
    
    // 존재 여부
    boolean existsByDataSourceIdAndFieldName(String dataSourceId, String fieldName);
}
package com.itmasters.icon.api.datasourceschema.adapter.out.persistence.repository;

import com.itmasters.icon.api.datasourceschema.adapter.out.persistence.entity.DataSourceOriginalSchemaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 데이터소스 원본 스키마 JPA Repository
 */
@Repository
public interface DataSourceOriginalSchemaJpaRepository extends JpaRepository<DataSourceOriginalSchemaEntity, String> {
    
    /**
     * 데이터소스 ID로 스키마 목록 조회
     */
    List<DataSourceOriginalSchemaEntity> findByDataSourceDataSourceId(String dataSourceId);
    
    /**
     * 데이터소스 ID로 스키마 목록 조회 (필드 순서로 정렬)
     */
    List<DataSourceOriginalSchemaEntity> findByDataSourceDataSourceIdOrderByFieldOrder(String dataSourceId);
    
    /**
     * 데이터소스 ID와 필드명으로 존재 여부 확인
     */
    boolean existsByDataSourceDataSourceIdAndFieldName(String dataSourceId, String fieldName);
}
package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDataSourceSchemaEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 테스트용 Mock EngineDataSourceSchemaRepository
 */
public class MockEngineDataSourceSchemaRepository implements EngineDataSourceSchemaRepository {
    
    private List<EngineDataSourceSchemaEntity> schemas = new ArrayList<>();
    
    public MockEngineDataSourceSchemaRepository() {
    }
    
    public MockEngineDataSourceSchemaRepository(List<EngineDataSourceSchemaEntity> schemas) {
        this.schemas = schemas;
    }
    
    @Override
    public List<EngineDataSourceSchemaEntity> findByDataSourceId(String dataSourceId) {
        return schemas.stream()
                .filter(s -> dataSourceId.equals(s.getDataSource() != null ? s.getDataSource().getDataSourceId() : null))
                .toList();
    }
    
    @Override
    public EngineDataSourceSchemaEntity findByDataSourceIdAndFieldName(String dataSourceId, String fieldName) {
        return schemas.stream()
                .filter(s -> dataSourceId.equals(s.getDataSource() != null ? s.getDataSource().getDataSourceId() : null))
                .filter(s -> fieldName.equals(s.getFieldName()))
                .findFirst()
                .orElse(null);
    }
    
    public void setSchemas(List<EngineDataSourceSchemaEntity> schemas) {
        this.schemas = schemas;
    }
}
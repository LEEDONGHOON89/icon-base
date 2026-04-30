package com.itmasters.icon.api.parser.adapter.out.persistence.repository;

import com.itmasters.icon.api.parser.adapter.out.persistence.entity.DataSourceParserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// [2026-04-20] 데이터소스-파서 연결 JPA 레포지토리
@Repository
public interface DataSourceParserJpaRepository extends JpaRepository<DataSourceParserEntity, String> {

    List<DataSourceParserEntity> findByDataSourceIdOrderByParserOrderAsc(String dataSourceId);

    List<DataSourceParserEntity> findByDataSourceIdAndIsActiveTrueOrderByParserOrderAsc(String dataSourceId);

    boolean existsByDataSourceIdAndParser_ParserId(String dataSourceId, String parserId);

    void deleteByDataSourceIdAndParser_ParserId(String dataSourceId, String parserId);
}

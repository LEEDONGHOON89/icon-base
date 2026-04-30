package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDataSourceParserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// [2026-04-20] 룰 엔진용 데이터소스-파서 연결 레포지토리
@Repository
public interface EngineDataSourceParserJpaRepository extends JpaRepository<EngineDataSourceParserEntity, String> {

    /** 데이터소스에 연결된 활성 파서 목록 (순서 오름차순) */
    List<EngineDataSourceParserEntity> findByDataSourceIdAndIsActiveTrueOrderByParserOrderAsc(String dataSourceId);
}

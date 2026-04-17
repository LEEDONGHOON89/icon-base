package com.itmasters.icon.api.datasource.adapter.out.persistence.repository;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DataSourceEntity;
import com.itmasters.icon.common.domain.type.DataSourceType;

import java.util.List;
import java.util.Optional;

/**
 * DataSource 커스텀 Repository 인터페이스
 */
public interface DataSourceJpaRepositoryCustom {
    
    /**
     * 데이터 소스 타입별 조회
     */
    List<DataSourceEntity> findBySourceType(DataSourceType sourceType);
    
    /**
     * 이름으로 데이터 소스 조회 (중복 체크용)
     */
    Optional<DataSourceEntity> findByName(String name);
    
    /**
     * 이름에 특정 키워드가 포함된 데이터 소스 검색
     */
    List<DataSourceEntity> searchByNameContaining(String keyword);
    
    /**
     * 활성화된 데이터 소스 조회
     */
    List<DataSourceEntity> findActiveDataSources();
}
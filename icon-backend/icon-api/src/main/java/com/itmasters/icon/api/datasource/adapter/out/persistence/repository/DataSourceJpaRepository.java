package com.itmasters.icon.api.datasource.adapter.out.persistence.repository;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DataSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * DataSource JPA Repository
 */
@Repository
public interface DataSourceJpaRepository extends JpaRepository<DataSourceEntity, String>, DataSourceJpaRepositoryCustom {
    // QueryDSL을 사용하므로 DataSourceJpaRepositoryCustom의 메서드들은 DataSourceJpaRepositoryImpl에서 구현됨
}
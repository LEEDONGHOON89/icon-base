package com.itmasters.icon.api.datasource.adapter.out.persistence.repository;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DataSourceEntity;
import com.itmasters.icon.common.domain.type.DataSourceType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.itmasters.icon.api.datasource.adapter.out.persistence.entity.QDataSourceEntity.dataSourceEntity;

/**
 * DataSource QueryDSL Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class DataSourceJpaRepositoryImpl implements DataSourceJpaRepositoryCustom {
    
    private final JPAQueryFactory queryFactory;
    
    @Override
    public List<DataSourceEntity> findBySourceType(DataSourceType sourceType) {
        return queryFactory
                .selectFrom(dataSourceEntity)
                .where(dataSourceEntity.sourceType.eq(sourceType))
                .orderBy(dataSourceEntity.name.asc())
                .fetch();
    }
    
    @Override
    public Optional<DataSourceEntity> findByName(String name) {
        DataSourceEntity result = queryFactory
                .selectFrom(dataSourceEntity)
                .where(dataSourceEntity.name.eq(name))
                .fetchOne();
        
        return Optional.ofNullable(result);
    }
    
    @Override
    public List<DataSourceEntity> searchByNameContaining(String keyword) {
        return queryFactory
                .selectFrom(dataSourceEntity)
                .where(dataSourceEntity.name.containsIgnoreCase(keyword))
                .orderBy(dataSourceEntity.name.asc())
                .fetch();
    }
    
    @Override
    public List<DataSourceEntity> findActiveDataSources() {
        // 활성화 상태를 관리하는 필드가 있다면 사용
        // 현재는 모든 데이터소스를 반환
        return queryFactory
                .selectFrom(dataSourceEntity)
                .orderBy(dataSourceEntity.createdAt.desc())
                .fetch();
    }
}
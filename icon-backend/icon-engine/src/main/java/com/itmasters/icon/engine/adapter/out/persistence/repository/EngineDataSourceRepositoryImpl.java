package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.common.domain.type.DataSourceType;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDataSourceEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QEngineDataSourceEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * QueryDSL을 사용하는 데이터소스 레파지토리 구현체
 */
@Repository
@RequiredArgsConstructor
public class EngineDataSourceRepositoryImpl implements EngineDataSourceRepository {
    
    private final JPAQueryFactory queryFactory;
    private final ModelMapper modelMapper;
    
    // Q클래스 (빌드 후 생성됨)
    private final QEngineDataSourceEntity dataSource = QEngineDataSourceEntity.engineDataSourceEntity;
    
    @Override
    public Optional<EngineDataSourceEntity> findById(String dataSourceId) {
        EngineDataSourceEntity result = queryFactory
                .selectFrom(dataSource)
                .where(dataSource.dataSourceId.eq(dataSourceId))
                .fetchOne();

        return Optional.ofNullable(result);
    }
    
    @Override
    public List<EngineDataSourceEntity> findActiveDataSources() {
        return queryFactory
                .selectFrom(dataSource)
                .where(dataSource.isActive.eq(true))
                .orderBy(dataSource.name.asc())
                .fetch();
    }
    
    @Override
    public List<EngineDataSourceEntity> findActiveDataSourcesByType(DataSourceType sourceType) {
        return queryFactory
                .selectFrom(dataSource)
                .where(
                    dataSource.isActive.eq(true),
                    dataSource.sourceType.eq(sourceType)
                )
                .orderBy(dataSource.name.asc())
                .fetch();
    }
}
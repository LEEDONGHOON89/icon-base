package com.itmasters.icon.api.datasourceschema.adapter.out.persistence;

import com.itmasters.icon.api.datasourceschema.adapter.out.persistence.entity.DataSourceOriginalSchemaEntity;
import com.itmasters.icon.api.datasourceschema.adapter.out.persistence.entity.QDataSourceOriginalSchemaEntity;
import com.itmasters.icon.api.datasourceschema.adapter.out.persistence.repository.DataSourceOriginalSchemaJpaRepository;
import com.itmasters.icon.api.datasourceschema.application.port.out.DataSourceOriginalSchemaRepository;
import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.QDataSourceEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 데이터소스 원본 스키마 리포지토리 구현체
 */
@Repository
@RequiredArgsConstructor
public class DataSourceOriginalSchemaRepositoryImpl implements DataSourceOriginalSchemaRepository {

    private final DataSourceOriginalSchemaJpaRepository jpaRepository;
    private final JPAQueryFactory queryFactory;
    
    private final QDataSourceOriginalSchemaEntity originalSchema = QDataSourceOriginalSchemaEntity.dataSourceOriginalSchemaEntity;
    private final QDataSourceEntity dataSource = QDataSourceEntity.dataSourceEntity;

    @Override
    public Optional<DataSourceOriginalSchemaEntity> findById(String schemaId) {
        return jpaRepository.findById(schemaId);
    }

    @Override
    public List<DataSourceOriginalSchemaEntity> findAllById(List<String> schemaIds) {
        return queryFactory
                .selectFrom(originalSchema)
                .leftJoin(originalSchema.dataSource, dataSource).fetchJoin()
                .where(originalSchema.dataSourceSchemaId.in(schemaIds))
                .fetch();
    }

    @Override
    public List<DataSourceOriginalSchemaEntity> findByDataSourceId(String dataSourceId) {
        return queryFactory
                .selectFrom(originalSchema)
                .leftJoin(originalSchema.dataSource, dataSource).fetchJoin()
                .where(originalSchema.dataSource.dataSourceId.eq(dataSourceId))
                .orderBy(originalSchema.fieldOrder.asc(), originalSchema.fieldName.asc())
                .fetch();
    }

    @Override
    public List<DataSourceOriginalSchemaEntity> saveAll(List<DataSourceOriginalSchemaEntity> schemas) {
        return jpaRepository.saveAll(schemas);
    }

    @Override
    public boolean existsByDataSourceIdAndFieldName(String dataSourceId, String fieldName) {
        Integer result = queryFactory
                .selectOne()
                .from(originalSchema)
                .where(
                    originalSchema.dataSource.dataSourceId.eq(dataSourceId),
                    originalSchema.fieldName.eq(fieldName)
                )
                .fetchFirst();
        
        return result != null;
    }

}
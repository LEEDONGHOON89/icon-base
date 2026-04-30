package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDataSourceSchemaEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QEngineDataSourceSchemaEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

import static com.itmasters.icon.engine.adapter.out.persistence.entity.QEngineDataSourceSchemaEntity.*;

/**
 * QueryDSL을 사용하는 데이터소스 스키마 레파지토리 구현체
 */
@Repository
@RequiredArgsConstructor
public class EngineDataSourceSchemaRepositoryImpl implements EngineDataSourceSchemaRepository {
    
    private final JPAQueryFactory queryFactory;
    private final ModelMapper modelMapper;
    
    // Q클래스 (빌드 후 생성됨)
    private final QEngineDataSourceSchemaEntity schema = engineDataSourceSchemaEntity;
    
    @Override
    public List<EngineDataSourceSchemaEntity> findByDataSourceId(String dataSourceId) {
        return queryFactory
                .selectFrom(schema)
                .where(schema.dataSource.dataSourceId.eq(dataSourceId))
                .orderBy(schema.fieldOrder.asc().nullsLast())
                .fetch();
    }
    
    @Override
    public EngineDataSourceSchemaEntity findByDataSourceIdAndFieldName(String dataSourceId, String fieldName) {
        return queryFactory
                .selectFrom(schema)
                .where(
                    schema.dataSource.dataSourceId.eq(dataSourceId),
                    schema.fieldName.eq(fieldName)
                )
                .fetchOne();
    }
}
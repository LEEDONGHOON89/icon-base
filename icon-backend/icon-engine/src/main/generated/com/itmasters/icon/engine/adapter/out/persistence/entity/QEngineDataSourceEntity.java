package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEngineDataSourceEntity is a Querydsl query type for EngineDataSourceEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEngineDataSourceEntity extends EntityPathBase<EngineDataSourceEntity> {

    private static final long serialVersionUID = 906634267L;

    public static final QEngineDataSourceEntity engineDataSourceEntity = new QEngineDataSourceEntity("engineDataSourceEntity");

    public final StringPath dataSourceId = createString("dataSourceId");

    public final BooleanPath isActive = createBoolean("isActive");

    public final StringPath name = createString("name");

    public final EnumPath<com.itmasters.icon.common.domain.type.DataSourceType> sourceType = createEnum("sourceType", com.itmasters.icon.common.domain.type.DataSourceType.class);

    public final StringPath transactionIdField = createString("transactionIdField");

    public QEngineDataSourceEntity(String variable) {
        super(EngineDataSourceEntity.class, forVariable(variable));
    }

    public QEngineDataSourceEntity(Path<? extends EngineDataSourceEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEngineDataSourceEntity(PathMetadata metadata) {
        super(EngineDataSourceEntity.class, metadata);
    }

}


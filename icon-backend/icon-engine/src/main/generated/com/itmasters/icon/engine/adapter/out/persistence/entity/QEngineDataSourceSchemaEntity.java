package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEngineDataSourceSchemaEntity is a Querydsl query type for EngineDataSourceSchemaEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEngineDataSourceSchemaEntity extends EntityPathBase<EngineDataSourceSchemaEntity> {

    private static final long serialVersionUID = -338903268L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEngineDataSourceSchemaEntity engineDataSourceSchemaEntity = new QEngineDataSourceSchemaEntity("engineDataSourceSchemaEntity");

    public final QEngineDataSourceEntity dataSource;

    public final EnumPath<com.itmasters.icon.common.domain.type.FieldDataType> dataType = createEnum("dataType", com.itmasters.icon.common.domain.type.FieldDataType.class);

    public final StringPath fieldName = createString("fieldName");

    public final NumberPath<Integer> fieldOrder = createNumber("fieldOrder", Integer.class);

    public final BooleanPath isActive = createBoolean("isActive");

    public final BooleanPath isRequired = createBoolean("isRequired");

    public final StringPath schemaId = createString("schemaId");

    public final QEngineStandardFieldEntity standardField;

    public final StringPath standardFieldId = createString("standardFieldId");

    public QEngineDataSourceSchemaEntity(String variable) {
        this(EngineDataSourceSchemaEntity.class, forVariable(variable), INITS);
    }

    public QEngineDataSourceSchemaEntity(Path<? extends EngineDataSourceSchemaEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEngineDataSourceSchemaEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEngineDataSourceSchemaEntity(PathMetadata metadata, PathInits inits) {
        this(EngineDataSourceSchemaEntity.class, metadata, inits);
    }

    public QEngineDataSourceSchemaEntity(Class<? extends EngineDataSourceSchemaEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.dataSource = inits.isInitialized("dataSource") ? new QEngineDataSourceEntity(forProperty("dataSource")) : null;
        this.standardField = inits.isInitialized("standardField") ? new QEngineStandardFieldEntity(forProperty("standardField")) : null;
    }

}


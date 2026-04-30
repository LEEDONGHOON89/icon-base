package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEngineStandardFieldEntity is a Querydsl query type for EngineStandardFieldEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEngineStandardFieldEntity extends EntityPathBase<EngineStandardFieldEntity> {

    private static final long serialVersionUID = 1204715053L;

    public static final QEngineStandardFieldEntity engineStandardFieldEntity = new QEngineStandardFieldEntity("engineStandardFieldEntity");

    public final StringPath category = createString("category");

    public final EnumPath<com.itmasters.icon.common.domain.type.FieldDataType> dataType = createEnum("dataType", com.itmasters.icon.common.domain.type.FieldDataType.class);

    public final StringPath description = createString("description");

    public final StringPath displayName = createString("displayName");

    public final BooleanPath isActive = createBoolean("isActive");

    public final StringPath standardFieldId = createString("standardFieldId");

    public QEngineStandardFieldEntity(String variable) {
        super(EngineStandardFieldEntity.class, forVariable(variable));
    }

    public QEngineStandardFieldEntity(Path<? extends EngineStandardFieldEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEngineStandardFieldEntity(PathMetadata metadata) {
        super(EngineStandardFieldEntity.class, metadata);
    }

}


package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEntityRelationFieldEntity is a Querydsl query type for EntityRelationFieldEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEntityRelationFieldEntity extends EntityPathBase<EntityRelationFieldEntity> {

    private static final long serialVersionUID = 1330101837L;

    public static final QEntityRelationFieldEntity entityRelationFieldEntity = new QEntityRelationFieldEntity("entityRelationFieldEntity");

    public final NumberPath<Long> configId = createNumber("configId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath dataSourceId = createString("dataSourceId");

    public final StringPath entityType = createString("entityType");

    public final StringPath fieldName = createString("fieldName");

    public final StringPath fieldRole = createString("fieldRole");

    public final BooleanPath isEnabled = createBoolean("isEnabled");

    public final NumberPath<Integer> priority = createNumber("priority", Integer.class);

    public final StringPath relationType = createString("relationType");

    public final StringPath targetEntityType = createString("targetEntityType");

    public final StringPath targetField = createString("targetField");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QEntityRelationFieldEntity(String variable) {
        super(EntityRelationFieldEntity.class, forVariable(variable));
    }

    public QEntityRelationFieldEntity(Path<? extends EntityRelationFieldEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEntityRelationFieldEntity(PathMetadata metadata) {
        super(EntityRelationFieldEntity.class, metadata);
    }

}


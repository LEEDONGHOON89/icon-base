package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEntityAttributeEntity is a Querydsl query type for EntityAttributeEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEntityAttributeEntity extends EntityPathBase<EntityAttributeEntity> {

    private static final long serialVersionUID = 1728955403L;

    public static final QEntityAttributeEntity entityAttributeEntity = new QEntityAttributeEntity("entityAttributeEntity");

    public final MapPath<String, Object, SimplePath<Object>> attributes = this.<String, Object, SimplePath<Object>>createMap("attributes", String.class, Object.class, SimplePath.class);

    public final DateTimePath<java.time.LocalDateTime> discoveredAt = createDateTime("discoveredAt", java.time.LocalDateTime.class);

    public final StringPath discoveredFrom = createString("discoveredFrom");

    public final NumberPath<Long> entityAttrId = createNumber("entityAttrId", Long.class);

    public final StringPath entityId = createString("entityId");

    public final StringPath entityType = createString("entityType");

    public final StringPath status = createString("status");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QEntityAttributeEntity(String variable) {
        super(EntityAttributeEntity.class, forVariable(variable));
    }

    public QEntityAttributeEntity(Path<? extends EntityAttributeEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEntityAttributeEntity(PathMetadata metadata) {
        super(EntityAttributeEntity.class, metadata);
    }

}


package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEntityRelationEntity is a Querydsl query type for EntityRelationEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEntityRelationEntity extends EntityPathBase<EntityRelationEntity> {

    private static final long serialVersionUID = 1385574675L;

    public static final QEntityRelationEntity entityRelationEntity = new QEntityRelationEntity("entityRelationEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath fromEntityId = createString("fromEntityId");

    public final StringPath fromEntityType = createString("fromEntityType");

    public final SimplePath<com.fasterxml.jackson.databind.JsonNode> properties = createSimple("properties", com.fasterxml.jackson.databind.JsonNode.class);

    public final NumberPath<Long> relationId = createNumber("relationId", Long.class);

    public final StringPath relationType = createString("relationType");

    public final StringPath toEntityId = createString("toEntityId");

    public final StringPath toEntityType = createString("toEntityType");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QEntityRelationEntity(String variable) {
        super(EntityRelationEntity.class, forVariable(variable));
    }

    public QEntityRelationEntity(Path<? extends EntityRelationEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEntityRelationEntity(PathMetadata metadata) {
        super(EntityRelationEntity.class, metadata);
    }

}


package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRelationRuleEntity is a Querydsl query type for RelationRuleEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRelationRuleEntity extends EntityPathBase<RelationRuleEntity> {

    private static final long serialVersionUID = 602084012L;

    public static final QRelationRuleEntity relationRuleEntity = new QRelationRuleEntity("relationRuleEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath dataSourceId = createString("dataSourceId");

    public final StringPath description = createString("description");

    public final StringPath fromEntityType = createString("fromEntityType");

    public final StringPath fromIdField = createString("fromIdField");

    public final BooleanPath isActive = createBoolean("isActive");

    public final SimplePath<com.fasterxml.jackson.databind.JsonNode> propertiesTemplate = createSimple("propertiesTemplate", com.fasterxml.jackson.databind.JsonNode.class);

    public final StringPath relationType = createString("relationType");

    public final NumberPath<Long> ruleId = createNumber("ruleId", Long.class);

    public final SimplePath<com.fasterxml.jackson.databind.JsonNode> toEntityAttributesTemplate = createSimple("toEntityAttributesTemplate", com.fasterxml.jackson.databind.JsonNode.class);

    public final StringPath toEntityType = createString("toEntityType");

    public final StringPath toIdField = createString("toIdField");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QRelationRuleEntity(String variable) {
        super(RelationRuleEntity.class, forVariable(variable));
    }

    public QRelationRuleEntity(Path<? extends RelationRuleEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRelationRuleEntity(PathMetadata metadata) {
        super(RelationRuleEntity.class, metadata);
    }

}


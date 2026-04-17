package com.itmasters.icon.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDerivedRuleEntity is a Querydsl query type for DerivedRuleEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDerivedRuleEntity extends EntityPathBase<DerivedRuleEntity> {

    private static final long serialVersionUID = -655675951L;

    public static final QDerivedRuleEntity derivedRuleEntity = new QDerivedRuleEntity("derivedRuleEntity");

    public final StringPath computationConfig = createString("computationConfig");

    public final StringPath computationType = createString("computationType");

    public final StringPath dataSourceId = createString("dataSourceId");

    public final StringPath description = createString("description");

    public final StringPath fieldType = createString("fieldType");

    public final BooleanPath isActive = createBoolean("isActive");

    public final NumberPath<Integer> priority = createNumber("priority", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> regDt = createDateTime("regDt", java.time.LocalDateTime.class);

    public final StringPath regUserId = createString("regUserId");

    public final NumberPath<Long> ruleId = createNumber("ruleId", Long.class);

    public final StringPath targetField = createString("targetField");

    public final DateTimePath<java.time.LocalDateTime> updDt = createDateTime("updDt", java.time.LocalDateTime.class);

    public final StringPath updUserId = createString("updUserId");

    public QDerivedRuleEntity(String variable) {
        super(DerivedRuleEntity.class, forVariable(variable));
    }

    public QDerivedRuleEntity(Path<? extends DerivedRuleEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDerivedRuleEntity(PathMetadata metadata) {
        super(DerivedRuleEntity.class, metadata);
    }

}


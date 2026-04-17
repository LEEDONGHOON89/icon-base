package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QContextRuleMappingEntity is a Querydsl query type for ContextRuleMappingEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QContextRuleMappingEntity extends EntityPathBase<ContextRuleMappingEntity> {

    private static final long serialVersionUID = 1862255511L;

    public static final QContextRuleMappingEntity contextRuleMappingEntity = new QContextRuleMappingEntity("contextRuleMappingEntity");

    public final BooleanPath alertSent = createBoolean("alertSent");

    public final NumberPath<Long> contextRuleMappingId = createNumber("contextRuleMappingId", Long.class);

    public final NumberPath<Long> detectionContextId = createNumber("detectionContextId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> detectionTimestamp = createDateTime("detectionTimestamp", java.time.LocalDateTime.class);

    public final BooleanPath isTrigger = createBoolean("isTrigger");

    public final StringPath matchedConditions = createString("matchedConditions");

    public final DateTimePath<java.time.LocalDateTime> regDt = createDateTime("regDt", java.time.LocalDateTime.class);

    public final StringPath ruleCategory = createString("ruleCategory");

    public final StringPath ruleId = createString("ruleId");

    public final StringPath ruleMetadata = createString("ruleMetadata");

    public final StringPath ruleName = createString("ruleName");

    public final NumberPath<Double> ruleScore = createNumber("ruleScore", Double.class);

    public final NumberPath<Integer> sequenceNumber = createNumber("sequenceNumber", Integer.class);

    public final StringPath severity = createString("severity");

    public final NumberPath<Integer> timeSinceLastRuleSeconds = createNumber("timeSinceLastRuleSeconds", Integer.class);

    public QContextRuleMappingEntity(String variable) {
        super(ContextRuleMappingEntity.class, forVariable(variable));
    }

    public QContextRuleMappingEntity(Path<? extends ContextRuleMappingEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QContextRuleMappingEntity(PathMetadata metadata) {
        super(ContextRuleMappingEntity.class, metadata);
    }

}


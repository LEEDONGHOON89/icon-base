package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPatternRelationEntity is a Querydsl query type for PatternRelationEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPatternRelationEntity extends EntityPathBase<PatternRelationEntity> {

    private static final long serialVersionUID = -947710594L;

    public static final QPatternRelationEntity patternRelationEntity = new QPatternRelationEntity("patternRelationEntity");

    public final StringPath approvalStatus = createString("approvalStatus");

    public final NumberPath<java.math.BigDecimal> confidenceScore = createNumber("confidenceScore", java.math.BigDecimal.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath dataSourceId = createString("dataSourceId");

    public final StringPath detectionMethod = createString("detectionMethod");

    public final DateTimePath<java.time.LocalDateTime> firstSeenAt = createDateTime("firstSeenAt", java.time.LocalDateTime.class);

    public final StringPath fromEntityType = createString("fromEntityType");

    public final StringPath fromIdField = createString("fromIdField");

    public final DateTimePath<java.time.LocalDateTime> lastSeenAt = createDateTime("lastSeenAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> occurrenceCount = createNumber("occurrenceCount", Integer.class);

    public final SimplePath<com.fasterxml.jackson.databind.JsonNode> patternEvidence = createSimple("patternEvidence", com.fasterxml.jackson.databind.JsonNode.class);

    public final NumberPath<Long> patternId = createNumber("patternId", Long.class);

    public final StringPath rejectReason = createString("rejectReason");

    public final StringPath relationType = createString("relationType");

    public final DateTimePath<java.time.LocalDateTime> reviewedAt = createDateTime("reviewedAt", java.time.LocalDateTime.class);

    public final StringPath reviewedBy = createString("reviewedBy");

    public final StringPath toEntityType = createString("toEntityType");

    public final StringPath toIdField = createString("toIdField");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QPatternRelationEntity(String variable) {
        super(PatternRelationEntity.class, forVariable(variable));
    }

    public QPatternRelationEntity(Path<? extends PatternRelationEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPatternRelationEntity(PathMetadata metadata) {
        super(PatternRelationEntity.class, metadata);
    }

}


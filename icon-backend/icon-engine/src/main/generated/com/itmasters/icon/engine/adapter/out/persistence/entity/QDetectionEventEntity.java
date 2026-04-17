package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDetectionEventEntity is a Querydsl query type for DetectionEventEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDetectionEventEntity extends EntityPathBase<DetectionEventEntity> {

    private static final long serialVersionUID = 13049001L;

    public static final QDetectionEventEntity detectionEventEntity = new QDetectionEventEntity("detectionEventEntity");

    public final NumberPath<Double> anomalyScore = createNumber("anomalyScore", Double.class);

    public final StringPath category = createString("category");

    public final NumberPath<Long> contextId = createNumber("contextId", Long.class);

    public final StringPath description = createString("description");

    public final StringPath eventData = createString("eventData");

    public final NumberPath<Long> eventId = createNumber("eventId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> eventTimestamp = createDateTime("eventTimestamp", java.time.LocalDateTime.class);

    public final StringPath eventType = createString("eventType");

    public final NumberPath<Long> execDsMpId = createNumber("execDsMpId", Long.class);

    public final StringPath matchedConditions = createString("matchedConditions");

    public final DateTimePath<java.time.LocalDateTime> regDt = createDateTime("regDt", java.time.LocalDateTime.class);

    public final StringPath ruleId = createString("ruleId");

    public final StringPath ruleName = createString("ruleName");

    public final StringPath severity = createString("severity");

    public QDetectionEventEntity(String variable) {
        super(DetectionEventEntity.class, forVariable(variable));
    }

    public QDetectionEventEntity(Path<? extends DetectionEventEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDetectionEventEntity(PathMetadata metadata) {
        super(DetectionEventEntity.class, metadata);
    }

}


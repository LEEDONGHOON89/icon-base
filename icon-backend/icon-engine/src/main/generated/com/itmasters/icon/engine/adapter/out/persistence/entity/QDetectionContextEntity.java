package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDetectionContextEntity is a Querydsl query type for DetectionContextEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDetectionContextEntity extends EntityPathBase<DetectionContextEntity> {

    private static final long serialVersionUID = -1113241346L;

    public static final QDetectionContextEntity detectionContextEntity = new QDetectionContextEntity("detectionContextEntity");

    public final NumberPath<Double> anomalyScore = createNumber("anomalyScore", Double.class);

    public final MapPath<String, Object, SimplePath<Object>> contextData = this.<String, Object, SimplePath<Object>>createMap("contextData", String.class, Object.class, SimplePath.class);

    public final StringPath correlationKey = createString("correlationKey");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> detectionContextId = createNumber("detectionContextId", Long.class);

    public final NumberPath<Integer> eventCount = createNumber("eventCount", Integer.class);

    public final StringPath keyType = createString("keyType");

    public final StringPath profileId = createString("profileId");

    public final StringPath riskLevel = createString("riskLevel");

    public final NumberPath<Integer> ruleMatchCount = createNumber("ruleMatchCount", Integer.class);

    public final StringPath scenarioStage = createString("scenarioStage");

    public final StringPath scenarioType = createString("scenarioType");

    public final StringPath status = createString("status");

    public final DateTimePath<java.time.LocalDateTime> triggeredAt = createDateTime("triggeredAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> windowDurationMinutes = createNumber("windowDurationMinutes", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> windowEnd = createDateTime("windowEnd", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> windowStart = createDateTime("windowStart", java.time.LocalDateTime.class);

    public QDetectionContextEntity(String variable) {
        super(DetectionContextEntity.class, forVariable(variable));
    }

    public QDetectionContextEntity(Path<? extends DetectionContextEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDetectionContextEntity(PathMetadata metadata) {
        super(DetectionContextEntity.class, metadata);
    }

}


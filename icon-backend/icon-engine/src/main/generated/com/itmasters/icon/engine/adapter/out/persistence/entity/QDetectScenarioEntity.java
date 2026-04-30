package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDetectScenarioEntity is a Querydsl query type for DetectScenarioEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDetectScenarioEntity extends EntityPathBase<DetectScenarioEntity> {

    private static final long serialVersionUID = 1787010663L;

    public static final QDetectScenarioEntity detectScenarioEntity = new QDetectScenarioEntity("detectScenarioEntity");

    public final DateTimePath<java.time.LocalDateTime> detectedDt = createDateTime("detectedDt", java.time.LocalDateTime.class);

    public final NumberPath<Long> detectScenarioId = createNumber("detectScenarioId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> eventDt = createDateTime("eventDt", java.time.LocalDateTime.class);

    public final StringPath groupKey = createString("groupKey");

    public final NumberPath<Long> mappedStorageId = createNumber("mappedStorageId", Long.class);

    public final StringPath riskLevel = createString("riskLevel");

    public final StringPath scenarioId = createString("scenarioId");

    public final StringPath transactionId = createString("transactionId");

    public final DateTimePath<java.time.LocalDateTime> windowEnd = createDateTime("windowEnd", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> windowStart = createDateTime("windowStart", java.time.LocalDateTime.class);

    public QDetectScenarioEntity(String variable) {
        super(DetectScenarioEntity.class, forVariable(variable));
    }

    public QDetectScenarioEntity(Path<? extends DetectScenarioEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDetectScenarioEntity(PathMetadata metadata) {
        super(DetectScenarioEntity.class, metadata);
    }

}


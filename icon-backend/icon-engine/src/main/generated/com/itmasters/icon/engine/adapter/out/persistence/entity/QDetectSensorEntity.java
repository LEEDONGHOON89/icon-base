package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDetectSensorEntity is a Querydsl query type for DetectSensorEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDetectSensorEntity extends EntityPathBase<DetectSensorEntity> {

    private static final long serialVersionUID = 2066209809L;

    public static final QDetectSensorEntity detectSensorEntity = new QDetectSensorEntity("detectSensorEntity");

    public final DateTimePath<java.time.LocalDateTime> detectedDt = createDateTime("detectedDt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> detectSensorId = createNumber("detectSensorId", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> eventDt = createDateTime("eventDt", java.time.LocalDateTime.class);

    public final StringPath groupKey = createString("groupKey");

    public final NumberPath<Long> mappedStorageId = createNumber("mappedStorageId", Long.class);

    public final StringPath matchedFields = createString("matchedFields");

    public final NumberPath<Integer> rowNumber = createNumber("rowNumber", Integer.class);

    public final StringPath sensorId = createString("sensorId");

    public final StringPath transactionId = createString("transactionId");

    public QDetectSensorEntity(String variable) {
        super(DetectSensorEntity.class, forVariable(variable));
    }

    public QDetectSensorEntity(Path<? extends DetectSensorEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDetectSensorEntity(PathMetadata metadata) {
        super(DetectSensorEntity.class, metadata);
    }

}


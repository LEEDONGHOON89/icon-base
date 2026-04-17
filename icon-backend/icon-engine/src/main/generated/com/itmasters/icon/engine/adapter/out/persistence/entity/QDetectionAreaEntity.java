package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDetectionAreaEntity is a Querydsl query type for DetectionAreaEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDetectionAreaEntity extends EntityPathBase<DetectionAreaEntity> {

    private static final long serialVersionUID = -118182972L;

    public static final QDetectionAreaEntity detectionAreaEntity = new QDetectionAreaEntity("detectionAreaEntity");

    public final StringPath areaName = createString("areaName");

    public final StringPath color = createString("color");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath description = createString("description");

    public final StringPath detectionAreaId = createString("detectionAreaId");

    public final NumberPath<Integer> displayOrder = createNumber("displayOrder", Integer.class);

    public final StringPath icon = createString("icon");

    public final BooleanPath isActive = createBoolean("isActive");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QDetectionAreaEntity(String variable) {
        super(DetectionAreaEntity.class, forVariable(variable));
    }

    public QDetectionAreaEntity(Path<? extends DetectionAreaEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDetectionAreaEntity(PathMetadata metadata) {
        super(DetectionAreaEntity.class, metadata);
    }

}


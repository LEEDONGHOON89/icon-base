package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSensorEntity is a Querydsl query type for SensorEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSensorEntity extends EntityPathBase<SensorEntity> {

    private static final long serialVersionUID = -2055733618L;

    public static final QSensorEntity sensorEntity = new QSensorEntity("sensorEntity");

    public final StringPath description = createString("description");

    public final BooleanPath isActive = createBoolean("isActive");

    public final StringPath sensorId = createString("sensorId");

    public final StringPath sensorName = createString("sensorName");

    public final NumberPath<Integer> version = createNumber("version", Integer.class);

    public final StringPath whereJson = createString("whereJson");

    public QSensorEntity(String variable) {
        super(SensorEntity.class, forVariable(variable));
    }

    public QSensorEntity(Path<? extends SensorEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSensorEntity(PathMetadata metadata) {
        super(SensorEntity.class, metadata);
    }

}


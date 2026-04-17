package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEngineEventStreamEntity is a Querydsl query type for EngineEventStreamEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEngineEventStreamEntity extends EntityPathBase<EngineEventStreamEntity> {

    private static final long serialVersionUID = 1787528746L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEngineEventStreamEntity engineEventStreamEntity = new QEngineEventStreamEntity("engineEventStreamEntity");

    public final MapPath<String, Object, SimplePath<Object>> eventData = this.<String, Object, SimplePath<Object>>createMap("eventData", String.class, Object.class, SimplePath.class);

    public final DateTimePath<java.time.LocalDateTime> eventDt = createDateTime("eventDt", java.time.LocalDateTime.class);

    public final NumberPath<Long> eventStreamId = createNumber("eventStreamId", Long.class);

    public final QMappedDataStorageEntity mappedDataStorage;

    public final NumberPath<Long> mappedDataStorageId = createNumber("mappedDataStorageId", Long.class);

    public final StringPath transactionId = createString("transactionId");

    public QEngineEventStreamEntity(String variable) {
        this(EngineEventStreamEntity.class, forVariable(variable), INITS);
    }

    public QEngineEventStreamEntity(Path<? extends EngineEventStreamEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEngineEventStreamEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEngineEventStreamEntity(PathMetadata metadata, PathInits inits) {
        this(EngineEventStreamEntity.class, metadata, inits);
    }

    public QEngineEventStreamEntity(Class<? extends EngineEventStreamEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.mappedDataStorage = inits.isInitialized("mappedDataStorage") ? new QMappedDataStorageEntity(forProperty("mappedDataStorage")) : null;
    }

}


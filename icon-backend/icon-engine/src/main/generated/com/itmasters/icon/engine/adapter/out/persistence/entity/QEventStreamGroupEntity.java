package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEventStreamGroupEntity is a Querydsl query type for EventStreamGroupEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEventStreamGroupEntity extends EntityPathBase<EventStreamGroupEntity> {

    private static final long serialVersionUID = 1151829305L;

    public static final QEventStreamGroupEntity eventStreamGroupEntity = new QEventStreamGroupEntity("eventStreamGroupEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> eventStreamId = createNumber("eventStreamId", Long.class);

    public final StringPath groupKey = createString("groupKey");

    public final NumberPath<Long> mappedStorageId = createNumber("mappedStorageId", Long.class);

    public final StringPath ruleId = createString("ruleId");

    public QEventStreamGroupEntity(String variable) {
        super(EventStreamGroupEntity.class, forVariable(variable));
    }

    public QEventStreamGroupEntity(Path<? extends EventStreamGroupEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEventStreamGroupEntity(PathMetadata metadata) {
        super(EventStreamGroupEntity.class, metadata);
    }

}


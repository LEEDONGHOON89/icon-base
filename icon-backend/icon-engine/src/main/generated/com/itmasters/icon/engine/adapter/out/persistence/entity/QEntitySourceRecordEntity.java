package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEntitySourceRecordEntity is a Querydsl query type for EntitySourceRecordEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEntitySourceRecordEntity extends EntityPathBase<EntitySourceRecordEntity> {

    private static final long serialVersionUID = -753004189L;

    public static final QEntitySourceRecordEntity entitySourceRecordEntity = new QEntitySourceRecordEntity("entitySourceRecordEntity");

    public final StringPath dataSourceId = createString("dataSourceId");

    public final StringPath entityId = createString("entityId");

    public final StringPath entityType = createString("entityType");

    public final NumberPath<Long> mappedStorageId = createNumber("mappedStorageId", Long.class);

    public final StringPath processingBatchId = createString("processingBatchId");

    public final DateTimePath<java.time.LocalDateTime> receivedAt = createDateTime("receivedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> recordId = createNumber("recordId", Long.class);

    public final SimplePath<com.fasterxml.jackson.databind.JsonNode> sourceData = createSimple("sourceData", com.fasterxml.jackson.databind.JsonNode.class);

    public final StringPath sourceTxId = createString("sourceTxId");

    public QEntitySourceRecordEntity(String variable) {
        super(EntitySourceRecordEntity.class, forVariable(variable));
    }

    public QEntitySourceRecordEntity(Path<? extends EntitySourceRecordEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEntitySourceRecordEntity(PathMetadata metadata) {
        super(EntitySourceRecordEntity.class, metadata);
    }

}


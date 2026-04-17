package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMappedDataStorageEntity is a Querydsl query type for MappedDataStorageEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMappedDataStorageEntity extends EntityPathBase<MappedDataStorageEntity> {

    private static final long serialVersionUID = 1980850640L;

    public static final QMappedDataStorageEntity mappedDataStorageEntity = new QMappedDataStorageEntity("mappedDataStorageEntity");

    public final StringPath errorMessage = createString("errorMessage");

    public final NumberPath<Long> execDsMpId = createNumber("execDsMpId", Long.class);

    public final NumberPath<Long> landingRecordId = createNumber("landingRecordId", Long.class);

    public final NumberPath<Long> mappedDataStorageId = createNumber("mappedDataStorageId", Long.class);

    public final EnumPath<MappedDataStorageEntity.ProcessingStatus> processingStatus = createEnum("processingStatus", MappedDataStorageEntity.ProcessingStatus.class);

    public final DateTimePath<java.time.LocalDateTime> regDt = createDateTime("regDt", java.time.LocalDateTime.class);

    public final MapPath<String, Object, SimplePath<Object>> rowData = this.<String, Object, SimplePath<Object>>createMap("rowData", String.class, Object.class, SimplePath.class);

    public final NumberPath<Integer> rowIndex = createNumber("rowIndex", Integer.class);

    public final StringPath transactionId = createString("transactionId");

    public QMappedDataStorageEntity(String variable) {
        super(MappedDataStorageEntity.class, forVariable(variable));
    }

    public QMappedDataStorageEntity(Path<? extends MappedDataStorageEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMappedDataStorageEntity(PathMetadata metadata) {
        super(MappedDataStorageEntity.class, metadata);
    }

}


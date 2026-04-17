package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEngineDsFileSystemLogEntity is a Querydsl query type for EngineDsFileSystemLogEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEngineDsFileSystemLogEntity extends EntityPathBase<EngineDsFileSystemLogEntity> {

    private static final long serialVersionUID = -1422569190L;

    public static final QEngineDsFileSystemLogEntity engineDsFileSystemLogEntity = new QEngineDsFileSystemLogEntity("engineDsFileSystemLogEntity");

    public final StringPath configId = createString("configId");

    public final StringPath errorMessage = createString("errorMessage");

    public final StringPath fileName = createString("fileName");

    public final StringPath filePath = createString("filePath");

    public final NumberPath<Long> fileSize = createNumber("fileSize", Long.class);

    public final StringPath id = createString("id");

    public final DateTimePath<java.time.LocalDateTime> processedAt = createDateTime("processedAt", java.time.LocalDateTime.class);

    public final EnumPath<EngineDsFileSystemLogEntity.ProcessingStatus> processingStatus = createEnum("processingStatus", EngineDsFileSystemLogEntity.ProcessingStatus.class);

    public final NumberPath<Integer> recordsProcessed = createNumber("recordsProcessed", Integer.class);

    public QEngineDsFileSystemLogEntity(String variable) {
        super(EngineDsFileSystemLogEntity.class, forVariable(variable));
    }

    public QEngineDsFileSystemLogEntity(Path<? extends EngineDsFileSystemLogEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEngineDsFileSystemLogEntity(PathMetadata metadata) {
        super(EngineDsFileSystemLogEntity.class, metadata);
    }

}


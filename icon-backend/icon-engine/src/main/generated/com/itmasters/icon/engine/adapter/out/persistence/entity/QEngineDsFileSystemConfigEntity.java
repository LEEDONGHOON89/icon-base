package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEngineDsFileSystemConfigEntity is a Querydsl query type for EngineDsFileSystemConfigEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEngineDsFileSystemConfigEntity extends EntityPathBase<EngineDsFileSystemConfigEntity> {

    private static final long serialVersionUID = 1111745554L;

    public static final QEngineDsFileSystemConfigEntity engineDsFileSystemConfigEntity = new QEngineDsFileSystemConfigEntity("engineDsFileSystemConfigEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath dataSourceId = createString("dataSourceId");

    public final StringPath delimiter = createString("delimiter");

    public final StringPath fileEncoding = createString("fileEncoding");

    public final StringPath filePattern = createString("filePattern");

    public final BooleanPath hasHeader = createBoolean("hasHeader");

    public final StringPath id = createString("id");

    public final BooleanPath isActive = createBoolean("isActive");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final StringPath watchDirectory = createString("watchDirectory");

    public QEngineDsFileSystemConfigEntity(String variable) {
        super(EngineDsFileSystemConfigEntity.class, forVariable(variable));
    }

    public QEngineDsFileSystemConfigEntity(Path<? extends EngineDsFileSystemConfigEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEngineDsFileSystemConfigEntity(PathMetadata metadata) {
        super(EngineDsFileSystemConfigEntity.class, metadata);
    }

}


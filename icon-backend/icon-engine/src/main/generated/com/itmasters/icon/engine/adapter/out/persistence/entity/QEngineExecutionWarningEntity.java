package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEngineExecutionWarningEntity is a Querydsl query type for EngineExecutionWarningEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEngineExecutionWarningEntity extends EntityPathBase<EngineExecutionWarningEntity> {

    private static final long serialVersionUID = 700332954L;

    public static final QEngineExecutionWarningEntity engineExecutionWarningEntity = new QEngineExecutionWarningEntity("engineExecutionWarningEntity");

    public final StringPath code = createString("code");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> execDsMpId = createNumber("execDsMpId", Long.class);

    public final StringPath message = createString("message");

    public final StringPath step = createString("step");

    public final NumberPath<Long> warningId = createNumber("warningId", Long.class);

    public QEngineExecutionWarningEntity(String variable) {
        super(EngineExecutionWarningEntity.class, forVariable(variable));
    }

    public QEngineExecutionWarningEntity(Path<? extends EngineExecutionWarningEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEngineExecutionWarningEntity(PathMetadata metadata) {
        super(EngineExecutionWarningEntity.class, metadata);
    }

}


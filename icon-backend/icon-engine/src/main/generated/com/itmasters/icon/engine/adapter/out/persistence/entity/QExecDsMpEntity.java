package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QExecDsMpEntity is a Querydsl query type for ExecDsMpEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QExecDsMpEntity extends EntityPathBase<ExecDsMpEntity> {

    private static final long serialVersionUID = -1302865257L;

    public static final QExecDsMpEntity execDsMpEntity = new QExecDsMpEntity("execDsMpEntity");

    public final DateTimePath<java.time.LocalDateTime> completeAt = createDateTime("completeAt", java.time.LocalDateTime.class);

    public final StringPath dataSourceId = createString("dataSourceId");

    public final StringPath errorMessage = createString("errorMessage");

    public final NumberPath<Long> execDsMpId = createNumber("execDsMpId", Long.class);

    public final StringPath executedBy = createString("executedBy");

    public final MapPath<String, Object, SimplePath<Object>> executionContext = this.<String, Object, SimplePath<Object>>createMap("executionContext", String.class, Object.class, SimplePath.class);

    public final EnumPath<com.itmasters.icon.common.domain.type.ExecutionMode> executionMode = createEnum("executionMode", com.itmasters.icon.common.domain.type.ExecutionMode.class);

    public final DateTimePath<java.time.LocalDateTime> startDt = createDateTime("startDt", java.time.LocalDateTime.class);

    public final EnumPath<com.itmasters.icon.common.domain.type.ExecutionStatus> status = createEnum("status", com.itmasters.icon.common.domain.type.ExecutionStatus.class);

    public final NumberPath<Integer> totalRows = createNumber("totalRows", Integer.class);

    public QExecDsMpEntity(String variable) {
        super(ExecDsMpEntity.class, forVariable(variable));
    }

    public QExecDsMpEntity(Path<? extends ExecDsMpEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QExecDsMpEntity(PathMetadata metadata) {
        super(ExecDsMpEntity.class, metadata);
    }

}


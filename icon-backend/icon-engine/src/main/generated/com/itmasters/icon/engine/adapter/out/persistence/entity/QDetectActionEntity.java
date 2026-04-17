package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDetectActionEntity is a Querydsl query type for DetectActionEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDetectActionEntity extends EntityPathBase<DetectActionEntity> {

    private static final long serialVersionUID = -497796723L;

    public static final QDetectActionEntity detectActionEntity = new QDetectActionEntity("detectActionEntity");

    public final StringPath actionMemo = createString("actionMemo");

    public final StringPath actionReason = createString("actionReason");

    public final EnumPath<DetectActionEntity.ActionStatus> actionStatus = createEnum("actionStatus", DetectActionEntity.ActionStatus.class);

    public final EnumPath<DetectActionEntity.ActionType> actionType = createEnum("actionType", DetectActionEntity.ActionType.class);

    public final DateTimePath<java.time.LocalDateTime> approvedAt = createDateTime("approvedAt", java.time.LocalDateTime.class);

    public final StringPath approvedBy = createString("approvedBy");

    public final DateTimePath<java.time.LocalDateTime> completedAt = createDateTime("completedAt", java.time.LocalDateTime.class);

    public final StringPath completedBy = createString("completedBy");

    public final NumberPath<Long> detectActionId = createNumber("detectActionId", Long.class);

    public final NumberPath<Long> detectScenarioId = createNumber("detectScenarioId", Long.class);

    public final StringPath groupKey = createString("groupKey");

    public final MapPath<String, Object, SimplePath<Object>> metadata = this.<String, Object, SimplePath<Object>>createMap("metadata", String.class, Object.class, SimplePath.class);

    public final DateTimePath<java.time.LocalDateTime> regDt = createDateTime("regDt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> requestedAt = createDateTime("requestedAt", java.time.LocalDateTime.class);

    public final StringPath requestedBy = createString("requestedBy");

    public final StringPath riskLevel = createString("riskLevel");

    public final StringPath scenarioId = createString("scenarioId");

    public QDetectActionEntity(String variable) {
        super(DetectActionEntity.class, forVariable(variable));
    }

    public QDetectActionEntity(Path<? extends DetectActionEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDetectActionEntity(PathMetadata metadata) {
        super(DetectActionEntity.class, metadata);
    }

}


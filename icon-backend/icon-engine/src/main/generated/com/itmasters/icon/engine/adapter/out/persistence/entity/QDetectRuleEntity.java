package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDetectRuleEntity is a Querydsl query type for DetectRuleEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDetectRuleEntity extends EntityPathBase<DetectRuleEntity> {

    private static final long serialVersionUID = -580187437L;

    public static final QDetectRuleEntity detectRuleEntity = new QDetectRuleEntity("detectRuleEntity");

    public final NumberPath<Integer> dedupMinutes = createNumber("dedupMinutes", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> detectedDt = createDateTime("detectedDt", java.time.LocalDateTime.class);

    public final NumberPath<Long> detectRuleId = createNumber("detectRuleId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> endDt = createDateTime("endDt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> eventDt = createDateTime("eventDt", java.time.LocalDateTime.class);

    public final StringPath groupKey = createString("groupKey");

    public final NumberPath<Long> mappedStorageId = createNumber("mappedStorageId", Long.class);

    public final NumberPath<java.math.BigDecimal> matchedCount = createNumber("matchedCount", java.math.BigDecimal.class);

    public final StringPath operator = createString("operator");

    public final StringPath originalGroupKey = createString("originalGroupKey");

    public final BooleanPath pass = createBoolean("pass");

    public final StringPath ruleId = createString("ruleId");

    public final DateTimePath<java.time.LocalDateTime> startDt = createDateTime("startDt", java.time.LocalDateTime.class);

    public final NumberPath<java.math.BigDecimal> thresholdCount = createNumber("thresholdCount", java.math.BigDecimal.class);

    public final StringPath transactionId = createString("transactionId");

    public final NumberPath<Integer> windowMinutes = createNumber("windowMinutes", Integer.class);

    public QDetectRuleEntity(String variable) {
        super(DetectRuleEntity.class, forVariable(variable));
    }

    public QDetectRuleEntity(Path<? extends DetectRuleEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDetectRuleEntity(PathMetadata metadata) {
        super(DetectRuleEntity.class, metadata);
    }

}


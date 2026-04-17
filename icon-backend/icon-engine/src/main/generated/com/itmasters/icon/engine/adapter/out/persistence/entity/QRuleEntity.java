package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRuleEntity is a Querydsl query type for RuleEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRuleEntity extends EntityPathBase<RuleEntity> {

    private static final long serialVersionUID = -445929328L;

    public static final QRuleEntity ruleEntity = new QRuleEntity("ruleEntity");

    public final StringPath aggregationField = createString("aggregationField");

    public final StringPath anchorSensorId = createString("anchorSensorId");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> dedupMinutes = createNumber("dedupMinutes", Integer.class);

    public final StringPath description = createString("description");

    public final StringPath evaluationMode = createString("evaluationMode");

    public final ArrayPath<String[], String> groupByFields = createArray("groupByFields", String[].class);

    public final BooleanPath isActive = createBoolean("isActive");

    public final StringPath name = createString("name");

    public final StringPath nextSensorId = createString("nextSensorId");

    public final EnumPath<com.itmasters.icon.common.domain.aggregate.AggregateOperator> operator = createEnum("operator", com.itmasters.icon.common.domain.aggregate.AggregateOperator.class);

    public final StringPath predicateSensorId = createString("predicateSensorId");

    public final StringPath prevSensorId = createString("prevSensorId");

    public final StringPath ruleId = createString("ruleId");

    public final NumberPath<java.math.BigDecimal> thresholdAmount = createNumber("thresholdAmount", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> thresholdCount = createNumber("thresholdCount", java.math.BigDecimal.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final StringPath whereJson = createString("whereJson");

    public final NumberPath<Integer> windowMinutes = createNumber("windowMinutes", Integer.class);

    public QRuleEntity(String variable) {
        super(RuleEntity.class, forVariable(variable));
    }

    public QRuleEntity(Path<? extends RuleEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRuleEntity(PathMetadata metadata) {
        super(RuleEntity.class, metadata);
    }

}


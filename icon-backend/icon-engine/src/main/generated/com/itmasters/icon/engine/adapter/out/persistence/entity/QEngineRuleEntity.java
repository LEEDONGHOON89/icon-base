package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEngineRuleEntity is a Querydsl query type for EngineRuleEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEngineRuleEntity extends EntityPathBase<EngineRuleEntity> {

    private static final long serialVersionUID = -994565198L;

    public static final QEngineRuleEntity engineRuleEntity = new QEngineRuleEntity("engineRuleEntity");

    public final StringPath anchorSensorId = createString("anchorSensorId");

    public final BooleanPath isActive = createBoolean("isActive");

    public final StringPath nextSensorId = createString("nextSensorId");

    public final StringPath operatorName = createString("operatorName");

    public final StringPath predicateSensorId = createString("predicateSensorId");

    public final StringPath prevSensorId = createString("prevSensorId");

    public final StringPath ruleId = createString("ruleId");

    public final StringPath ruleName = createString("ruleName");

    public final StringPath whereJson = createString("whereJson");

    public QEngineRuleEntity(String variable) {
        super(EngineRuleEntity.class, forVariable(variable));
    }

    public QEngineRuleEntity(Path<? extends EngineRuleEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEngineRuleEntity(PathMetadata metadata) {
        super(EngineRuleEntity.class, metadata);
    }

}


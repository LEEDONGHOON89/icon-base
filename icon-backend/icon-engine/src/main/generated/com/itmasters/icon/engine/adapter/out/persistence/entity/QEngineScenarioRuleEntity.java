package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEngineScenarioRuleEntity is a Querydsl query type for EngineScenarioRuleEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEngineScenarioRuleEntity extends EntityPathBase<EngineScenarioRuleEntity> {

    private static final long serialVersionUID = 400612450L;

    public static final QEngineScenarioRuleEntity engineScenarioRuleEntity = new QEngineScenarioRuleEntity("engineScenarioRuleEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final EnumPath<EngineScenarioRuleEntity.ScenarioOperator> operator = createEnum("operator", EngineScenarioRuleEntity.ScenarioOperator.class);

    public final NumberPath<Integer> orderNo = createNumber("orderNo", Integer.class);

    public final StringPath ruleId = createString("ruleId");

    public final StringPath scenarioId = createString("scenarioId");

    public final StringPath scenarioRuleId = createString("scenarioRuleId");

    public QEngineScenarioRuleEntity(String variable) {
        super(EngineScenarioRuleEntity.class, forVariable(variable));
    }

    public QEngineScenarioRuleEntity(Path<? extends EngineScenarioRuleEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEngineScenarioRuleEntity(PathMetadata metadata) {
        super(EngineScenarioRuleEntity.class, metadata);
    }

}


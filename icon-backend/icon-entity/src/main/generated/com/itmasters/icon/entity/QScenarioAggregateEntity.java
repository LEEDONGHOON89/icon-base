package com.itmasters.icon.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QScenarioAggregateEntity is a Querydsl query type for ScenarioAggregateEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QScenarioAggregateEntity extends EntityPathBase<ScenarioAggregateEntity> {

    private static final long serialVersionUID = -234208537L;

    public static final QScenarioAggregateEntity scenarioAggregateEntity = new QScenarioAggregateEntity("scenarioAggregateEntity");

    public final StringPath aggregateId = createString("aggregateId");

    public final EnumPath<com.itmasters.icon.common.domain.scenario.ScenarioOperator> operator = createEnum("operator", com.itmasters.icon.common.domain.scenario.ScenarioOperator.class);

    public final NumberPath<Integer> orderNo = createNumber("orderNo", Integer.class);

    public final StringPath scenarioAggregateId = createString("scenarioAggregateId");

    public final StringPath scenarioId = createString("scenarioId");

    public QScenarioAggregateEntity(String variable) {
        super(ScenarioAggregateEntity.class, forVariable(variable));
    }

    public QScenarioAggregateEntity(Path<? extends ScenarioAggregateEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QScenarioAggregateEntity(PathMetadata metadata) {
        super(ScenarioAggregateEntity.class, metadata);
    }

}


package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEngineScenarioEntity is a Querydsl query type for EngineScenarioEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEngineScenarioEntity extends EntityPathBase<EngineScenarioEntity> {

    private static final long serialVersionUID = 1103835078L;

    public static final QEngineScenarioEntity engineScenarioEntity = new QEngineScenarioEntity("engineScenarioEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath createdBy = createString("createdBy");

    public final NumberPath<Integer> dedupMinutes = createNumber("dedupMinutes", Integer.class);

    public final StringPath description = createString("description");

    public final StringPath entityFilterJson = createString("entityFilterJson");

    public final BooleanPath isActive = createBoolean("isActive");

    public final StringPath riskLevelId = createString("riskLevelId");

    public final StringPath scenarioId = createString("scenarioId");

    public final StringPath scenarioName = createString("scenarioName");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final StringPath updatedBy = createString("updatedBy");

    public QEngineScenarioEntity(String variable) {
        super(EngineScenarioEntity.class, forVariable(variable));
    }

    public QEngineScenarioEntity(Path<? extends EngineScenarioEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEngineScenarioEntity(PathMetadata metadata) {
        super(EngineScenarioEntity.class, metadata);
    }

}


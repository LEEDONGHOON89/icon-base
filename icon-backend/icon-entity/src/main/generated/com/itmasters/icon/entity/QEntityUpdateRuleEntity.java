package com.itmasters.icon.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEntityUpdateRuleEntity is a Querydsl query type for EntityUpdateRuleEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEntityUpdateRuleEntity extends EntityPathBase<EntityUpdateRuleEntity> {

    private static final long serialVersionUID = -2068724490L;

    public static final QEntityUpdateRuleEntity entityUpdateRuleEntity = new QEntityUpdateRuleEntity("entityUpdateRuleEntity");

    public final StringPath dataSourceId = createString("dataSourceId");

    public final StringPath deleteEntityIf = createString("deleteEntityIf");

    public final StringPath description = createString("description");

    public final StringPath entityFilter = createString("entityFilter");

    public final StringPath entityIdExpression = createString("entityIdExpression");

    public final StringPath entityType = createString("entityType");

    public final StringPath entityUpdateRuleId = createString("entityUpdateRuleId");

    public final StringPath eventCondition = createString("eventCondition");

    public final StringPath fieldName = createString("fieldName");

    public final StringPath fieldType = createString("fieldType");

    public final StringPath fieldValue = createString("fieldValue");

    public final StringPath fieldValueExpression = createString("fieldValueExpression");

    public final BooleanPath isActive = createBoolean("isActive");

    public final DateTimePath<java.time.LocalDateTime> regDt = createDateTime("regDt", java.time.LocalDateTime.class);

    public final StringPath scenarioId = createString("scenarioId");

    public final StringPath triggerType = createString("triggerType");

    public final StringPath updateType = createString("updateType");

    public final DateTimePath<java.time.LocalDateTime> updDt = createDateTime("updDt", java.time.LocalDateTime.class);

    public QEntityUpdateRuleEntity(String variable) {
        super(EntityUpdateRuleEntity.class, forVariable(variable));
    }

    public QEntityUpdateRuleEntity(Path<? extends EntityUpdateRuleEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEntityUpdateRuleEntity(PathMetadata metadata) {
        super(EntityUpdateRuleEntity.class, metadata);
    }

}


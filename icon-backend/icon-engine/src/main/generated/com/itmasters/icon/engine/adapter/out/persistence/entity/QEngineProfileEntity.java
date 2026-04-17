package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEngineProfileEntity is a Querydsl query type for EngineProfileEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEngineProfileEntity extends EntityPathBase<EngineProfileEntity> {

    private static final long serialVersionUID = -503673767L;

    public static final QEngineProfileEntity engineProfileEntity = new QEngineProfileEntity("engineProfileEntity");

    public final StringPath dataSourceId = createString("dataSourceId");

    public final StringPath description = createString("description");

    public final EnumPath<DestinationType> destinationType = createEnum("destinationType", DestinationType.class);

    public final NumberPath<Integer> displayOrder = createNumber("displayOrder", Integer.class);

    public final StringPath entityIdField = createString("entityIdField");

    public final EnumPath<com.itmasters.icon.common.domain.DomainEntityType> entityType = createEnum("entityType", com.itmasters.icon.common.domain.DomainEntityType.class);

    public final BooleanPath isActive = createBoolean("isActive");

    public final StringPath profileId = createString("profileId");

    public final StringPath profileName = createString("profileName");

    public final StringPath profilePurpose = createString("profilePurpose");

    public final NumberPath<Integer> ruleCount = createNumber("ruleCount", Integer.class);

    public final NumberPath<Integer> schemaCount = createNumber("schemaCount", Integer.class);

    public final ListPath<String, StringPath> storeFields = this.<String, StringPath>createList("storeFields", String.class, StringPath.class, PathInits.DIRECT2);

    public final StringPath timestampKey = createString("timestampKey");

    public QEngineProfileEntity(String variable) {
        super(EngineProfileEntity.class, forVariable(variable));
    }

    public QEngineProfileEntity(Path<? extends EngineProfileEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEngineProfileEntity(PathMetadata metadata) {
        super(EngineProfileEntity.class, metadata);
    }

}


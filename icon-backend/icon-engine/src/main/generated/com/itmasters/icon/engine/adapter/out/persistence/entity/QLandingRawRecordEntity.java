package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLandingRawRecordEntity is a Querydsl query type for LandingRawRecordEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLandingRawRecordEntity extends EntityPathBase<LandingRawRecordEntity> {

    private static final long serialVersionUID = -37988906L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLandingRawRecordEntity landingRawRecordEntity = new QLandingRawRecordEntity("landingRawRecordEntity");

    public final StringPath batchKey = createString("batchKey");

    public final StringPath dataSourceId = createString("dataSourceId");

    public final QExecDsMpEntity execDsMp;

    public final DateTimePath<java.time.LocalDateTime> extractedAt = createDateTime("extractedAt", java.time.LocalDateTime.class);

    public final StringPath fileName = createString("fileName");

    public final StringPath filePath = createString("filePath");

    public final StringPath ingestionMessage = createString("ingestionMessage");

    public final EnumPath<LandingRawRecordEntity.IngestionStatus> ingestionStatus = createEnum("ingestionStatus", LandingRawRecordEntity.IngestionStatus.class);

    public final NumberPath<Long> landingRecordId = createNumber("landingRecordId", Long.class);

    public final MapPath<String, Object, SimplePath<Object>> rawPayload = this.<String, Object, SimplePath<Object>>createMap("rawPayload", String.class, Object.class, SimplePath.class);

    public final NumberPath<Integer> rowIndex = createNumber("rowIndex", Integer.class);

    public final EnumPath<com.itmasters.icon.common.domain.type.DataSourceType> sourceType = createEnum("sourceType", com.itmasters.icon.common.domain.type.DataSourceType.class);

    public QLandingRawRecordEntity(String variable) {
        this(LandingRawRecordEntity.class, forVariable(variable), INITS);
    }

    public QLandingRawRecordEntity(Path<? extends LandingRawRecordEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLandingRawRecordEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLandingRawRecordEntity(PathMetadata metadata, PathInits inits) {
        this(LandingRawRecordEntity.class, metadata, inits);
    }

    public QLandingRawRecordEntity(Class<? extends LandingRawRecordEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.execDsMp = inits.isInitialized("execDsMp") ? new QExecDsMpEntity(forProperty("execDsMp")) : null;
    }

}


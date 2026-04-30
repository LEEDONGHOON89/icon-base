package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRiskLevelEntity is a Querydsl query type for RiskLevelEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRiskLevelEntity extends EntityPathBase<RiskLevelEntity> {

    private static final long serialVersionUID = -1850248633L;

    public static final QRiskLevelEntity riskLevelEntity = new QRiskLevelEntity("riskLevelEntity");

    public final StringPath actionType = createString("actionType");

    public final StringPath colorCode = createString("colorCode");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath description = createString("description");

    public final NumberPath<Integer> displayOrder = createNumber("displayOrder", Integer.class);

    public final BooleanPath isActive = createBoolean("isActive");

    public final NumberPath<Integer> levelCode = createNumber("levelCode", Integer.class);

    public final StringPath levelName = createString("levelName");

    public final BooleanPath notificationRequired = createBoolean("notificationRequired");

    public final StringPath riskLevelId = createString("riskLevelId");

    public QRiskLevelEntity(String variable) {
        super(RiskLevelEntity.class, forVariable(variable));
    }

    public QRiskLevelEntity(Path<? extends RiskLevelEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRiskLevelEntity(PathMetadata metadata) {
        super(RiskLevelEntity.class, metadata);
    }

}


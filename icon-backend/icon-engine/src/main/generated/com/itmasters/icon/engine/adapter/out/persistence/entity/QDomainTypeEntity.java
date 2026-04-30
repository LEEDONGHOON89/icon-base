package com.itmasters.icon.engine.adapter.out.persistence.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDomainTypeEntity is a Querydsl query type for DomainTypeEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDomainTypeEntity extends EntityPathBase<DomainTypeEntity> {

    private static final long serialVersionUID = 47682930L;

    public static final QDomainTypeEntity domainTypeEntity = new QDomainTypeEntity("domainTypeEntity");

    public final StringPath color = createString("color");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath description = createString("description");

    public final NumberPath<Integer> displayOrder = createNumber("displayOrder", Integer.class);

    public final StringPath domainName = createString("domainName");

    public final StringPath domainTypeId = createString("domainTypeId");

    public final StringPath icon = createString("icon");

    public final BooleanPath isActive = createBoolean("isActive");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QDomainTypeEntity(String variable) {
        super(DomainTypeEntity.class, forVariable(variable));
    }

    public QDomainTypeEntity(Path<? extends DomainTypeEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDomainTypeEntity(PathMetadata metadata) {
        super(DomainTypeEntity.class, metadata);
    }

}


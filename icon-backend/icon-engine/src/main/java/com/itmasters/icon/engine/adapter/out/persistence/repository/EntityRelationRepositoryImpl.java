package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityRelationEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QEntityRelationEntity;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Entity Relations 커스텀 Repository 구현체
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
@Repository
@RequiredArgsConstructor
public class EntityRelationRepositoryImpl implements EntityRelationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<EntityRelationEntity> findSharedDevices(String relationType, String toEntityType) {
        QEntityRelationEntity r = QEntityRelationEntity.entityRelationEntity;
        QEntityRelationEntity r2 = new QEntityRelationEntity("r2");

        // 서브쿼리: 2개 이상의 관계를 가진 to_entity_id 찾기
        return queryFactory
                .selectFrom(r)
                .where(
                        r.relationType.eq(relationType),
                        r.toEntityType.eq(toEntityType),
                        r.toEntityId.in(
                                JPAExpressions
                                        .select(r2.toEntityId)
                                        .from(r2)
                                        .where(
                                                r2.relationType.eq(relationType),
                                                r2.toEntityType.eq(toEntityType)
                                        )
                                        .groupBy(r2.toEntityId)
                                        .having(r2.count().gt(1))
                        )
                )
                .orderBy(r.toEntityId.asc(), r.fromEntityId.asc())
                .fetch();
    }
}

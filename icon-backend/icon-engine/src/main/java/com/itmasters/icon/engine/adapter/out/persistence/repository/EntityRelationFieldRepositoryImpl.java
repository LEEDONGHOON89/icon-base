package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityRelationFieldEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QEntityRelationFieldEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Entity Relation Fields 커스텀 Repository 구현체
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
@Repository
@RequiredArgsConstructor
public class EntityRelationFieldRepositoryImpl implements EntityRelationFieldRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<EntityRelationFieldEntity> findByDataSourceIdAndRoles(String dataSourceId, List<String> roles) {
        QEntityRelationFieldEntity q = QEntityRelationFieldEntity.entityRelationFieldEntity;

        return queryFactory
                .selectFrom(q)
                .where(
                        q.dataSourceId.eq(dataSourceId),
                        q.isEnabled.isTrue(),
                        q.fieldRole.in(roles)
                )
                .orderBy(q.priority.desc())
                .fetch();
    }
}

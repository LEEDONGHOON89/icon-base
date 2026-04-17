package com.itmasters.icon.api.derivedfield.adapter.persistence;

import com.itmasters.icon.entity.DerivedRuleEntity;
import com.itmasters.icon.entity.QDerivedRuleEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 파생 필드 규칙 커스텀 Repository 구현체
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
@Repository
@RequiredArgsConstructor
public class DerivedRuleRepositoryImpl implements DerivedRuleRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<DerivedRuleEntity> findActiveRulesByDataSourceId(String dataSourceId) {
        QDerivedRuleEntity q = QDerivedRuleEntity.derivedRuleEntity;

        return queryFactory
                .selectFrom(q)
                .where(
                        q.dataSourceId.eq(dataSourceId),
                        q.isActive.isTrue()
                )
                .orderBy(q.priority.asc(), q.ruleId.asc())
                .fetch();
    }
}

package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineRuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QEngineRuleEntity;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 엔진 룰 Repository 구현체
 * QueryDSL을 사용한 상세 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class EngineRuleRepositoryImpl implements EngineRuleRepository {

    private final JPAQueryFactory queryFactory;


    @Override
    public List<EngineRuleEntity> findActiveRulesByProfileId(String profileId) {
        // profile 매핑 폐지: 전역 활성 룰을 반환
        QEngineRuleEntity rule = QEngineRuleEntity.engineRuleEntity;
        return queryFactory
                .selectFrom(rule)
                .where(isActiveTrue())
                .orderBy(rule.ruleId.asc())
                .fetch();
    }

    // toDomain 메서드 제거 - 엔티티를 직접 반환

    @Override
    public EngineRuleEntity findById(String ruleId) {
        if (ruleId == null) {
            return null;
        }

        QEngineRuleEntity rule = QEngineRuleEntity.engineRuleEntity;

        EngineRuleEntity entity = queryFactory
                .selectFrom(rule)
                .where(rule.ruleId.eq(ruleId))
                .fetchOne();

        return entity;
    }

    @Override
    public List<EngineRuleEntity> findAllByProfileId(String profileId) {
        // profile 매핑 폐지: 전역 룰 목록 반환(활성 우선)
        QEngineRuleEntity rule = QEngineRuleEntity.engineRuleEntity;
        return queryFactory
                .selectFrom(rule)
                .orderBy(
                        rule.isActive.desc(),
                        rule.ruleId.asc()
                )
                .fetch();
    }

    @Override
    public List<EngineRuleEntity> findAllActive() {
        QEngineRuleEntity rule = QEngineRuleEntity.engineRuleEntity;
        return queryFactory
                .selectFrom(rule)
                .where(isActiveTrue())
                .orderBy(rule.ruleId.asc())
                .fetch();
    }

    // Category 관련 메서드는 EngineRuleEntity에 category 필드가 없으므로 제거
    // 필요시 추후 추가

    /**
     * 룰 이름으로 검색
     */
    public List<EngineRuleEntity> searchByRuleName(String profileId, String keyword) {
        // profile 매핑 폐지: 전역 활성 룰 이름 검색
        QEngineRuleEntity rule = QEngineRuleEntity.engineRuleEntity;
        return queryFactory
                .selectFrom(rule)
                .where(
                        ruleNameContains(keyword),
                        isActiveTrue()
                )
                .orderBy(rule.ruleName.asc())
                .fetch();
    }

    // QueryDSL 조건 메서드들
    // category 필드는 EngineRuleEntity에 없으므로 제거
    // 필요시 추후 추가

    private BooleanExpression ruleNameContains(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }
        QEngineRuleEntity rule = QEngineRuleEntity.engineRuleEntity;
        return rule.ruleName.containsIgnoreCase(keyword);
    }

    private BooleanExpression isActiveTrue() {
        QEngineRuleEntity rule = QEngineRuleEntity.engineRuleEntity;
        return rule.isActive.eq(true);
    }

}

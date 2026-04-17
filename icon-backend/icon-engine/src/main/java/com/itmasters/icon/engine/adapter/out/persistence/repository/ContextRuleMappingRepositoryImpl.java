package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.ContextRuleMappingEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QContextRuleMappingEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 컨텍스트-룰 매핑 저장소 구현
 */
@Repository
@RequiredArgsConstructor
public class ContextRuleMappingRepositoryImpl implements ContextRuleMappingRepository {
    
    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;
    
    private final QContextRuleMappingEntity qMapping = QContextRuleMappingEntity.contextRuleMappingEntity;
    
    @Override
    public ContextRuleMappingEntity save(ContextRuleMappingEntity entity) {
        if (entity.getContextRuleMappingId() == null) {
            entityManager.persist(entity);
            return entity;
        } else {
            return entityManager.merge(entity);
        }
    }
    
    @Override
    public Optional<ContextRuleMappingEntity> findById(Long contextRuleMappingId) {
        ContextRuleMappingEntity result = queryFactory
                .selectFrom(qMapping)
                .where(qMapping.contextRuleMappingId.eq(contextRuleMappingId))
                .fetchOne();
        return Optional.ofNullable(result);
    }
    
    @Override
    public List<ContextRuleMappingEntity> findByDetectionContextId(Long detectionContextId) {
        return queryFactory
                .selectFrom(qMapping)
                .where(qMapping.detectionContextId.eq(detectionContextId))
                .orderBy(qMapping.detectionTimestamp.desc())
                .fetch();
    }
    
    @Override
    public List<ContextRuleMappingEntity> findByRuleId(String ruleId) {
        return queryFactory
                .selectFrom(qMapping)
                .where(qMapping.ruleId.eq(ruleId))
                .orderBy(qMapping.detectionTimestamp.desc())
                .fetch();
    }
    
    @Override
    public Optional<ContextRuleMappingEntity> findByDetectionContextIdAndRuleId(Long detectionContextId, String ruleId) {
        ContextRuleMappingEntity result = queryFactory
                .selectFrom(qMapping)
                .where(qMapping.detectionContextId.eq(detectionContextId)
                        .and(qMapping.ruleId.eq(ruleId)))
                .fetchOne();
        return Optional.ofNullable(result);
    }
    
    @Override
    public List<ContextRuleMappingEntity> findActiveByDetectionContextId(Long detectionContextId) {
        return queryFactory
                .selectFrom(qMapping)
                .where(qMapping.detectionContextId.eq(detectionContextId))
                .orderBy(qMapping.detectionTimestamp.desc())
                .fetch();
    }
    
    @Override
    public List<ContextRuleMappingEntity> saveAll(List<ContextRuleMappingEntity> entities) {
        List<ContextRuleMappingEntity> savedEntities = new ArrayList<>();
        for (ContextRuleMappingEntity entity : entities) {
            savedEntities.add(save(entity));
        }
        entityManager.flush();
        return savedEntities;
    }
    
    @Override
    public void deleteById(Long contextRuleMappingId) {
        queryFactory
                .delete(qMapping)
                .where(qMapping.contextRuleMappingId.eq(contextRuleMappingId))
                .execute();
    }
    
    @Override
    public int deleteByDetectionContextId(Long detectionContextId) {
        return (int) queryFactory
                .delete(qMapping)
                .where(qMapping.detectionContextId.eq(detectionContextId))
                .execute();
    }
}
package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectionContextEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QDetectionContextEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 시나리오 탐지 컨텍스트 저장소 구현
 */
@Repository
@RequiredArgsConstructor
public class DetectionContextRepositoryImpl implements DetectionContextRepository {
    
    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;
    
    private final QDetectionContextEntity qContext = QDetectionContextEntity.detectionContextEntity;
    
    @Override
    public DetectionContextEntity save(DetectionContextEntity entity) {
        if (entity.getDetectionContextId() == null) {
            entityManager.persist(entity);
            return entity;
        } else {
            return entityManager.merge(entity);
        }
    }
    
    @Override
    public Optional<DetectionContextEntity> findById(Long contextId) {
        DetectionContextEntity result = queryFactory
                .selectFrom(qContext)
                .where(qContext.detectionContextId.eq(contextId))
                .fetchOne();
        return Optional.ofNullable(result);
    }
    
    @Override
    public Optional<DetectionContextEntity> findActiveByCorrelationKey(String correlationKey) {
        DetectionContextEntity result = queryFactory
                .selectFrom(qContext)
                .where(qContext.correlationKey.eq(correlationKey)
                        .and(qContext.status.eq("ACTIVE")))
                .orderBy(qContext.createdAt.desc())
                .fetchFirst();
        return Optional.ofNullable(result);
    }
    
    // @Override - 인터페이스에 정의되지 않은 추가 메서드
    public List<DetectionContextEntity> findActiveByProfileId(String profileId) {
        return queryFactory
                .selectFrom(qContext)
                .where(qContext.profileId.eq(profileId)
                        .and(qContext.status.eq("ACTIVE")))
                .orderBy(qContext.createdAt.desc())
                .fetch();
    }
    
    @Override
    public List<DetectionContextEntity> findExpiredContexts(LocalDateTime expiredBefore) {
        return queryFactory
                .selectFrom(qContext)
                .where(qContext.windowEnd.lt(expiredBefore)
                        .and(qContext.status.eq("ACTIVE")))
                .fetch();
    }
    
    @Override
    public void updateStatus(Long contextId, String status) {
        queryFactory
                .update(qContext)
                .set(qContext.status, status)
                .set(qContext.updatedAt, LocalDateTime.now())
                .where(qContext.detectionContextId.eq(contextId))
                .execute();
    }
    
    @Override
    public void deleteById(Long contextId) {
        queryFactory
                .delete(qContext)
                .where(qContext.detectionContextId.eq(contextId))
                .execute();
    }
    
    @Override
    public int deleteExpiredContexts(LocalDateTime expiredBefore) {
        return (int) queryFactory
                .delete(qContext)
                .where(qContext.windowEnd.lt(expiredBefore))
                .execute();
    }
}
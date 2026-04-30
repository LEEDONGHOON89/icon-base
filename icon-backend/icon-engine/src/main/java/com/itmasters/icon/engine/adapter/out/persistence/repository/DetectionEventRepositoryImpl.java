package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectionEventEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QDetectionEventEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 시나리오 탐지 이벤트 저장소 구현
 */
@Repository
@RequiredArgsConstructor
public class DetectionEventRepositoryImpl implements DetectionEventRepository {
    
    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;
    
    private final QDetectionEventEntity qEvent = QDetectionEventEntity.detectionEventEntity;

    @Override
    public DetectionEventEntity save(DetectionEventEntity entity) {
        if (entity.getEventId() == null) {
            entityManager.persist(entity);
            return entity;
        } else {
            return entityManager.merge(entity);
        }
    }
    
    @Override
    public Optional<DetectionEventEntity> findById(Long eventId) {
        DetectionEventEntity result = queryFactory
                .selectFrom(qEvent)
                .where(qEvent.eventId.eq(eventId))
                .fetchOne();
        return Optional.ofNullable(result);
    }
    
    @Override
    public List<DetectionEventEntity> findByContextId(Long contextId) {
        return queryFactory
                .selectFrom(qEvent)
                .where(qEvent.contextId.eq(contextId))
                .orderBy(qEvent.eventTimestamp.asc())
                .fetch();
    }
    
    @Override
    public List<DetectionEventEntity> findByRuleId(String ruleId) {
        return queryFactory
                .selectFrom(qEvent)
                .where(qEvent.ruleId.eq(ruleId))
                .orderBy(qEvent.eventTimestamp.desc())
                .fetch();
    }
    
    @Override
    public List<DetectionEventEntity> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return queryFactory
                .selectFrom(qEvent)
                .where(qEvent.eventTimestamp.between(startTime, endTime))
                .orderBy(qEvent.eventTimestamp.asc())
                .fetch();
    }
    
    @Override
    public long countByContextId(Long contextId) {
        return queryFactory
                .select(qEvent.count())
                .from(qEvent)
                .where(qEvent.contextId.eq(contextId))
                .fetchOne();
    }
    
    @Override
    public List<DetectionEventEntity> findByEventType(String eventType) {
        return queryFactory
                .selectFrom(qEvent)
                .where(qEvent.eventType.eq(eventType))
                .orderBy(qEvent.eventTimestamp.desc())
                .fetch();
    }
    
    @Override
    public List<DetectionEventEntity> saveAll(List<DetectionEventEntity> entities) {
        List<DetectionEventEntity> savedEntities = new ArrayList<>();
        for (DetectionEventEntity entity : entities) {
            savedEntities.add(save(entity));
        }
        entityManager.flush();
        return savedEntities;
    }
    
    @Override
    public int deleteByContextId(Long contextId) {
        return (int) queryFactory
                .delete(qEvent)
                .where(qEvent.contextId.eq(contextId))
                .execute();
    }
}
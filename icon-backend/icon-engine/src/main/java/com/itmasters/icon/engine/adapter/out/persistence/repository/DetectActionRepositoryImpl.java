package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectActionEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectActionEntity.ActionStatus;
import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectActionEntity.ActionType;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QDetectActionEntity;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 탐지 조치(Action) Repository 구현체 (QueryDSL)
 */
@Repository
@RequiredArgsConstructor
public class DetectActionRepositoryImpl implements DetectActionRepository {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public DetectActionEntity save(DetectActionEntity action) {
        if (action.getDetectActionId() == null) {
            entityManager.persist(action);
            return action;
        } else {
            return entityManager.merge(action);
        }
    }

    @Override
    public Optional<DetectActionEntity> findById(Long detectActionId) {
        QDetectActionEntity q = QDetectActionEntity.detectActionEntity;
        DetectActionEntity result = queryFactory.selectFrom(q)
                .where(q.detectActionId.eq(detectActionId))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public List<DetectActionEntity> findByDetectScenarioId(Long detectScenarioId) {
        QDetectActionEntity q = QDetectActionEntity.detectActionEntity;
        return queryFactory.selectFrom(q)
                .where(q.detectScenarioId.eq(detectScenarioId))
                .orderBy(q.requestedAt.desc())
                .fetch();
    }

    @Override
    public List<DetectActionEntity> findByGroupKey(String groupKey) {
        QDetectActionEntity q = QDetectActionEntity.detectActionEntity;
        return queryFactory.selectFrom(q)
                .where(q.groupKey.eq(groupKey))
                .orderBy(q.requestedAt.desc())
                .fetch();
    }

    @Override
    public List<DetectActionEntity> findByScenarioIdAndGroupKey(String scenarioId, String groupKey) {
        QDetectActionEntity q = QDetectActionEntity.detectActionEntity;
        return queryFactory.selectFrom(q)
                .where(q.scenarioId.eq(scenarioId)
                        .and(q.groupKey.eq(groupKey)))
                .orderBy(q.requestedAt.desc())
                .fetch();
    }

    @Override
    public List<DetectActionEntity> findByActionStatus(ActionStatus status, int limit) {
        QDetectActionEntity q = QDetectActionEntity.detectActionEntity;
        return queryFactory.selectFrom(q)
                .where(q.actionStatus.eq(status))
                .orderBy(q.requestedAt.desc())
                .limit(limit > 0 ? limit : 100)
                .fetch();
    }

    @Override
    public List<DetectActionEntity> findByActionType(ActionType actionType, int limit) {
        QDetectActionEntity q = QDetectActionEntity.detectActionEntity;
        return queryFactory.selectFrom(q)
                .where(q.actionType.eq(actionType))
                .orderBy(q.requestedAt.desc())
                .limit(limit > 0 ? limit : 100)
                .fetch();
    }

    @Override
    public List<DetectActionEntity> findPendingActions(int limit) {
        return findByActionStatus(ActionStatus.PENDING, limit);
    }

    @Override
    public List<DetectActionEntity> findByRequestedBy(String requestedBy, int limit) {
        QDetectActionEntity q = QDetectActionEntity.detectActionEntity;
        return queryFactory.selectFrom(q)
                .where(q.requestedBy.eq(requestedBy))
                .orderBy(q.requestedAt.desc())
                .limit(limit > 0 ? limit : 100)
                .fetch();
    }

    @Override
    public List<DetectActionEntity> findByApprovedBy(String approvedBy, int limit) {
        QDetectActionEntity q = QDetectActionEntity.detectActionEntity;
        return queryFactory.selectFrom(q)
                .where(q.approvedBy.eq(approvedBy))
                .orderBy(q.approvedAt.desc())
                .limit(limit > 0 ? limit : 100)
                .fetch();
    }

    @Override
    public List<DetectActionEntity> findByRequestedAtBetween(
            LocalDateTime startInclusive,
            LocalDateTime endInclusive,
            int limit) {
        QDetectActionEntity q = QDetectActionEntity.detectActionEntity;
        return queryFactory.selectFrom(q)
                .where(q.requestedAt.goe(startInclusive)
                        .and(q.requestedAt.loe(endInclusive)))
                .orderBy(q.requestedAt.desc())
                .limit(limit > 0 ? limit : 100)
                .fetch();
    }

    @Override
    public Map<ActionStatus, Long> countByActionStatus() {
        QDetectActionEntity q = QDetectActionEntity.detectActionEntity;
        List<Tuple> results = queryFactory
                .select(q.actionStatus, q.detectActionId.count())
                .from(q)
                .groupBy(q.actionStatus)
                .fetch();

        Map<ActionStatus, Long> countMap = new EnumMap<>(ActionStatus.class);
        for (Tuple tuple : results) {
            ActionStatus status = tuple.get(q.actionStatus);
            Long count = tuple.get(q.detectActionId.count());
            if (status != null && count != null) {
                countMap.put(status, count);
            }
        }
        return countMap;
    }

    @Override
    public Map<ActionType, Long> countByActionType() {
        QDetectActionEntity q = QDetectActionEntity.detectActionEntity;
        List<Tuple> results = queryFactory
                .select(q.actionType, q.detectActionId.count())
                .from(q)
                .groupBy(q.actionType)
                .fetch();

        Map<ActionType, Long> countMap = new EnumMap<>(ActionType.class);
        for (Tuple tuple : results) {
            ActionType type = tuple.get(q.actionType);
            Long count = tuple.get(q.detectActionId.count());
            if (type != null && count != null) {
                countMap.put(type, count);
            }
        }
        return countMap;
    }

    @Override
    public long countByDetectScenarioId(Long detectScenarioId) {
        QDetectActionEntity q = QDetectActionEntity.detectActionEntity;
        Long count = queryFactory
                .select(q.detectActionId.count())
                .from(q)
                .where(q.detectScenarioId.eq(detectScenarioId))
                .fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public long countByGroupKey(String groupKey) {
        QDetectActionEntity q = QDetectActionEntity.detectActionEntity;
        Long count = queryFactory
                .select(q.detectActionId.count())
                .from(q)
                .where(q.groupKey.eq(groupKey))
                .fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public long countPendingActions() {
        QDetectActionEntity q = QDetectActionEntity.detectActionEntity;
        Long count = queryFactory
                .select(q.detectActionId.count())
                .from(q)
                .where(q.actionStatus.eq(ActionStatus.PENDING))
                .fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public void delete(DetectActionEntity action) {
        entityManager.remove(action);
    }

    @Override
    public void deleteById(Long detectActionId) {
        QDetectActionEntity q = QDetectActionEntity.detectActionEntity;
        queryFactory.delete(q)
                .where(q.detectActionId.eq(detectActionId))
                .execute();
    }
}

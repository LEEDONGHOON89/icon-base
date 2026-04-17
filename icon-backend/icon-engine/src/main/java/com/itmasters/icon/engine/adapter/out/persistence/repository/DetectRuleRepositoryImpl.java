package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectRuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QDetectRuleEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class DetectRuleRepositoryImpl implements DetectRuleRepository {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public DetectRuleEntity save(DetectRuleEntity entity) {
        if(entity.getDetectRuleId() == null){
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);
    }



    @Override
    public List<DetectRuleEntity> findByMappedStorageId(Long mappedStorageId) {
        QDetectRuleEntity q = QDetectRuleEntity.detectRuleEntity;
        return queryFactory
                .selectFrom(q)
                .where(q.mappedStorageId.eq(mappedStorageId))
                .orderBy(q.detectedDt.asc())
                .fetch();
    }

    @Override
    public List<DetectRuleEntity> findActiveAggregatesByGroupKeys(Set<String> groupKeys) {
        QDetectRuleEntity q = QDetectRuleEntity.detectRuleEntity;
        return queryFactory
                .selectFrom(q)
                .where(
                    q.groupKey.in(groupKeys),
                    q.pass.eq(true)
                    // 시간 필터 제거: aggregate의 pass=true는 이미 시간 조건을 포함하므로 추가 필터 불필요
                )
                .orderBy(q.groupKey.asc(), q.detectedDt.asc())
                .fetch();
    }

    @Override
    public List<DetectRuleEntity> findByTransactionId(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            return List.of();
        }
        
        QDetectRuleEntity q = QDetectRuleEntity.detectRuleEntity;
        return queryFactory
                .selectFrom(q)
                .where(q.transactionId.eq(transactionId))
                .orderBy(q.detectedDt.asc())
                .fetch();
    }
}


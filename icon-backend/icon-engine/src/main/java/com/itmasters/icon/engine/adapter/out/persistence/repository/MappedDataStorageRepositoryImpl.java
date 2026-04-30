package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.MappedDataStorageEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QLandingRawRecordEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QMappedDataStorageEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * DataSourceSchema 기반 매핑 데이터 저장소 Repository 구현체
 * QueryDSL을 사용한 구현
 */
@Slf4j
@Repository
@Transactional
public class MappedDataStorageRepositoryImpl implements MappedDataStorageRepository {
    
    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;
    
    public MappedDataStorageRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.queryFactory = new JPAQueryFactory(entityManager);
    }
    
    @Override
    public MappedDataStorageEntity save(MappedDataStorageEntity entity) {
        if (entity.getMappedDataStorageId() == null) {
            entityManager.persist(entity);
            return entity;
        } else {
            return entityManager.merge(entity);
        }
    }
    
    @Override
    public Optional<MappedDataStorageEntity> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(MappedDataStorageEntity.class, id));
    }

    @Override
    public MappedDataStorageEntity getReference(Long id) {
        if (id == null) {
            return null;
        }
        try {
            return entityManager.getReference(MappedDataStorageEntity.class, id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    @Override
    public List<MappedDataStorageEntity> saveAll(List<MappedDataStorageEntity> entities) {
        for (MappedDataStorageEntity entity : entities) {
            if (entity.getMappedDataStorageId() == null) {
                entityManager.persist(entity);
            } else {
                entityManager.merge(entity);
            }
        }
        entityManager.flush(); // 배치 처리를 위해 flush
        return entities;
    }
    
    @Override
    public List<MappedDataStorageEntity> findByExecDsMpIdOrderByRowIndex(Long execDsMpId) {
        if (execDsMpId == null) {
            return List.of();
        }

        QMappedDataStorageEntity storage = QMappedDataStorageEntity.mappedDataStorageEntity;
        QLandingRawRecordEntity landing = QLandingRawRecordEntity.landingRawRecordEntity;

        return queryFactory
                .selectFrom(storage)
                .join(landing).on(storage.landingRecordId.eq(landing.landingRecordId))
                .where(landing.execDsMp.execDsMpId.eq(execDsMpId))
                .orderBy(storage.rowIndex.asc())
                .fetch();
    }

    @Override
    public long countByExecDsMpId(Long execDsMpId) {
        if (execDsMpId == null) {
            return 0L;
        }

        QMappedDataStorageEntity storage = QMappedDataStorageEntity.mappedDataStorageEntity;
        QLandingRawRecordEntity landing = QLandingRawRecordEntity.landingRawRecordEntity;

        Long result = queryFactory
                .select(storage.count())
                .from(storage)
                .join(landing).on(storage.landingRecordId.eq(landing.landingRecordId))
                .where(landing.execDsMp.execDsMpId.eq(execDsMpId))
                .fetchOne();

        return result != null ? result : 0L;
    }

    @Override
    public void deleteByExecDsMpId(Long execDsMpId) {
        if (execDsMpId == null) {
            return;
        }

        // QueryDSL delete는 join을 지원하지 않으므로 서브쿼리 사용
        QLandingRawRecordEntity landing = QLandingRawRecordEntity.landingRawRecordEntity;

        List<Long> landingRecordIds = queryFactory
                .select(landing.landingRecordId)
                .from(landing)
                .where(landing.execDsMp.execDsMpId.eq(execDsMpId))
                .fetch();

        if (!landingRecordIds.isEmpty()) {
            QMappedDataStorageEntity storage = QMappedDataStorageEntity.mappedDataStorageEntity;
            queryFactory
                    .delete(storage)
                    .where(storage.landingRecordId.in(landingRecordIds))
                    .execute();
        }
    }
    
    @Override
    public long count() {
        QMappedDataStorageEntity storage = QMappedDataStorageEntity.mappedDataStorageEntity;
        
        Long result = queryFactory
                .select(storage.count())
                .from(storage)
                .fetchOne();
                
        return result != null ? result : 0L;
    }
    
    @Override
    public List<MappedDataStorageEntity> findAll() {
        QMappedDataStorageEntity storage = QMappedDataStorageEntity.mappedDataStorageEntity;
        
        return queryFactory
                .selectFrom(storage)
                .orderBy(storage.mappedDataStorageId.desc())
                .fetch();
    }
    
    @Override
    public List<MappedDataStorageEntity> findByTransactionId(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            return List.of();
        }
        
        QMappedDataStorageEntity storage = QMappedDataStorageEntity.mappedDataStorageEntity;
        
        return queryFactory
                .selectFrom(storage)
                .where(storage.transactionId.eq(transactionId))
                .orderBy(storage.regDt.asc())
                .fetch();
    }
    
    @Override
    public List<Object[]> findRecentTransactions(int limit) {
        // QueryDSL을 사용한 transaction_id별 집계 쿼리 (landing_records 조인)
        QMappedDataStorageEntity storage = QMappedDataStorageEntity.mappedDataStorageEntity;
        QLandingRawRecordEntity landing = QLandingRawRecordEntity.landingRawRecordEntity;

        List<com.querydsl.core.Tuple> results = queryFactory
                .select(
                        storage.transactionId,
                        landing.dataSourceId,
                        storage.regDt.min(),
                        storage.regDt.max(),
                        storage.count()
                )
                .from(storage)
                .join(landing).on(storage.landingRecordId.eq(landing.landingRecordId))
                .where(storage.transactionId.isNotNull())
                .groupBy(storage.transactionId, landing.dataSourceId)
                .orderBy(storage.regDt.max().desc())
                .limit(limit)
                .fetch();

        // Tuple을 Object[]로 변환
        return results.stream()
                .map(tuple -> new Object[]{
                        tuple.get(storage.transactionId),
                        tuple.get(landing.dataSourceId),
                        tuple.get(storage.regDt.min()),
                        tuple.get(storage.regDt.max()),
                        tuple.get(storage.count())
                })
                .toList();
    }
}

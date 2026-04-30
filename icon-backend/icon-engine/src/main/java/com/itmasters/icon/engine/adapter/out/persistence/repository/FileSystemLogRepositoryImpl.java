package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDsFileSystemLogEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QEngineDsFileSystemLogEntity;
import com.itmasters.icon.engine.datasource.repository.FileSystemLogRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FileSystemLogRepositoryImpl implements FileSystemLogRepository {

    private final JPAQueryFactory queryFactory;
    private final EntityManager em;

    private final QEngineDsFileSystemLogEntity qLog = QEngineDsFileSystemLogEntity.engineDsFileSystemLogEntity;

    @Override
    public EngineDsFileSystemLogEntity save(EngineDsFileSystemLogEntity log) {
        EngineDsFileSystemLogEntity existing = em.find(EngineDsFileSystemLogEntity.class, log.getId());
        if (existing == null) em.persist(log); else em.merge(log);
        em.flush();
        return findById(log.getId()).orElse(log);
    }

    @Override
    public Optional<EngineDsFileSystemLogEntity> findById(String id) {
        EngineDsFileSystemLogEntity e = queryFactory
                .selectFrom(qLog)
                .where(qLog.id.eq(id))
                .fetchOne();
        return Optional.ofNullable(e);
    }

    @Override
    public boolean isAlreadyProcessed(String filePath) {
        Long cnt = queryFactory
                .select(qLog.count())
                .from(qLog)
                .where(qLog.filePath.eq(filePath))
                .fetchOne();
        return cnt != null && cnt > 0;
    }

    @Override
    public boolean isSuccessfullyProcessed(String filePath) {
        Long cnt = queryFactory
                .select(qLog.count())
                .from(qLog)
                .where(qLog.filePath.eq(filePath)
                        .and(qLog.processingStatus.eq(EngineDsFileSystemLogEntity.ProcessingStatus.SUCCESS)))
                .fetchOne();
        return cnt != null && cnt > 0;
    }

    @Override
    public List<EngineDsFileSystemLogEntity> findByConfigId(String configId) {
        return queryFactory
                .selectFrom(qLog)
                .where(qLog.configId.eq(configId))
                .orderBy(qLog.processedAt.desc())
                .fetch();
    }

    @Override
    public List<EngineDsFileSystemLogEntity> findSuccessfulProcessingByConfigId(String configId) {
        return queryFactory
                .selectFrom(qLog)
                .where(qLog.configId.eq(configId)
                        .and(qLog.processingStatus.eq(EngineDsFileSystemLogEntity.ProcessingStatus.SUCCESS)))
                .orderBy(qLog.processedAt.desc())
                .fetch();
    }

    @Override
    public List<EngineDsFileSystemLogEntity> findByConfigIdAndDateRange(String configId, LocalDateTime startDate, LocalDateTime endDate) {
        return queryFactory
                .selectFrom(qLog)
                .where(qLog.configId.eq(configId)
                        .and(qLog.processedAt.between(startDate, endDate)))
                .orderBy(qLog.processedAt.desc())
                .fetch();
    }

    @Override
    public Optional<EngineDsFileSystemLogEntity> findLatestSuccessfulProcessing(String configId) {
        EngineDsFileSystemLogEntity e = queryFactory
                .selectFrom(qLog)
                .where(qLog.configId.eq(configId)
                        .and(qLog.processingStatus.eq(EngineDsFileSystemLogEntity.ProcessingStatus.SUCCESS)))
                .orderBy(qLog.processedAt.desc())
                .limit(1)
                .fetchOne();
        return Optional.ofNullable(e);
    }

    @Override
    public List<EngineDsFileSystemLogEntity> findFailedProcessingByConfigId(String configId) {
        return queryFactory
                .selectFrom(qLog)
                .where(qLog.configId.eq(configId)
                        .and(qLog.processingStatus.eq(EngineDsFileSystemLogEntity.ProcessingStatus.FAILED)))
                .orderBy(qLog.processedAt.desc())
                .fetch();
    }
}

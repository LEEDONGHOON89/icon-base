package com.itmasters.icon.api.datasource.adapter.out.persistence.repository;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.FileSystemLogEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.FileSystemLogEntity.ProcessingStatus;
import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.QFileSystemLogEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 파일 시스템 로그 커스텀 Repository 구현체
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
@Repository
@RequiredArgsConstructor
public class FileSystemLogJpaRepositoryImpl implements FileSystemLogJpaRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsSuccessfulProcessingByFilePath(String filePath) {
        QFileSystemLogEntity q = QFileSystemLogEntity.fileSystemLogEntity;

        Long count = queryFactory
                .select(q.count())
                .from(q)
                .where(
                        q.filePath.eq(filePath),
                        q.processingStatus.eq(ProcessingStatus.SUCCESS)
                )
                .fetchOne();

        return count != null && count > 0;
    }

    @Override
    public List<FileSystemLogEntity> findSuccessfulProcessingByConfigId(String configId) {
        QFileSystemLogEntity q = QFileSystemLogEntity.fileSystemLogEntity;

        return queryFactory
                .selectFrom(q)
                .where(
                        q.dsFileSystemConfigId.eq(configId),
                        q.processingStatus.eq(ProcessingStatus.SUCCESS)
                )
                .orderBy(q.processedAt.desc())
                .fetch();
    }

    @Override
    public List<FileSystemLogEntity> findByConfigIdAndDateRange(String configId,
                                                                LocalDateTime startDate,
                                                                LocalDateTime endDate) {
        QFileSystemLogEntity q = QFileSystemLogEntity.fileSystemLogEntity;

        return queryFactory
                .selectFrom(q)
                .where(
                        q.dsFileSystemConfigId.eq(configId),
                        q.processedAt.between(startDate, endDate)
                )
                .orderBy(q.processedAt.desc())
                .fetch();
    }

    @Override
    public Optional<FileSystemLogEntity> findLatestSuccessfulProcessing(String configId) {
        QFileSystemLogEntity q = QFileSystemLogEntity.fileSystemLogEntity;

        FileSystemLogEntity result = queryFactory
                .selectFrom(q)
                .where(
                        q.dsFileSystemConfigId.eq(configId),
                        q.processingStatus.eq(ProcessingStatus.SUCCESS)
                )
                .orderBy(q.processedAt.desc())
                .fetchFirst();  // LIMIT 1과 동일

        return Optional.ofNullable(result);
    }

    @Override
    public List<FileSystemLogEntity> findFailedProcessingByConfigId(String configId) {
        QFileSystemLogEntity q = QFileSystemLogEntity.fileSystemLogEntity;

        return queryFactory
                .selectFrom(q)
                .where(
                        q.dsFileSystemConfigId.eq(configId),
                        q.processingStatus.eq(ProcessingStatus.FAILED)
                )
                .orderBy(q.processedAt.desc())
                .fetch();
    }
}

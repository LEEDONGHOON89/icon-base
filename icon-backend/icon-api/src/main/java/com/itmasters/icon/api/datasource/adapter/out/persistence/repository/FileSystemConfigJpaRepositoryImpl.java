package com.itmasters.icon.api.datasource.adapter.out.persistence.repository;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.FileSystemConfigEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.QFileSystemConfigEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 파일 시스템 설정 커스텀 Repository 구현체
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
@Repository
@RequiredArgsConstructor
public class FileSystemConfigJpaRepositoryImpl implements FileSystemConfigJpaRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<FileSystemConfigEntity> findActiveByDataSourceId(String dataSourceId) {
        QFileSystemConfigEntity q = QFileSystemConfigEntity.fileSystemConfigEntity;

        FileSystemConfigEntity result = queryFactory
                .selectFrom(q)
                .where(
                        q.dataSourceId.eq(dataSourceId),
                        q.isActive.isTrue()
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }
}

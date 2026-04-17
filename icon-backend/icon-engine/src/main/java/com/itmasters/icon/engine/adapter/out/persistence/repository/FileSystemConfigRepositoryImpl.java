package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDsFileSystemConfigEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QEngineDsFileSystemConfigEntity;
import com.itmasters.icon.engine.datasource.repository.FileSystemConfigRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FileSystemConfigRepositoryImpl implements FileSystemConfigRepository {

    private final JPAQueryFactory queryFactory;
    private final EntityManager em;

    private final QEngineDsFileSystemConfigEntity qConfig = QEngineDsFileSystemConfigEntity.engineDsFileSystemConfigEntity;

    @Override
    public EngineDsFileSystemConfigEntity save(EngineDsFileSystemConfigEntity config) {
        EngineDsFileSystemConfigEntity existing = em.find(EngineDsFileSystemConfigEntity.class, config.getId());
        if (existing == null) {
            em.persist(config);
        } else {
            em.merge(config);
        }
        em.flush();
        return findById(config.getId()).orElse(config);
    }

    @Override
    public Optional<EngineDsFileSystemConfigEntity> findById(String id) {
        EngineDsFileSystemConfigEntity e = queryFactory
                .selectFrom(qConfig)
                .where(qConfig.id.eq(id))
                .fetchOne();
        return Optional.ofNullable(e);
    }

    @Override
    public Optional<EngineDsFileSystemConfigEntity> findByDataSourceId(String dataSourceId) {
        EngineDsFileSystemConfigEntity e = queryFactory
                .selectFrom(qConfig)
                .where(qConfig.dataSourceId.eq(dataSourceId))
                .orderBy(qConfig.createdAt.asc())
                .limit(1)
                .fetchOne();
        return Optional.ofNullable(e);
    }

    @Override
    public List<EngineDsFileSystemConfigEntity> findAllByDataSourceId(String dataSourceId) {
        return queryFactory
                .selectFrom(qConfig)
                .where(qConfig.dataSourceId.eq(dataSourceId))
                .orderBy(qConfig.createdAt.asc())
                .fetch();
    }

    @Override
    public List<EngineDsFileSystemConfigEntity> findActiveConfigs() {
        return queryFactory
                .selectFrom(qConfig)
                .where(qConfig.isActive.isTrue())
                .fetch();
    }

    @Override
    public Optional<EngineDsFileSystemConfigEntity> findActiveByDataSourceId(String dataSourceId) {
        EngineDsFileSystemConfigEntity e = queryFactory
                .selectFrom(qConfig)
                .where(qConfig.dataSourceId.eq(dataSourceId).and(qConfig.isActive.isTrue()))
                .orderBy(qConfig.createdAt.asc())
                .limit(1)
                .fetchOne();
        return Optional.ofNullable(e);
    }

    @Override
    public void deleteById(String id) {
        EngineDsFileSystemConfigEntity ref = em.getReference(EngineDsFileSystemConfigEntity.class, id);
        em.remove(ref);
    }

    @Override
    public List<EngineDsFileSystemConfigEntity> findAll() {
        return queryFactory
                .selectFrom(qConfig)
                .orderBy(qConfig.createdAt.asc())
                .fetch();
    }
}

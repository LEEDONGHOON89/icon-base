package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.ExecDsMpEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QExecDsMpEntity;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 데이터소스 필드매핑 실행 로그 Repository 구현체 (QueryDSL)
 */
@Repository
@RequiredArgsConstructor
public class ExecDsMpRepositoryImpl implements ExecDsMpRepository {

    private final EntityManager entityManager;
    private final JPAQueryFactory queryFactory;

    private final QExecDsMpEntity execDsMp = QExecDsMpEntity.execDsMpEntity;

    @Override
    @Transactional
    public ExecDsMpEntity save(ExecDsMpEntity entity) {
        // BIGSERIAL이므로 새로 생성할 때는 ID가 null
        if (entity.getExecDsMpId() == null) {
            entityManager.persist(entity);
        } else {
            entity = entityManager.merge(entity);
        }
        entityManager.flush();
        return entity;
    }

    @Override
    public Optional<ExecDsMpEntity> findById(Long execDsMpId) {
        return Optional.ofNullable(
                queryFactory.selectFrom(execDsMp)
                        .where(execDsMp.execDsMpId.eq(execDsMpId))
                        .fetchOne()
        );
    }

    @Override
    public List<ExecDsMpEntity> findRecentByDataSourceId(String dataSourceId, int limit) {
        return queryFactory.selectFrom(execDsMp)
                .where(execDsMp.dataSourceId.eq(dataSourceId))
                .orderBy(execDsMp.startDt.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<ExecDsMpEntity> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        BooleanExpression dateCondition = execDsMp.startDt.between(startDate, endDate);

        return queryFactory.selectFrom(execDsMp)
                .where(dateCondition)
                .orderBy(execDsMp.startDt.desc())
                .fetch();
    }

    @Override
    public List<ExecDsMpEntity> findByStatus(String status) {
        return queryFactory.selectFrom(execDsMp)
                .where(execDsMp.status.stringValue().eq(status))
                .orderBy(execDsMp.startDt.desc())
                .fetch();
    }

    @Override
    public List<ExecDsMpEntity> findRunningExecutions() {
        return queryFactory.selectFrom(execDsMp)
                .where(execDsMp.status.stringValue().eq("RUNNING"))
                .orderBy(execDsMp.startDt.asc())
                .fetch();
    }

    @Override
    public Optional<ExecDsMpEntity> findLastSuccessfulExecution(String dataSourceId) {
        return Optional.ofNullable(
                queryFactory.selectFrom(execDsMp)
                        .where(
                                execDsMp.dataSourceId.eq(dataSourceId),
                                execDsMp.status.stringValue().eq("SUCCESS")
                        )
                        .orderBy(execDsMp.completeAt.desc())
                        .fetchFirst()
        );
    }

    public List<ExecDsMpEntity> findAllActive() {
        return queryFactory
                .selectFrom(execDsMp)
                .fetch();
    }
}
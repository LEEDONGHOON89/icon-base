package com.itmasters.icon.api.rule.adapter.persistence.repository;

import com.itmasters.icon.common.domain.EntityType;
import com.itmasters.icon.common.domain.IdGenerator;
import com.itmasters.icon.api.rule.adapter.persistence.entity.SensorEntity;
import com.itmasters.icon.api.rule.application.port.out.SensorRepository;
import com.itmasters.icon.api.common.domain.RuleCategory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.itmasters.icon.api.rule.adapter.persistence.entity.QSensorEntity.sensorEntity;

@Repository
@RequiredArgsConstructor
public class SensorRepositoryImpl implements SensorRepository {

    private final JpaSensorRepository jpaSensorRepository;
    private final JPAQueryFactory queryFactory;
    private final IdGenerator idGenerator;


    @Override
    public SensorEntity save(SensorEntity entity) {
        // 새로운 엔티티인 경우 ID 할당
        if (entity.getSensorId() == null) {
            entity.assignId(idGenerator.generateId(EntityType.RULE));
        }

        return jpaSensorRepository.save(entity);
    }


    @Override
    public Optional<SensorEntity> findById(String sensorId) {
        return jpaSensorRepository.findById(sensorId);
    }

    @Override
    public List<SensorEntity> findAll() {
        return queryFactory.selectFrom(sensorEntity)
                .orderBy(sensorEntity.sensorId.desc())
                .fetch();
    }

    public List<SensorEntity> findFiltered(String q, Boolean active, String sort, String dir, int offset, int limit) {
        var query = queryFactory.selectFrom(sensorEntity);
        if (q != null && !q.isBlank()) {
            query.where(sensorEntity.sensorName.containsIgnoreCase(q));
        }
        if (active != null) {
            query.where(sensorEntity.isActive.eq(active));
        }
        // sort
        boolean asc = (dir == null || dir.equalsIgnoreCase("asc"));
        if ("name".equalsIgnoreCase(sort)) {
            query.orderBy(asc ? sensorEntity.sensorName.asc() : sensorEntity.sensorName.desc());
        } else if ("id".equalsIgnoreCase(sort)) {
            query.orderBy(asc ? sensorEntity.sensorId.asc() : sensorEntity.sensorId.desc());
        } else {
            query.orderBy(asc ? sensorEntity.sensorName.asc() : sensorEntity.sensorName.desc());
        }
        if (offset >= 0) query.offset(offset);
        if (limit > 0) query.limit(limit);
        return query.fetch();
    }

    public long countFiltered(String q, Boolean active) {
        var query = queryFactory.selectFrom(sensorEntity);
        if (q != null && !q.isBlank()) {
            query.where(sensorEntity.sensorName.containsIgnoreCase(q));
        }
        if (active != null) {
            query.where(sensorEntity.isActive.eq(active));
        }
        return query.fetchCount();
    }

    @Override
    public List<SensorEntity> findByCategory(String category) {
        // v4.0 schema no longer persists category; return all and let client-side filter if needed
        return queryFactory.selectFrom(sensorEntity)
                .orderBy(sensorEntity.sensorName.asc())
                .fetch();
    }

    @Override
    public List<SensorEntity> findByIsActive(boolean isActive) {
        return queryFactory.selectFrom(sensorEntity)
                .where(sensorEntity.isActive.eq(isActive))
                .orderBy(sensorEntity.sensorName.asc())
                .fetch();
    }

    @Override
    public boolean existsById(String sensorId) {
        return jpaSensorRepository.existsById(sensorId);
    }


    /**
     * 카테고리별 활성화된 센서 조회 (QueryDSL)
     */
    public List<SensorEntity> findActiveByCategory(RuleCategory category) {
        // v4.0 schema no longer persists category; return active sensors only
        return queryFactory.selectFrom(sensorEntity)
                .where(sensorEntity.isActive.eq(true))
                .orderBy(sensorEntity.sensorName.asc())
                .fetch();
    }

    /**
     * 센서명으로 검색 (QueryDSL)
     */
    public List<SensorEntity> findBySensorNameContaining(String name) {
        return queryFactory.selectFrom(sensorEntity)
                .where(sensorEntity.sensorName.containsIgnoreCase(name))
                .orderBy(sensorEntity.sensorName.asc())
                .fetch();
    }

    public List<SensorEntity> findByIds(List<String> sensorIds) {
        if (sensorIds == null || sensorIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return queryFactory.selectFrom(sensorEntity)
                .where(sensorEntity.sensorId.in(sensorIds))
                .fetch();
    }
}

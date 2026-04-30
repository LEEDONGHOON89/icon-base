package com.itmasters.icon.api.datasource.adapter.out.persistence;

import com.itmasters.icon.common.domain.EntityType;
import com.itmasters.icon.common.domain.IdGenerator;
import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DataSourceEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.DataSourceJpaRepository;
import com.itmasters.icon.api.datasource.application.port.out.DataSourceRepository;
import com.itmasters.icon.common.domain.type.DataSourceType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.itmasters.icon.api.datasource.adapter.out.persistence.entity.QDataSourceEntity.dataSourceEntity;

/**
 * DataSource Repository 구현체 (단순화됨)
 */
@Repository
@RequiredArgsConstructor
public class DataSourceRepositoryImpl implements DataSourceRepository {

    private final DataSourceJpaRepository jpaRepository;
    private final JPAQueryFactory queryFactory;
    private final IdGenerator idGenerator;

    @Override
    public Optional<DataSourceEntity> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<DataSourceEntity> findAll() {
        return queryFactory.selectFrom(dataSourceEntity)
                .orderBy(dataSourceEntity.dataSourceId.asc())
                .fetch();
    }

    @Override
    public List<DataSourceEntity> findBySourceType(DataSourceType sourceType) {
        return jpaRepository.findBySourceType(sourceType);
    }

    @Override
    public Optional<DataSourceEntity> findByName(String name) {
        return jpaRepository.findByName(name);
    }

    @Override
    public DataSourceEntity save(DataSourceEntity dataSourceEntity) {
        // ID가 없는 경우 새로 생성
        if (dataSourceEntity.getDataSourceId() == null) {
            dataSourceEntity.assignId(idGenerator.generateId(EntityType.DATA_SOURCE));
        }
        
        return jpaRepository.save(dataSourceEntity);
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void delete(DataSourceEntity dataSourceEntity) {
        jpaRepository.delete(dataSourceEntity);
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.findByName(name).isPresent();
    }
}

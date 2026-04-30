package com.itmasters.icon.api.dataprofile.adapter.out.persistence.repository;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.QDataSourceEntity;
import com.itmasters.icon.api.dataprofile.adapter.out.persistence.entity.ProfileEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.itmasters.icon.api.dataprofile.adapter.out.persistence.entity.QProfileEntity.profileEntity;

/**
 * 데이터 프로파일 QueryDSL Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class DataProfileQueryDslRepositoryImpl implements DataProfileQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    // QueryDSL Q클래스
    private final QDataSourceEntity dataSourceEntity = QDataSourceEntity.dataSourceEntity;

    /**
     * 데이터소스별 프로파일 목록 조회 (활성화된 것만)
     */
    @Override
    public List<ProfileEntity> findByDataSourceIdAndIsActiveTrue(String dataSourceId) {
        return queryFactory
                .selectFrom(profileEntity)
                .leftJoin(profileEntity.dataSource, dataSourceEntity).fetchJoin()
                .where(
                        dataSourceEntity.dataSourceId.eq(dataSourceId)
                                .and(profileEntity.isActive.isTrue())
                )
                .orderBy(
                        profileEntity.displayOrder.asc(),
                        profileEntity.profileName.asc()
                )
                .fetch();
    }

    /**
     * 데이터소스별 프로파일 목록 조회 (전체)
     */
    @Override
    public List<ProfileEntity> findByDataSourceId(String dataSourceId) {
        return queryFactory
                .selectFrom(profileEntity)
                .leftJoin(profileEntity.dataSource, dataSourceEntity).fetchJoin()
                .where(dataSourceEntity.dataSourceId.eq(dataSourceId))
                .orderBy(
                        profileEntity.displayOrder.asc(),
                        profileEntity.profileName.asc()
                )
                .fetch();
    }

    /**
     * 데이터소스별 프로파일명 중복 체크
     */
    @Override
    public Optional<ProfileEntity> findByDataSourceIdAndProfileName(String dataSourceId, String profileName) {
        ProfileEntity result = queryFactory
                .selectFrom(profileEntity)
                .leftJoin(profileEntity.dataSource, dataSourceEntity).fetchJoin()
                .where(
                        dataSourceEntity.dataSourceId.eq(dataSourceId)
                                .and(profileEntity.profileName.eq(profileName))
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * 데이터소스별 프로파일명 중복 체크 (특정 프로파일 제외)
     */
    @Override
    public Optional<ProfileEntity> findByDataSourceIdAndProfileNameAndProfileIdNot(
            String dataSourceId, String profileName, String excludeProfileId) {
        ProfileEntity result = queryFactory
                .selectFrom(profileEntity)
                .leftJoin(profileEntity.dataSource, dataSourceEntity).fetchJoin()
                .where(
                        dataSourceEntity.dataSourceId.eq(dataSourceId)
                                .and(profileEntity.profileName.eq(profileName))
                                .and(profileEntity.profileId.ne(excludeProfileId))
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * 프로파일 ID로 조회 (데이터소스 정보 포함)
     */
    @Override
    public Optional<ProfileEntity> findByIdWithDataSource(String profileId) {
        ProfileEntity result = queryFactory
                .selectFrom(profileEntity)
                .leftJoin(profileEntity.dataSource, dataSourceEntity).fetchJoin()
                .where(profileEntity.profileId.eq(profileId))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
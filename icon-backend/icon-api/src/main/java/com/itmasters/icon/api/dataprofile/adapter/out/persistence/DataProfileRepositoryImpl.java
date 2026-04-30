package com.itmasters.icon.api.dataprofile.adapter.out.persistence;

import com.itmasters.icon.common.domain.EntityType;
import com.itmasters.icon.common.domain.IdGenerator;
import com.itmasters.icon.api.dataprofile.adapter.out.persistence.entity.ProfileEntity;
import com.itmasters.icon.api.dataprofile.adapter.out.persistence.repository.JpaDataProfileRepository;
import com.itmasters.icon.api.dataprofile.application.port.out.DataProfileRepository;
import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.QDataSourceEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.itmasters.icon.api.dataprofile.adapter.out.persistence.entity.QProfileEntity.profileEntity;

import java.util.List;
import java.util.Optional;

/**
 * 데이터 프로파일 Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class DataProfileRepositoryImpl implements DataProfileRepository {

    private final JpaDataProfileRepository jpaRepository;
    private final JPAQueryFactory queryFactory;
    private final IdGenerator idGenerator;
    
    // QueryDSL Q클래스
    private final QDataSourceEntity dataSourceEntity = QDataSourceEntity.dataSourceEntity;

    @Override
    public ProfileEntity save(ProfileEntity profile) {
        if (profile.getProfileId() == null) {
            profile.setProfileId(idGenerator.generateId(EntityType.PROFILE_SCHEMA));
        }
        return jpaRepository.save(profile);
    }

    @Override
    public Optional<ProfileEntity> findById(String profileId) {
        return jpaRepository.findById(profileId);
    }

    @Override
    public List<ProfileEntity> findByDataSourceId(String dataSourceId) {
        // QueryDSL 직접 사용
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

    @Override
    public List<ProfileEntity> findAllByDataSourceId(String dataSourceId) {
        // QueryDSL 직접 사용
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

    @Override
    public boolean existsByDataSourceIdAndProfileName(String dataSourceId, String profileName) {
        // QueryDSL 직접 사용
        ProfileEntity result = queryFactory
                .selectFrom(profileEntity)
                .leftJoin(profileEntity.dataSource, dataSourceEntity).fetchJoin()
                .where(
                        dataSourceEntity.dataSourceId.eq(dataSourceId)
                                .and(profileEntity.profileName.eq(profileName))
                )
                .fetchOne();
        
        return result != null;
    }

    @Override
    public boolean existsByDataSourceIdAndProfileNameExcluding(String dataSourceId, String profileName, String excludeProfileId) {
        // QueryDSL 직접 사용
        ProfileEntity result = queryFactory
                .selectFrom(profileEntity)
                .leftJoin(profileEntity.dataSource, dataSourceEntity).fetchJoin()
                .where(
                        dataSourceEntity.dataSourceId.eq(dataSourceId)
                                .and(profileEntity.profileName.eq(profileName))
                                .and(profileEntity.profileId.ne(excludeProfileId))
                )
                .fetchOne();
        
        return result != null;
    }

    @Override
    public void delete(String profileId) {
        jpaRepository.deleteById(profileId);
    }

    @Override
    public boolean existsById(String profileId) {
        return jpaRepository.existsById(profileId);
    }


    @Override
    public Optional<ProfileEntity> findDefaultProfileByDataSourceId(String dataSourceId) {
        // QueryDSL 직접 사용 - DEFAULT 프로파일 조회
        ProfileEntity result = queryFactory
                .selectFrom(profileEntity)
                .leftJoin(profileEntity.dataSource, dataSourceEntity).fetchJoin()
                .where(
                        dataSourceEntity.dataSourceId.eq(dataSourceId)
                                .and(profileEntity.profileName.eq("DEFAULT"))
                )
                .fetchOne();
        
        return Optional.ofNullable(result);
    }
    
    @Override
    public List<ProfileEntity> findByDataSourceIdAndProfileName(String dataSourceId, String profileName) {
        // QueryDSL 직접 사용 - 데이터소스ID와 프로파일명으로 조회
        return queryFactory
                .selectFrom(profileEntity)
                .leftJoin(profileEntity.dataSource, dataSourceEntity).fetchJoin()
                .where(
                        dataSourceEntity.dataSourceId.eq(dataSourceId)
                                .and(profileEntity.profileName.eq(profileName))
                )
                .fetch();
    }
}

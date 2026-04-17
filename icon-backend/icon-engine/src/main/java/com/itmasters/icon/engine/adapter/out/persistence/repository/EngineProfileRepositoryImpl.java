package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineProfileEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.QEngineProfileEntity;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 엔진 프로파일 Repository 구현체
 * QueryDSL을 사용한 상세 구현
 */
@Slf4j
@Repository
public class EngineProfileRepositoryImpl implements EngineProfileRepository {

    private final JPAQueryFactory queryFactory;

    public EngineProfileRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<EngineProfileEntity> findActiveProfilesByDataSourceId(String dataSourceId) {
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;

        return queryFactory
                .selectFrom(profile)
                .where(
                        dataSourceIdEq(dataSourceId),
                        isActiveTrue()
                )
                .orderBy(
                        displayOrderAsc(),
                        profile.profileId.asc()
                )
                .fetch();
    }

    @Override
    public Optional<EngineProfileEntity> findById(String profileId) {
        if (profileId == null) {
            return Optional.empty();
        }

        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;

        EngineProfileEntity entity = queryFactory
                .selectFrom(profile)
                .where(profile.profileId.eq(profileId))
                .fetchOne();
        
        return Optional.ofNullable(entity);
    }

    /**
     * 데이터소스의 모든 프로파일 조회 (활성/비활성 포함)
     */
    public List<EngineProfileEntity> findAllByDataSourceId(String dataSourceId) {
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;

        return queryFactory
                .selectFrom(profile)
                .where(dataSourceIdEq(dataSourceId))
                .orderBy(
                        profile.isActive.desc(),  // 활성화된 항목 먼저
                        displayOrderAsc(),
                        profile.profileId.asc()
                )
                .fetch();
    }

    /**
     * 프로파일 목적별 조회
     */
    public List<EngineProfileEntity> findActiveProfilesByPurpose(String profilePurpose) {
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;

        return queryFactory
                .selectFrom(profile)
                .where(
                        profilePurposeEq(profilePurpose),
                        isActiveTrue()
                )
                .orderBy(displayOrderAsc())
                .fetch();
    }

    /**
     * 프로파일 이름으로 검색
     */
    public List<EngineProfileEntity> searchByProfileName(String keyword) {
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;

        return queryFactory
                .selectFrom(profile)
                .where(
                        profileNameContains(keyword),
                        isActiveTrue()
                )
                .orderBy(profile.profileName.asc())
                .fetch();
    }

    /**
     * 데이터소스와 프로파일 이름으로 조회
     */
    public Optional<EngineProfileEntity> findByDataSourceIdAndProfileName(String dataSourceId, String profileName) {
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;

        EngineProfileEntity entity = queryFactory
                .selectFrom(profile)
                .where(
                        dataSourceIdEq(dataSourceId),
                        profileNameEq(profileName)
                )
                .fetchOne();

        return Optional.ofNullable(entity);
    }

    /**
     * 실행 순서가 지정된 프로파일 조회
     */
    public List<EngineProfileEntity> findActiveProfilesWithDisplayOrder(String dataSourceId) {
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;

        return queryFactory
                .selectFrom(profile)
                .where(
                        dataSourceIdEq(dataSourceId),
                        hasDisplayOrder(),
                        isActiveTrue()
                )
                .orderBy(displayOrderAsc())
                .fetch();
    }

    /**
     * 프로파일 개수 조회
     */
    public long countActiveProfilesByDataSourceId(String dataSourceId) {
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;

        return queryFactory
                .select(profile.count())
                .from(profile)
                .where(
                        dataSourceIdEq(dataSourceId),
                        isActiveTrue()
                )
                .fetchOne();
    }

    /**
     * 최근 업데이트된 프로파일 조회
     */
    public List<EngineProfileEntity> findRecentlyUpdatedProfiles(int limit) {
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;

        return queryFactory
                .selectFrom(profile)
                .where(isActiveTrue())
                .orderBy(profile.profileId.desc())
                .limit(limit)
                .fetch();
    }

    /**
     * 페이징을 지원하는 프로파일 조회
     */
    public List<EngineProfileEntity> findActiveProfilesWithPaging(String dataSourceId, int offset, int limit) {
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;

        return queryFactory
                .selectFrom(profile)
                .where(
                        dataSourceIdEq(dataSourceId),
                        isActiveTrue()
                )
                .orderBy(displayOrderAsc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    /**
     * detect_key가 설정된 프로파일 조회
     */
    public List<EngineProfileEntity> findProfilesWithDetectKey(String dataSourceId) {
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;

        return queryFactory
                .selectFrom(profile)
                .where(
                        dataSourceIdEq(dataSourceId),
                        hasDetectKey(),
                        isActiveTrue()
                )
                .orderBy(displayOrderAsc())
                .fetch();
    }

    /**
     * detect_key 타입별 프로파일 조회
     */
    public List<EngineProfileEntity> findProfilesByDetectKeyType(String detectKeyType) {
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;

        return queryFactory
                .selectFrom(profile)
                .where(
                        detectKeyTypeEq(detectKeyType),
                        isActiveTrue()
                )
                .orderBy(profile.profileId.asc())
                .fetch();
    }

    // QueryDSL 조건 메서드들
    private BooleanExpression dataSourceIdEq(String dataSourceId) {
        if (dataSourceId == null || dataSourceId.isEmpty()) {
            return null;
        }
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;
        return profile.dataSourceId.eq(dataSourceId);
    }

    private BooleanExpression profilePurposeEq(String profilePurpose) {
        if (profilePurpose == null || profilePurpose.isEmpty()) {
            return null;
        }
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;
        return profile.profilePurpose.eq(profilePurpose);
    }

    private BooleanExpression profileNameEq(String profileName) {
        if (profileName == null || profileName.isEmpty()) {
            return null;
        }
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;
        return profile.profileName.eq(profileName);
    }

    private BooleanExpression profileNameContains(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;
        return profile.profileName.containsIgnoreCase(keyword);
    }

    private BooleanExpression isActiveTrue() {
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;
        return profile.isActive.eq(true);
    }

    private BooleanExpression hasDisplayOrder() {
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;
        return profile.displayOrder.isNotNull();
    }

    private BooleanExpression hasDetectKey() {
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;
        // TODO: groupKey 컬럼 제거됨 - entity_id_field로 대체
        return profile.entityIdField.isNotNull().and(profile.entityIdField.isNotEmpty());
    }

    private BooleanExpression detectKeyTypeEq(String detectKeyType) {
        // TODO: groupKeyType 컬럼 제거됨 - 타입 체크 불가능, 항상 null 반환
        return null;
    }

    // 정렬 메서드들
    private OrderSpecifier<Integer> displayOrderAsc() {
        QEngineProfileEntity profile = QEngineProfileEntity.engineProfileEntity;
        return profile.displayOrder.asc().nullsLast();
    }

    // toDomain 메서드 제거 - 더 이상 필요 없음
    // 엔티티를 직접 반환
}
package com.itmasters.icon.api.user.adapter.persistence.repository;


import com.itmasters.icon.common.domain.EntityType;
import com.itmasters.icon.common.domain.IdGenerator;
import com.itmasters.icon.api.user.adapter.persistence.entity.UserEntity;
import com.itmasters.icon.api.user.application.port.out.UserRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.itmasters.icon.api.user.adapter.persistence.entity.QUserEntity.userEntity;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final JpaUserRepository jpaUserRepository;
    private final JPAQueryFactory queryFactory;
    private final IdGenerator idGenerator;

    @Override
    public UserEntity save(UserEntity user) {
        // 새로운 엔티티인 경우 ID 할당
        if (user.getUserId() == null) {
            user.assignId(idGenerator.generateId(EntityType.USER));
        }
        
        return jpaUserRepository.save(user);
    }

    @Override
    public Optional<UserEntity> findById(String userId) {
        return jpaUserRepository.findById(userId);
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        UserEntity entity =
                queryFactory.selectFrom(userEntity).where(userEntity.email.eq(email)).fetchOne();
        return Optional.ofNullable(entity);
    }

    @Override
    public Optional<UserEntity> findByLoginId(String loginId) {
        UserEntity entity =
                queryFactory.selectFrom(userEntity).where(userEntity.loginId.eq(loginId)).fetchOne();
        return Optional.ofNullable(entity);
    }

    @Override
    public List<UserEntity> findAll() {
        return queryFactory.selectFrom(userEntity).fetch();
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }

    @Override
    public void deleteById(String userId) {
        jpaUserRepository.deleteById(userId);
    }
}

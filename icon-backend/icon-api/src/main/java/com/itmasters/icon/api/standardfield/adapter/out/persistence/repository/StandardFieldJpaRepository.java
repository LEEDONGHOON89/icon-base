package com.itmasters.icon.api.standardfield.adapter.out.persistence.repository;

import com.itmasters.icon.api.standardfield.adapter.out.persistence.entity.StandardFieldEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 표준 필드 JPA Repository
 */
@Repository
public interface StandardFieldJpaRepository extends JpaRepository<StandardFieldEntity, String> {
    // 기본 JPA 메서드만 사용
}
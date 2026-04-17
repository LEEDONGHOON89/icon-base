package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DomainTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 도메인 타입 Repository
 */
@Repository
public interface DomainTypeRepository extends JpaRepository<DomainTypeEntity, String> {

    /**
     * 활성화 상태로 전체 조회 (표시 순서로 정렬)
     */
    List<DomainTypeEntity> findByIsActiveTrueOrderByDisplayOrder();

    /**
     * 활성화 상태로 조회
     */
    List<DomainTypeEntity> findByIsActive(Boolean isActive);

    /**
     * 도메인 이름으로 조회
     */
    Optional<DomainTypeEntity> findByDomainName(String domainName);

    /**
     * 도메인 타입 ID 존재 여부 확인
     */
    boolean existsByDomainTypeId(String domainTypeId);
}

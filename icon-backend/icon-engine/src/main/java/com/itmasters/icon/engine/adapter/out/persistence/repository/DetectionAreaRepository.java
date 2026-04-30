package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectionAreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 탐지영역 Repository
 */
@Repository
public interface DetectionAreaRepository extends JpaRepository<DetectionAreaEntity, String> {

    /**
     * 활성화 상태로 전체 조회 (표시 순서로 정렬)
     */
    List<DetectionAreaEntity> findByIsActiveTrueOrderByDisplayOrder();

    /**
     * 활성화 상태로 조회
     */
    List<DetectionAreaEntity> findByIsActive(Boolean isActive);

    /**
     * 탐지영역 이름으로 조회
     */
    Optional<DetectionAreaEntity> findByAreaName(String areaName);

    /**
     * 탐지영역 ID 존재 여부 확인
     */
    boolean existsByDetectionAreaId(String detectionAreaId);
}

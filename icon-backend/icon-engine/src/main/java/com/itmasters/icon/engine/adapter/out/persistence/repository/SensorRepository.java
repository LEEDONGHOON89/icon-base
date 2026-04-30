package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.SensorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Sensor Repository
 * sensors 테이블 조회 (읽기 전용)
 */
@Repository
public interface SensorRepository extends JpaRepository<SensorEntity, String> {

    /**
     * sensor_id로 센서 조회
     *
     * @param sensorId 센서 ID
     * @return 센서 엔티티
     */
    Optional<SensorEntity> findBySensorId(String sensorId);

    /**
     * 활성 센서 여부 확인
     *
     * @param sensorId 센서 ID
     * @return 활성 센서 존재 여부
     */
    boolean existsBySensorIdAndIsActiveTrue(String sensorId);
}

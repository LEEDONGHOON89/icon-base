package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectSensorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * DetectSensor Repository
 * detect_sensors 테이블 저장/조회
 */
@Repository
public interface DetectSensorRepository extends JpaRepository<DetectSensorEntity, Long> {
    
    /**
     * 중복 체크: 동일한 (sensor_id, mapped_storage_id) 조합이 이미 존재하는지 확인
     * - 같은 이벤트가 같은 센서에 여러 번 탐지되는 것을 방지
     * 
     * @param sensorId 센서 ID
     * @param mappedStorageId 매핑된 스토리지 ID
     * @return 존재하면 true, 아니면 false
     */
    boolean existsBySensorIdAndMappedStorageId(String sensorId, Long mappedStorageId);
}

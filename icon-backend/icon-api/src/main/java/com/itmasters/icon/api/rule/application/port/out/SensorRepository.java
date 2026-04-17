package com.itmasters.icon.api.rule.application.port.out;

import com.itmasters.icon.api.rule.adapter.persistence.entity.SensorEntity;
import java.util.List;
import java.util.Optional;

public interface SensorRepository {
    SensorEntity save(SensorEntity sensor);
    Optional<SensorEntity> findById(String sensorId);
    List<SensorEntity> findAll();
    List<SensorEntity> findByCategory(String category);
    List<SensorEntity> findByIsActive(boolean isActive);
    boolean existsById(String sensorId);
}

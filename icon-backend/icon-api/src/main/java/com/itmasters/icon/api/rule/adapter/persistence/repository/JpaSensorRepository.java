package com.itmasters.icon.api.rule.adapter.persistence.repository;

import com.itmasters.icon.api.rule.adapter.persistence.entity.SensorEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSensorRepository extends JpaRepository<SensorEntity, String> {
    // v4.0 schema: category is not a persisted attribute; avoid derived queries on it.
    List<SensorEntity> findByIsActive(Boolean isActive);

    // @Query 메서드들은 SensorRepositoryImpl에서 QueryDSL로 구현됨
}

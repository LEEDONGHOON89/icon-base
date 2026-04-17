package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Engine 모듈용 Sensor 엔티티
 * API 모듈의 SensorEntity와 동일한 테이블(sensors) 매핑
 * Engine에서는 읽기 전용으로만 사용 (탐지 로직에서 센서 조건 조회)
 */
@Entity(name = "EngineSensor")
@Table(name = "sensors")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SensorEntity {

    @Id
    @Column(name = "sensor_id", nullable = false)
    private String sensorId;

    @Column(name = "sensor_name", nullable = false, length = 255)
    private String sensorName;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "version", nullable = false)
    @Version
    private Integer version;

    /**
     * 센서 조건 JSON (신규 스키마)
     * 배열 형식: [{"fieldName": "profit_loss", "operator": "LESS_THAN_OR_EQUALS", "value": -1}]
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "where_json", columnDefinition = "jsonb")
    private String whereJson;
}

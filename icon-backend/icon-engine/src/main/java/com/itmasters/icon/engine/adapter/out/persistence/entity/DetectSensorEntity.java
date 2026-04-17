package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DetectSensors result entity (센서 탐지 결과)
 * - 센서 조건에 매칭된 이벤트 기록
 * - 테이블명: detect_sensors
 */
@Entity
@Table(name = "detect_sensors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectSensorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detect_sensor_id")
    private Integer detectSensorId;

    @Column(name = "mapped_storage_id")
    private Long mappedStorageId;

    @Column(name = "sensor_id", nullable = false, length = 50)
    private String sensorId;

    @Column(name = "row_number")
    private Integer rowNumber;

    @Column(name = "detected_dt")
    private LocalDateTime detectedDt;

    @Column(name = "event_dt")
    private LocalDateTime eventDt;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "matched_fields", columnDefinition = "jsonb")
    private String matchedFields;

    @Column(name = "group_key", length = 200)
    private String groupKey;

    @Column(name = "transaction_id", length = 255)
    private String transactionId;
}

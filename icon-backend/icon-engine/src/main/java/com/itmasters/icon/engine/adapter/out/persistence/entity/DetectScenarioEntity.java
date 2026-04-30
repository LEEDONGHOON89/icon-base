package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 시나리오 탐지 결과 엔티티 (detect_scenarios)
 */
@Entity
@Table(name = "detect_scenarios",
       indexes = {
           @Index(name = "idx_detect_scn_exec", columnList = "exec_ds_mp_id"),
           @Index(name = "idx_detect_scn_mapped_storage", columnList = "mapped_storage_id"),
           @Index(name = "idx_detect_scn_key_time", columnList = "group_key,detected_dt DESC"),
           @Index(name = "idx_detect_scn_scen_time", columnList = "scenario_id,detected_dt DESC"),
           @Index(name = "idx_detect_scn_landing_record", columnList = "landing_record_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectScenarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detect_scenario_id")
    private Long detectScenarioId;

    @Column(name = "group_key", length = 200)
    private String groupKey;

    @Column(name = "scenario_id", nullable = false, length = 100)
    private String scenarioId;

    @Column(name = "detected_dt", nullable = false)
    private LocalDateTime detectedDt;

    @Column(name = "event_dt")
    private LocalDateTime eventDt;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "window_start")
    private LocalDateTime windowStart;

    @Column(name = "window_end")
    private LocalDateTime windowEnd;

    @Column(name = "mapped_storage_id")
    private Long mappedStorageId;

    @Column(name = "transaction_id")
    private String transactionId;

    @PrePersist
    protected void onCreate() {
        if (detectedDt == null) detectedDt = LocalDateTime.now();
    }
}

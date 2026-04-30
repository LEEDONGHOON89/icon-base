package com.itmasters.icon.api.analytics.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "detect_scenarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiDetectScenarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detect_scenario_id")
    private Long detectScenarioId;

    @Column(name = "scenario_id", nullable = false, length = 100)
    private String scenarioId;

    @Column(name = "group_key", length = 200)
    private String groupKey;

    @Column(name = "detected_dt", nullable = false)
    private LocalDateTime detectedDt;

    @Column(name = "event_dt")
    private LocalDateTime eventDt;

    @Column(name = "window_start")
    private LocalDateTime windowStart;

    @Column(name = "window_end")
    private LocalDateTime windowEnd;

    @Column(name = "mapped_storage_id")
    private Long mappedStorageId;

    @Column(name = "transaction_id", length = 255)
    private String transactionId;
}

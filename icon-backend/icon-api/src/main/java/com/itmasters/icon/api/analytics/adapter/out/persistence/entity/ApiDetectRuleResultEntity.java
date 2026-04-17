package com.itmasters.icon.api.analytics.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * detect_rule_results 조회용 엔티티.
 */
@Entity
@Table(name = "detect_rule_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiDetectRuleResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detect_rule_result_id")
    private Long detectRuleResultId;

    @Column(name = "exec_ds_mp_id", nullable = false)
    private Long execDsMpId;

    @Column(name = "profile_id", nullable = false, length = 50)
    private String profileId;

    @Column(name = "execution_id", nullable = false, length = 100)
    private String executionId;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "total_matched")
    private Integer totalMatched;

    @Column(name = "total_rules")
    private Integer totalRules;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;
}

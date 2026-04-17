package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "engine_execution_warnings",
        indexes = {
                @Index(name = "idx_warn_exec", columnList = "exec_ds_mp_id"),
                @Index(name = "idx_warn_step", columnList = "step")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EngineExecutionWarningEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warning_id")
    private Long warningId;

    @Column(name = "exec_ds_mp_id", nullable = false)
    private Long execDsMpId;

    @Column(name = "step", length = 20, nullable = false)
    private String step; // e.g., STEP4

    @Column(name = "code", length = 50, nullable = false)
    private String code; // e.g., NO_GROUP_KEYS

    @Column(name = "message")
    private String message;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}


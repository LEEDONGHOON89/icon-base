package com.itmasters.icon.api.analytics.adapter.out.persistence.entity;

import com.itmasters.icon.common.domain.type.ExecutionMode;
import com.itmasters.icon.common.domain.type.ExecutionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * exec_ds_mp 실행 이력을 조회하기 위한 API 모듈용 엔티티.
 */
@Entity
@Table(name = "exec_ds_mp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiExecDsMpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exec_ds_mp_id")
    private Long execDsMpId;

    @Column(name = "data_source_id", nullable = false, length = 20)
    private String dataSourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_mode", length = 20)
    private ExecutionMode executionMode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_context", columnDefinition = "jsonb")
    private Map<String, Object> executionContext;

    @Column(name = "start_dt", nullable = false)
    private LocalDateTime startDt;

    @Column(name = "complete_at")
    private LocalDateTime completeAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExecutionStatus status;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "executed_by", length = 100)
    private String executedBy;
}

package com.itmasters.icon.engine.adapter.out.persistence.entity;

import com.itmasters.icon.common.domain.type.ExecutionMode;
import com.itmasters.icon.common.domain.type.ExecutionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 데이터소스 필드매핑 실행 로그 엔티티
 * exec_ds_mp 테이블과 매핑
 */
@Entity
@Table(name = "exec_ds_mp")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ExecDsMpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exec_ds_mp_id")
    private Long execDsMpId;

    @Column(name = "data_source_id", nullable = false, length = 20)
    private String dataSourceId;

    @Column(name = "execution_mode", length = 20)
    @Enumerated(EnumType.STRING)
    private ExecutionMode executionMode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_context", columnDefinition = "jsonb")
    private Map<String, Object> executionContext;

    @Column(name = "start_dt", nullable = false)
    private LocalDateTime startDt;

    @Column(name = "complete_at")
    private LocalDateTime completeAt;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "executed_by", length = 100)
    private String executedBy;


    /**
     * 실행 완료 처리
     */
    public void complete() {
        if (this.status == ExecutionStatus.RUNNING) {
            this.status = ExecutionStatus.SUCCESS;
            this.completeAt = LocalDateTime.now();
        }
    }

    /**
     * 실행 실패 처리
     */
    public void fail(String errorMessage) {
        this.completeAt = LocalDateTime.now();
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void updateTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }
}
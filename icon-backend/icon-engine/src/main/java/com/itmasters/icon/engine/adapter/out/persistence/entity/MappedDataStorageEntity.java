package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DataSourceSchema 기반 매핑된 데이터 저장 엔티티
 * - 도메인 없이 엔티티로만 구성 (KISS 원칙)
 * - DataSourceSchema로 매핑된 데이터를 개별 row로 저장
 * - landing_records를 통해 exec_ds_mp 참조 (exec_ds_mp_id 직접 참조 제거)
 */
@Entity
@Table(name = "mapped_storages",
        indexes = {
                @Index(name = "idx_mapped_storage_landing", columnList = "landing_record_id"),
                @Index(name = "idx_mapped_storages_tx_id", columnList = "transaction_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"rowData"})
public class MappedDataStorageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapped_storage_id")
    private Long mappedDataStorageId;

    @Column(name = "row_index", nullable = false)
    private Integer rowIndex;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "row_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> rowData;

    @Column(name = "reg_dt")
    private LocalDateTime regDt;

    @Column(name = "landing_record_id", nullable = false)
    private Long landingRecordId;

    /**
     * 실행 ID (exec_ds_mp 직접 참조)
     */
    @Column(name = "exec_ds_mp_id")
    private Long execDsMpId;

    /**
     * 처리 상태 (개별 레코드 트랜잭션 추적용)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", length = 20)
    @Builder.Default
    private ProcessingStatus processingStatus = ProcessingStatus.NEW;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "transaction_id")
    private String transactionId;

    public enum ProcessingStatus {
        NEW,           // 신규 생성
        PROCESSING,    // 처리 중
        COMPLETED,     // 처리 완료
        FAILED         // 처리 실패
    }

    public void markProcessing() {
        this.processingStatus = ProcessingStatus.PROCESSING;
        this.errorMessage = null;
    }

    public void markCompleted() {
        this.processingStatus = ProcessingStatus.COMPLETED;
        this.errorMessage = null;
    }

    public void markFailed(String message) {
        this.processingStatus = ProcessingStatus.FAILED;
        this.errorMessage = message;
    }
}

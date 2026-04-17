package com.itmasters.icon.api.analytics.adapter.out.persistence.entity;

import com.itmasters.icon.common.domain.type.DataSourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * landing_records 테이블 조회용 엔티티.
 */
@Entity
@Table(name = "landing_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiLandingRawRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "landing_record_id")
    private Long landingRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exec_ds_mp_id", nullable = false)
    private ApiExecDsMpEntity execDsMp;

    @Column(name = "data_source_id", nullable = false, length = 50)
    private String dataSourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private DataSourceType sourceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rawPayload;

    @Column(name = "row_index")
    private Integer rowIndex;

    @Column(name = "batch_key", length = 100)
    private String batchKey;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_name")
    private String fileName;

    @CreationTimestamp
    @Column(name = "extracted_at", nullable = false, updatable = false)
    private LocalDateTime extractedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingestion_status", nullable = false, length = 20)
    private IngestionStatus ingestionStatus;

    @Column(name = "ingestion_message")
    private String ingestionMessage;

    public enum IngestionStatus {
        NEW,
        TRANSFORMED,
        FAILED
    }
}


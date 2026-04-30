package com.itmasters.icon.engine.adapter.out.persistence.entity;

import com.itmasters.icon.common.domain.type.DataSourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Landing 영역 원본 데이터 저장 엔티티
 */
@Entity
@Table(name = "landing_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class LandingRawRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "landing_record_id")
    private Long landingRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exec_ds_mp_id", nullable = false)
    private ExecDsMpEntity execDsMp;

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
    @Builder.Default
    private IngestionStatus ingestionStatus = IngestionStatus.NEW;

    @Comment("변환 상태 및 오류 메시지")
    @Column(name = "ingestion_message")
    private String ingestionMessage;

    public enum IngestionStatus {
        NEW,
        TRANSFORMED,
        FAILED
    }

    public void markTransformed() {
        this.ingestionStatus = IngestionStatus.TRANSFORMED;
        this.ingestionMessage = null;
    }

    public void markFailed(String message) {
        this.ingestionStatus = IngestionStatus.FAILED;
        this.ingestionMessage = message;
    }
}

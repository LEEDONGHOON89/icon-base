package com.itmasters.icon.engine.adapter.out.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 엔티티 원본 데이터 이력
 *
 * 목적: DataSource에서 수신한 모든 row를 그대로 보관
 *
 * 특징:
 * - entity_attributes는 최신 상태만 유지 (UPSERT)
 * - entity_source_records는 모든 이력 보관 (INSERT만)
 * - 시계열 분석, 변경 추적, 데이터 복원 등에 활용
 *
 * 예시:
 * DataSource: DS_EMPLOYEE
 * Row: {"employee_id":"EMP001","emp_name":"김직원","dept_name":"영업1팀",...}
 * → entity_source_records에 원본 그대로 저장
 * → entity_attributes에 가공된 최신 상태만 저장
 *
 * @since 2025-12-22
 */
@Entity
@Table(name = "entity_source_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntitySourceRecordEntity {

    /**
     * 레코드 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    /**
     * 엔티티 타입
     * 예: "EMPLOYEE", "CUSTOMER", "CORPORATE"
     */
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    /**
     * 엔티티 ID
     * 예: "EMP001", "CUST001", "CORP_030"
     */
    @Column(name = "entity_id", nullable = false, length = 255)
    private String entityId;

    /**
     * DataSource ID
     * 예: "DS_EMPLOYEE", "DS_CUSTOMER", "DS_CORP_ACC"
     */
    @Column(name = "data_source_id", nullable = false, length = 50)
    private String dataSourceId;

    /**
     * 원본 시스템의 트랜잭션 ID
     * 예: "HR20251216001", "CUST_TX_001"
     */
    @Column(name = "source_tx_id", length = 100)
    private String sourceTxId;

    /**
     * 원본 row 데이터 (JSONB)
     * DataSource에서 들어온 데이터를 그대로 저장
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_data", columnDefinition = "jsonb", nullable = false)
    private JsonNode sourceData;

    /**
     * 데이터 수신 시각
     */
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    /**
     * 원본 mapped_storages 참조 (선택적)
     */
    @Column(name = "mapped_storage_id")
    private Long mappedStorageId;

    /**
     * 배치 처리 ID (선택적)
     * 같은 배치로 들어온 데이터 그룹핑
     */
    @Column(name = "processing_batch_id", length = 100)
    private String processingBatchId;

    /**
     * 새 원본 레코드 생성 (정적 팩토리 메서드)
     *
     * @param entityType 엔티티 타입
     * @param entityId 엔티티 ID
     * @param dataSourceId DataSource ID
     * @param sourceTxId 원본 트랜잭션 ID
     * @param sourceData 원본 데이터
     * @param receivedAt 수신 시각
     * @return 생성된 EntitySourceRecordEntity
     */
    public static EntitySourceRecordEntity of(
            String entityType,
            String entityId,
            String dataSourceId,
            String sourceTxId,
            JsonNode sourceData,
            LocalDateTime receivedAt) {

        EntitySourceRecordEntity entity = new EntitySourceRecordEntity();
        entity.entityType = entityType;
        entity.entityId = entityId;
        entity.dataSourceId = dataSourceId;
        entity.sourceTxId = sourceTxId;
        entity.sourceData = sourceData;
        entity.receivedAt = receivedAt;

        return entity;
    }

    /**
     * mapped_storage_id 설정
     */
    public void setMappedStorageId(Long mappedStorageId) {
        this.mappedStorageId = mappedStorageId;
    }

    /**
     * processing_batch_id 설정
     */
    public void setProcessingBatchId(String processingBatchId) {
        this.processingBatchId = processingBatchId;
    }

    /**
     * PrePersist: 생성 시점 자동 설정
     */
    @PrePersist
    protected void onCreate() {
        if (this.receivedAt == null) {
            this.receivedAt = LocalDateTime.now();
        }
    }
}

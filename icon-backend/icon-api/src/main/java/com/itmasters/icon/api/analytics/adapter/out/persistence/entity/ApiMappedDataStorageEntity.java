package com.itmasters.icon.api.analytics.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * mapped_storages 조회용 엔티티.
 */
@Entity
@Table(name = "mapped_storages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiMappedDataStorageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapped_storage_id")
    private Long mappedStorageId;

    @Column(name = "row_index", nullable = false)
    private Integer rowIndex;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "row_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> rowData;

    @Column(name = "reg_dt")
    private LocalDateTime regDt;

    @Column(name = "landing_record_id", nullable = false)
    private Long landingRecordId;
}


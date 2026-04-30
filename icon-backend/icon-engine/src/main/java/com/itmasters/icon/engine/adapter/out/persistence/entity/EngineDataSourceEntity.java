package com.itmasters.icon.engine.adapter.out.persistence.entity;

import com.itmasters.icon.common.domain.type.DataSourceType;
import jakarta.persistence.*;
import lombok.*;

/**
 * 룰 엔진용 데이터소스 엔티티
 * icon-api의 data_sources 테이블과 동일한 구조
 */
@Entity
@Table(name = "data_sources")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class EngineDataSourceEntity {
    
    @Id
    @Column(name = "data_source_id")
    private String dataSourceId;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private DataSourceType sourceType;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    @Column(name = "transaction_id_field", length = 100)
    private String transactionIdField;

    // Canonical timestamp field for this data source (optional)
    // Some environments may not have this column; ignore mapping to avoid SELECT errors.
    @Transient
    private String timestampKey;
}

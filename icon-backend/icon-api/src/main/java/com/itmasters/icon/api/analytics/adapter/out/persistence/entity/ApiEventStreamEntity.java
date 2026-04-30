package com.itmasters.icon.api.analytics.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "event_stream")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiEventStreamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_stream_id")
    private Long eventStreamId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> eventData;

    @Column(name = "event_dt", nullable = false)
    private LocalDateTime eventDt;

    @Column(name = "mapped_storage_id")
    private Long mappedStorageId;

    @Column(name = "transaction_id")
    private String transactionId;
}


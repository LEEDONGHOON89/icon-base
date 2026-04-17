package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 이벤트 스트림 엔티티
 * Event Stream 설계원칙: 효율적인 타임라인 저장
 * - event_data JSONB 필드 기반 유연성 극대화 (event_type 불필요)
 * - timestamp 기반 중복 제거
 */
@Entity
@Table(name = "event_stream",
       indexes = {
           @Index(name = "idx_event_stream_event_dt", columnList = "event_dt"),
           @Index(name = "idx_event_stream_mapped_storage", columnList = "mapped_storage_id")
       })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"eventData", "mappedDataStorage"})
public class EngineEventStreamEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_stream_id")
    private Long eventStreamId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> eventData;  // Raw 데이터 그대로 보존 (모든 원본 필드 포함)
    
    @Column(name = "event_dt", nullable = false)
    private LocalDateTime eventDt;

    // 원본 데이터 추적을 위한 외래키
    @Column(name = "mapped_storage_id")
    private Long mappedDataStorageId;

    @Column(name = "transaction_id")
    private String transactionId;

    // 관계 매핑 (Lazy Loading)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mapped_storage_id", insertable = false, updatable = false)
    private MappedDataStorageEntity mappedDataStorage;
    

    /**
     * 정적 팩토리 메서드 - 원본 추적 정보 포함 이벤트 생성
     */
    public static EngineEventStreamEntity ofWithMapping(Map<String, Object> eventData
            , Long mappedDataStorageId
            , String transactionId
            , LocalDateTime createdAt) {
        return EngineEventStreamEntity.builder()
                .eventData(eventData)
                .mappedDataStorageId(mappedDataStorageId)
                .transactionId(transactionId)
                .eventDt(createdAt)
                .build();
    }
    
    
    /**
     * 원본 데이터 존재 여부 확인
     */
    public boolean hasOriginalData() {
        return this.mappedDataStorageId != null;
    }
}

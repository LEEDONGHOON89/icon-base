package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Event Stream Groups 엔티티
 *
 * 각 event가 어떤 aggregate의 어떤 group_key에 속하는지 매핑하는 테이블
 *
 * 설계 목적:
 * - 여러 aggregate가 다른 group_by_fields를 가질 수 있음
 * - 1개 event는 여러 aggregate의 여러 group_key에 동시에 속할 수 있음
 * - 집계 조회 시 group_key 기반 인덱스 활용으로 성능 최적화
 *
 * 예시:
 * event_stream_id=1001 인 event가:
 * - AGG_SPLIT_LIMIT의 "EMP004|ACC_001" 그룹
 * - AGG_CUS020의 "CUS_001|ACC_002" 그룹
 * - AGG_PROXY의 "EMP004" 그룹
 * 에 동시에 속할 수 있음
 */
@Entity
@Table(name = "event_stream_groups")
@IdClass(EventStreamGroupId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EventStreamGroupEntity {

    @Id
    @Column(name = "event_stream_id", nullable = false)
    private Long eventStreamId;

    @Id
    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String ruleId;

    @Column(name = "group_key", nullable = false, length = 500)
    private String groupKey;

    @Column(name = "mapped_storage_id")
    private Long mappedStorageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * 정적 팩토리 메서드
     */
    public static EventStreamGroupEntity of(Long eventStreamId, String ruleId, String groupKey, Long mappedStorageId) {
        return EventStreamGroupEntity.builder()
                .eventStreamId(eventStreamId)
                .ruleId(ruleId)
                .groupKey(groupKey)
                .mappedStorageId(mappedStorageId)
                .build();
    }
}

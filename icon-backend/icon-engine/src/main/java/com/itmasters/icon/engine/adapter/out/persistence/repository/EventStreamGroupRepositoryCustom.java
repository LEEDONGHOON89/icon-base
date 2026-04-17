package com.itmasters.icon.engine.adapter.out.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event Stream Groups 커스텀 Repository 인터페이스
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
public interface EventStreamGroupRepositoryCustom {

    /**
     * 특정 aggregate의 특정 group_key에 속하는 event_stream_id 목록 조회
     */
    List<Long> findEventStreamIdsByAggregateIdAndGroupKey(String ruleId, String groupKey);

    /**
     * 특정 aggregate의 특정 group_key에 속하는 event들 조회 (기간 필터 포함)
     *
     * Note: event_stream 테이블과 JOIN하므로 Native SQL 유지
     */
    List<Long> findEventStreamIdsByAggregateAndGroupKeyAndDateRange(
            String ruleId,
            String groupKey,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * 특정 event_stream_id에 연결된 모든 그룹 매핑 삭제
     */
    void deleteByEventStreamId(Long eventStreamId);

    /**
     * 특정 aggregate의 모든 그룹 키 조회 (DISTINCT)
     */
    List<String> findDistinctGroupKeysByAggregateId(String ruleId);

    /**
     * 특정 aggregate의 모든 그룹 키 조회 (기간 필터 포함)
     *
     * Note: event_stream 테이블과 JOIN하므로 Native SQL 유지
     */
    List<String> findDistinctGroupKeysByAggregateIdAndDateRange(
            String ruleId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * 특정 aggregate의 모든 그룹 키 조회 (exec_ds_mp_id 필터 포함)
     *
     * Note: event_stream, mapped_storages 테이블과 JOIN하므로 Native SQL 유지
     */
    List<String> findDistinctGroupKeysByExecDsMpIdAndAggregateId(Long execDsMpId, String ruleId);

    /**
     * 특정 aggregate의 모든 그룹 키 조회 (mapped_storage_id 필터 포함)
     * - mapped_storage_id를 직접 사용하여 exec_ds_mp_id 변환 없이 조회
     *
     * @param mappedStorageId mapped_storages 테이블의 ID
     * @param ruleId aggregate(rule) ID
     * @return 해당 mapped_storage에 속하는 distinct group_key 목록
     */
    List<String> findDistinctGroupKeysByMappedStorageIdAndAggregateId(Long mappedStorageId, String ruleId);
}

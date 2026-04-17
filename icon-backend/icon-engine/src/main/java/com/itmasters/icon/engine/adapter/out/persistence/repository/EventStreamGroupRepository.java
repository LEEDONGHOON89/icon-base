package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EventStreamGroupEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EventStreamGroupId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event Stream Groups Repository
 *
 * 각 event가 어떤 aggregate의 어떤 group_key에 속하는지 관리하는 리포지토리
 * QueryDSL 기반 복잡한 쿼리는 EventStreamGroupRepositoryCustom에서 구현
 */
@Repository
public interface EventStreamGroupRepository extends JpaRepository<EventStreamGroupEntity, EventStreamGroupId>, EventStreamGroupRepositoryCustom {

    /**
     * 특정 aggregate의 특정 group_key에 속하는 event_stream_id 목록 조회 - QueryDSL로 구현
     */
    // @Query 제거됨 - EventStreamGroupRepositoryCustom 인터페이스에서 선언, EventStreamGroupRepositoryImpl에서 구현
    // List<Long> findEventStreamIdsByAggregateIdAndGroupKey(String ruleId, String groupKey);

    /**
     * 특정 aggregate의 특정 group_key에 속하는 event들 조회 (기간 필터 포함) - Native SQL로 구현
     *
     * Note: event_stream 테이블과 JOIN하므로 Native SQL 유지
     */
    // @Query 제거됨 - EventStreamGroupRepositoryCustom 인터페이스에서 선언, EventStreamGroupRepositoryImpl에서 Native SQL로 구현
    // List<Long> findEventStreamIdsByAggregateAndGroupKeyAndDateRange(String ruleId, String groupKey, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 특정 event_stream_id에 연결된 모든 그룹 매핑 조회
     *
     * @param eventStreamId 이벤트 스트림 ID
     * @return 해당 이벤트의 모든 그룹 매핑
     */
    List<EventStreamGroupEntity> findByEventStreamId(Long eventStreamId);

    /**
     * 특정 event_stream_id에 연결된 모든 그룹 매핑 삭제 - QueryDSL로 구현
     */
    // @Query 제거됨 - EventStreamGroupRepositoryCustom 인터페이스에서 선언, EventStreamGroupRepositoryImpl에서 구현
    // void deleteByEventStreamId(Long eventStreamId);

    /**
     * 특정 aggregate의 모든 그룹 키 조회 (DISTINCT) - QueryDSL로 구현
     */
    // @Query 제거됨 - EventStreamGroupRepositoryCustom 인터페이스에서 선언, EventStreamGroupRepositoryImpl에서 구현
    // List<String> findDistinctGroupKeysByAggregateId(String ruleId);

    /**
     * 특정 aggregate의 모든 그룹 키 조회 (기간 필터 포함) - Native SQL로 구현
     *
     * Note: event_stream 테이블과 JOIN하므로 Native SQL 유지
     */
    // @Query 제거됨 - EventStreamGroupRepositoryCustom 인터페이스에서 선언, EventStreamGroupRepositoryImpl에서 Native SQL로 구현
    // List<String> findDistinctGroupKeysByAggregateIdAndDateRange(String ruleId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 특정 aggregate의 모든 그룹 키 조회 (exec_ds_mp_id 필터 포함) - Native SQL로 구현
     * SINGLE_ROW 모드에서 사용 - 특정 실행 컨텍스트의 이벤트만 조회
     *
     * Note: event_stream, mapped_storages 테이블과 JOIN하므로 Native SQL 유지
     */
    // @Query 제거됨 - EventStreamGroupRepositoryCustom 인터페이스에서 선언, EventStreamGroupRepositoryImpl에서 Native SQL로 구현
    // List<String> findDistinctGroupKeysByExecDsMpIdAndAggregateId(Long execDsMpId, String ruleId);
}

package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineEventStreamEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * EventStream Repository Custom 인터페이스
 */
public interface EventStreamRepositoryCustom {
    
    /**
     * 그룹 키별 최근 이벤트 조회
     */
    List<EngineEventStreamEntity> findByGroupKeyOrderByCreatedAtDesc(String groupKey);
    
    /**
     * 특정 기간 내 그룹 이벤트 조회
     */
    List<EngineEventStreamEntity> findByGroupKeyAndCreatedAtBetween(
        String groupKey, 
        LocalDateTime startDate, 
        LocalDateTime endDate
    );

    /**
     * 특정 기간 내 그룹 이벤트 조회 (start 포함, end 미포함)
     */
    List<EngineEventStreamEntity> findByGroupKeyAndEventDtInRange(
        String groupKey,
        LocalDateTime startInclusive,
        LocalDateTime endExclusive
    );
    
    
    /**
     * 원본 추적 가능한 데이터 조회
     */
    List<EngineEventStreamEntity> findByMappedDataStorageIdIsNotNull();
    
    /**
     * 대액 이체 거래 조회 (JSONB 쿼리)
     */
    List<EngineEventStreamEntity> findLargeTransfers(
        BigDecimal amount,
        LocalDateTime afterDate
    );
    
    /**
     * OTP 발급 후 이체 패턴 조회
     */
    List<EngineEventStreamEntity> findTransfersAfterOTP(
        String groupKey,
        LocalDateTime startDate
    );
    
    /**
     * 원본 데이터와 함께 조회 (Fetch Join)
     */
    Optional<EngineEventStreamEntity> findByIdWithOriginalData(Long id);
    
    /**
     * 특정 mapped_storage_id로 조회
     */
    Optional<EngineEventStreamEntity> findByMappedDataStorageId(Long mappedDataStorageId);
    
    /**
     * group_key별 최근 N개 이벤트 조회 (시간 제한)
     */
    List<EngineEventStreamEntity> findByGroupKeyAndCreatedAtAfterOrderByCreatedAtDesc(
        String groupKey,
        LocalDateTime cutoffTime,
        int limit
    );
    
    
    /**
     * group_key별 최근 이벤트 개수 조회 (시간 제한)
     */
    long countByGroupKeyAndCreatedAtAfter(String groupKey, LocalDateTime cutoffTime);
    
    
    /**
     * 중복 확인: 같은 group_key + timestamp 조합이 존재하는지 확인
     */
    boolean existsByGroupKeyAndCreatedAt(String groupKey, LocalDateTime timestamp);
    
    /**
     * 특정 group_key들의 고유 조합 목록 조회
     */
    List<String> findDistinctGroupKeys();

    /**
     * 특정 실행(exec_ds_mp_id)에서 적재된 event_stream 기준으로 distinct group_key 목록을 조회
     */
    java.util.List<String> findDistinctGroupKeysByExecDsMpId(Long execDsMpId);
    
    /**
     * 트랜잭션 ID로 조회
     */
    List<EngineEventStreamEntity> findByTransactionId(String transactionId);
    
    /**
     * 지정된 기간 내에서 지정된 필드들의 distinct 조합 목록을 조회
     * (exec_ds_mp_id 필터 제거 - 모든 실행의 데이터를 대상으로 조회)
     *
     * @param fields JSONB event_data에서 추출할 필드명 리스트
     * @param startDate 집계 시작 시간
     * @param endDate 집계 종료 시간
     * @return 각 조합을 Map으로 표현한 리스트 (key: 필드명, value: 필드값)
     */
    List<java.util.Map<String, String>> findDistinctFieldCombinations(
            List<String> fields,
            LocalDateTime startDate,
            LocalDateTime endDate);

    /**
     * groupKeyValues(필드값 조합)와 시간 범위로 이벤트 조회
     * - event_data JSONB 필드를 직접 쿼리하여 필터링
     * - group_key 컬럼은 사용하지 않음
     *
     * @param groupKeyValues 필드명-값 조합 (예: {login_id: "EMP010"} 또는 {login_id: "EMP010", account_number: "123"})
     * @param startDate 시작 시간 (inclusive)
     * @param endDate 종료 시간 (inclusive)
     * @return 조건에 맞는 이벤트 목록
     */
    List<EngineEventStreamEntity> findByFieldValuesAndEventDtBetween(
        java.util.Map<String, String> groupKeyValues,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    /**
     * event_stream에서 최신 이벤트 시간 조회
     * event_dt 컬럼 기준
     *
     * @return 최신 event_dt (없으면 empty)
     */
    Optional<LocalDateTime> findLatestEventTime();

    /**
     * 특정 실행(exec_ds_mp_id)의 event_stream에서 최신 이벤트 시간 조회
     * - mapped_storages와 JOIN하여 exec_ds_mp_id 필터링
     * - event_dt 컬럼 기준
     *
     * @param execDsMpId 실행 컨텍스트 ID
     * @return 해당 실행의 최신 event_dt (없으면 empty)
     */
    Optional<LocalDateTime> findLatestEventTimeByExecDsMpId(Long execDsMpId);

    /**
     * exec_ds_mp_id와 groupKeyValues로 이벤트 조회
     * - mapped_storages에서 exec_ds_mp_id로 mapped_storage_id 목록 조회
     * - event_stream에서 해당 mapped_storage_id + groupKeyValues 조건으로 필터링
     *
     * @param execDsMpId 실행 ID
     * @param groupKeyValues 필드명-값 조합
     * @return 조건에 맞는 이벤트 목록
     */
    List<EngineEventStreamEntity> findByExecDsMpIdAndGroupKeyValues(
        Long execDsMpId,
        java.util.Map<String, String> groupKeyValues
    );

    /**
     * exec_ds_mp_id로 이벤트 조회
     * - mapped_storages에서 exec_ds_mp_id로 mapped_storage_id 목록 조회
     * - event_stream에서 해당 mapped_storage_id로 필터링
     *
     * @param execDsMpId 실행 ID
     * @return 해당 실행의 이벤트 목록
     */
    List<EngineEventStreamEntity> findByExecDsMpId(Long execDsMpId);

    /**
     * mappedStorageId와 groupKeyValues로 이벤트 조회
     * - mapped_storage_id를 직접 사용하여 exec_ds_mp_id 변환 없이 조회
     * - event_data JSONB 필드를 직접 쿼리하여 필터링
     *
     * @param mappedStorageId mapped_storages 테이블의 ID
     * @param groupKeyValues 필드명-값 조합
     * @return 조건에 맞는 이벤트 목록
     */
    List<EngineEventStreamEntity> findByMappedStorageIdAndGroupKeyValues(
        Long mappedStorageId,
        java.util.Map<String, String> groupKeyValues
    );
}

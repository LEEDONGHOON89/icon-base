package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.MappedDataStorageEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DataSourceSchema 기반 매핑 데이터 저장소 Repository 인터페이스
 * row별로 저장된 데이터를 관리
 */
public interface MappedDataStorageRepository {
    
    /**
     * 단일 엔티티 저장
     */
    MappedDataStorageEntity save(MappedDataStorageEntity entity);
    
    /**
     * 배치로 여러 엔티티 저장
     */
    List<MappedDataStorageEntity> saveAll(List<MappedDataStorageEntity> entities);
    
    /**
     * ID로 조회
     */
    Optional<MappedDataStorageEntity> findById(Long id);

    /**
     * ID 기반 proxy 조회 (실제 조회 없이 참조만 획득)
     */
    MappedDataStorageEntity getReference(Long id);
    
    /**
     * 실행 ID로 모든 row 조회 (rowIndex 순서대로)
     */
    List<MappedDataStorageEntity> findByExecDsMpIdOrderByRowIndex(Long execDsMpId);
    
    /**
     * 실행 ID로 데이터 개수 조회
     */
    long countByExecDsMpId(Long execDsMpId);
    
    /**
     * 실행 ID로 데이터 삭제
     */
    void deleteByExecDsMpId(Long execDsMpId);
    
    /**
     * 전체 카운트
     */
    long count();
    
    /**
     * 조건에 맞는 모든 엔티티 조회
     */
    List<MappedDataStorageEntity> findAll();
    
    /**
     * 트랜잭션 ID로 조회
     */
    List<MappedDataStorageEntity> findByTransactionId(String transactionId);
    
    /**
     * 최근 트랜잭션 목록 조회 (transaction_id별 집계)
     * 
     * @param limit 조회할 트랜잭션 개수
     * @return Object[] - [transactionId, dataSourceId, firstSeenAt, lastSeenAt, totalRecords]
     */
    List<Object[]> findRecentTransactions(int limit);
}

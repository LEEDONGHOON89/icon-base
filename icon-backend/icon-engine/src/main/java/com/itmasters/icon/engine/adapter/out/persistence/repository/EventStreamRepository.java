package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineEventStreamEntity;

import java.util.List;
import java.util.Optional;

/**
 * 이벤트 스트림 Repository
 */
public interface EventStreamRepository extends EventStreamRepositoryCustom {
    
    /**
     * 엔티티 저장
     */
    EngineEventStreamEntity save(EngineEventStreamEntity entity);
    
    /**
     * ID로 조회
     */
    Optional<EngineEventStreamEntity> findById(Long id);
    
    /**
     * 모든 엔티티 저장
     */
    List<EngineEventStreamEntity> saveAll(List<EngineEventStreamEntity> entities);
    
    /**
     * ID로 삭제
     */
    void deleteById(Long id);
    
    /**
     * 엔티티 삭제
     */
    void delete(EngineEventStreamEntity entity);
    
    /**
     * 트랜잭션 ID로 조회
     */
    List<EngineEventStreamEntity> findByTransactionId(String transactionId);
}
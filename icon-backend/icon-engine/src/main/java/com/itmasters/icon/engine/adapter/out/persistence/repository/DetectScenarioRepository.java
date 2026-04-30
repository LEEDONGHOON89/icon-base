package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectScenarioEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 시나리오 탐지 결과 Repository
 */
public interface DetectScenarioRepository {

    DetectScenarioEntity save(DetectScenarioEntity entity);

    boolean existsRecentByKeyAndScenario(String groupKey,
                                         String scenarioId,
                                         LocalDateTime since);

    List<DetectScenarioEntity> findRecentByKey(String groupKey, int limit);


    /**
     * mappedStorageId로 detect_scenarios 조회
     * - exec_ds_mp_id 대신 mapped_storage_id 기준 조회
     *
     * @param mappedStorageId mapped_storages 테이블의 ID
     * @return 해당 mappedStorageId의 detect_scenarios 목록
     */
    List<DetectScenarioEntity> findByMappedStorageId(Long mappedStorageId);
    
    /**
     * 트랜잭션 ID로 조회
     *
     * @param transactionId 트랜잭션 ID
     * @return 시나리오 목록
     */
    List<DetectScenarioEntity> findByTransactionId(String transactionId);
    
    /**
     * 트랜잭션 ID로 개수 조회
     *
     * @param transactionId 트랜잭션 ID
     * @return 시나리오 개수
     */
    Integer countByTransactionId(String transactionId);
}

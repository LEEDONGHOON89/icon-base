package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectRuleEntity;

import java.util.List;
import java.util.Set;

/**
 * Query repository for DetectRuleEntity
 */
public interface DetectRuleRepository {
    DetectRuleEntity save(DetectRuleEntity entity);


    /**
     * mappedStorageId로 detect_rules 조회
     * - exec_ds_mp_id 대신 mapped_storage_id 기준 조회
     *
     * @param mappedStorageId mapped_storages 테이블의 ID
     * @return 해당 mappedStorageId의 detect_rules 목록
     */
    List<DetectRuleEntity> findByMappedStorageId(Long mappedStorageId);

    /**
     * 여러 groupKey들에 대해 활성 aggregate 조회
     *
     * @param groupKeys 조회할 group key 목록
     * @return pass=true인 모든 aggregate
     */
    List<DetectRuleEntity> findActiveAggregatesByGroupKeys(Set<String> groupKeys);

    /**
     * 트랜잭션 ID로 조회
     */
    List<DetectRuleEntity> findByTransactionId(String transactionId);
}


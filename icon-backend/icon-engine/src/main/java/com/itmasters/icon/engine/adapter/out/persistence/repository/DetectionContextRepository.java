package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectionContextEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 시나리오 탐지 컨텍스트 저장소 인터페이스
 */
public interface DetectionContextRepository {
    
    /**
     * 컨텍스트 저장
     */
    DetectionContextEntity save(DetectionContextEntity entity);
    
    /**
     * 컨텍스트 ID로 조회
     */
    Optional<DetectionContextEntity> findById(Long contextId);
    
    /**
     * 상관 관계 키로 활성 컨텍스트 조회
     */
    Optional<DetectionContextEntity> findActiveByCorrelationKey(String correlationKey);
    
    /**
     * 만료된 컨텍스트 조회
     */
    List<DetectionContextEntity> findExpiredContexts(LocalDateTime expiredBefore);
    
    /**
     * 컨텍스트 상태 업데이트
     */
    void updateStatus(Long contextId, String status);
    
    /**
     * 컨텍스트 삭제
     */
    void deleteById(Long contextId);
    
    /**
     * 만료된 컨텍스트 일괄 삭제
     */
    int deleteExpiredContexts(LocalDateTime expiredBefore);
}
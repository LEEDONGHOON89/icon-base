package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectionEventEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 시나리오 탐지 이벤트 저장소 인터페이스
 */
public interface DetectionEventRepository {
    
    /**
     * 이벤트 저장
     */
    DetectionEventEntity save(DetectionEventEntity entity);
    
    /**
     * 이벤트 ID로 조회
     */
    Optional<DetectionEventEntity> findById(Long eventId);
    
    /**
     * 컨텍스트별 이벤트 목록 조회
     */
    List<DetectionEventEntity> findByContextId(Long contextId);
    
    /**
     * 룰별 이벤트 목록 조회
     */
    List<DetectionEventEntity> findByRuleId(String ruleId);
    
    /**
     * 시간 범위별 이벤트 조회
     */
    List<DetectionEventEntity> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 컨텍스트별 이벤트 수 조회
     */
    long countByContextId(Long contextId);
    
    /**
     * 이벤트 타입별 조회
     */
    List<DetectionEventEntity> findByEventType(String eventType);
    
    /**
     * 배치 저장
     */
    List<DetectionEventEntity> saveAll(List<DetectionEventEntity> entities);
    
    /**
     * 컨텍스트에 속한 이벤트 모두 삭제
     */
    int deleteByContextId(Long contextId);
}
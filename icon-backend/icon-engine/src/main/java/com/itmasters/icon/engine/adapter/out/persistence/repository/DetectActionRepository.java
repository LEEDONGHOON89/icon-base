package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectActionEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectActionEntity.ActionStatus;
import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectActionEntity.ActionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 탐지 조치(Action) Repository 인터페이스
 */
public interface DetectActionRepository {

    /**
     * 조치 저장
     */
    DetectActionEntity save(DetectActionEntity action);

    /**
     * ID로 조회
     */
    Optional<DetectActionEntity> findById(Long detectActionId);

    /**
     * 특정 탐지 시나리오의 모든 조치 조회
     */
    List<DetectActionEntity> findByDetectScenarioId(Long detectScenarioId);

    /**
     * 그룹키로 조회 (사용자/계좌별 조치 이력)
     */
    List<DetectActionEntity> findByGroupKey(String groupKey);

    /**
     * 시나리오 ID + 그룹키로 조회
     */
    List<DetectActionEntity> findByScenarioIdAndGroupKey(String scenarioId, String groupKey);

    /**
     * 상태별 조회
     */
    List<DetectActionEntity> findByActionStatus(ActionStatus status, int limit);

    /**
     * 조치 유형별 조회
     */
    List<DetectActionEntity> findByActionType(ActionType actionType, int limit);

    /**
     * 대기 중인 조치 목록 (PENDING)
     */
    List<DetectActionEntity> findPendingActions(int limit);

    /**
     * 요청자별 조회
     */
    List<DetectActionEntity> findByRequestedBy(String requestedBy, int limit);

    /**
     * 승인자별 조회
     */
    List<DetectActionEntity> findByApprovedBy(String approvedBy, int limit);

    /**
     * 기간별 조회
     */
    List<DetectActionEntity> findByRequestedAtBetween(
            LocalDateTime startInclusive,
            LocalDateTime endInclusive,
            int limit
    );

    /**
     * 상태별 카운트
     */
    Map<ActionStatus, Long> countByActionStatus();

    /**
     * 조치 유형별 카운트
     */
    Map<ActionType, Long> countByActionType();

    /**
     * 특정 시나리오의 조치 카운트
     */
    long countByDetectScenarioId(Long detectScenarioId);

    /**
     * 특정 그룹키의 조치 카운트
     */
    long countByGroupKey(String groupKey);

    /**
     * 대기 중인 조치 개수
     */
    long countPendingActions();

    /**
     * 조치 삭제 (실제로는 거의 사용하지 않음)
     */
    void delete(DetectActionEntity action);

    /**
     * ID로 삭제
     */
    void deleteById(Long detectActionId);
}

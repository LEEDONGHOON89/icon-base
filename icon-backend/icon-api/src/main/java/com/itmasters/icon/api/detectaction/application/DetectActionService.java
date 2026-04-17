package com.itmasters.icon.api.detectaction.application;

import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiDetectActionEntity;
import com.itmasters.icon.api.detectaction.adapter.out.persistence.DetectActionJpaRepository;
import com.itmasters.icon.api.detectaction.adapter.out.persistence.DetectActionQueryRepository;
import com.itmasters.icon.api.detectaction.dto.DetectActionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 탐지 조치 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DetectActionService {

    private final DetectActionQueryRepository detectActionQueryRepository;
    private final DetectActionJpaRepository detectActionJpaRepository;

    /**
     * 탐지 조치 목록 조회
     */
    public Page<DetectActionDto> getDetectActions(DetectActionDto.SearchRequest request) {
        log.info("Fetching detect actions with request: {}", request);

        // 날짜 범위 설정 (시작일 00:00:00 ~ 종료일 23:59:59)
        LocalDateTime startDate = request.getStartDate() != null
                ? request.getStartDate().atStartOfDay()
                : LocalDate.now().atStartOfDay();

        LocalDateTime endDate = request.getEndDate() != null
                ? request.getEndDate().atTime(23, 59, 59)
                : LocalDate.now().atTime(23, 59, 59);

        // 페이징 설정
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        Pageable pageable = PageRequest.of(page, size);

        return detectActionQueryRepository.findDetectActions(
                startDate,
                endDate,
                request.getRiskLevel(),
                request.getActionStatus(),
                pageable
        );
    }

    /**
     * 탐지 조치 업데이트
     */
    @Transactional
    public DetectActionDto updateDetectAction(Long id, DetectActionDto.UpdateRequest request, String currentUserId) {
        log.info("Updating detect action {} by user {}", id, currentUserId);

        // 엔티티 조회
        ApiDetectActionEntity entity = detectActionJpaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("조치 정보를 찾을 수 없습니다. ID: " + id));

        // 업데이트 가능한 필드 설정
        if (request.getActionMemo() != null) {
            entity.setActionMemo(request.getActionMemo());
        }
        if (request.getActionReason() != null) {
            entity.setActionReason(request.getActionReason());
        }

        // 상태 변경 시 자동 설정
        if (request.getActionStatus() != null && !request.getActionStatus().equals(entity.getActionStatus())) {
            String newStatus = request.getActionStatus();
            LocalDateTime now = LocalDateTime.now();

            // 상태에 따라 자동 설정
            if ("APPROVED".equals(newStatus)) {
                entity.setApprovedBy(currentUserId);
                entity.setApprovedAt(now);
            } else if ("COMPLETED".equals(newStatus)) {
                entity.setCompletedBy(currentUserId);
                entity.setCompletedAt(now);
            }

            entity.setActionStatus(newStatus);
        }

        // 저장
        ApiDetectActionEntity saved = detectActionJpaRepository.save(entity);

        // DTO 변환
        return DetectActionDto.builder()
                .detectActionId(saved.getDetectActionId())
                .detectScenarioId(saved.getDetectScenarioId())
                .scenarioId(saved.getScenarioId())
                .scenarioName(null) // 조인 없이 조회하므로 null
                .groupKey(saved.getGroupKey())
                .actionType(saved.getActionType())
                .actionStatus(saved.getActionStatus())
                .riskLevel(saved.getRiskLevel())
                .actionMemo(saved.getActionMemo())
                .actionReason(saved.getActionReason())
                .requestedBy(saved.getRequestedBy())
                .requestedAt(saved.getRequestedAt())
                .approvedBy(saved.getApprovedBy())
                .approvedAt(saved.getApprovedAt())
                .completedBy(saved.getCompletedBy())
                .completedAt(saved.getCompletedAt())
                .regDt(saved.getRegDt())
                .build();
    }
}

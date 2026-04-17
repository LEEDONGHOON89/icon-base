package com.itmasters.icon.api.audit.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.api.audit.adapter.out.persistence.DetectionConfigAuditEntity;
import com.itmasters.icon.api.audit.adapter.out.persistence.DetectionConfigAuditQueryRepository;
import com.itmasters.icon.api.audit.adapter.out.persistence.DetectionConfigAuditRepository;
import com.itmasters.icon.api.audit.domain.ConfigAuditAction;
import com.itmasters.icon.api.audit.domain.ConfigAuditTargetType;
import com.itmasters.icon.api.audit.dto.AuditSearchRequest;
import com.itmasters.icon.api.audit.dto.DetectionConfigAuditDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionConfigAuditService {

    private final DetectionConfigAuditRepository auditRepository;
    private final DetectionConfigAuditQueryRepository auditQueryRepository;
    private final ObjectMapper objectMapper;

    /**
     * 생성 감사 로그 기록
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCreate(ConfigAuditTargetType targetType, String targetId, String targetName,
                          Object afterData, String changedBy, String ipAddress) {
        try {
            String afterSnapshot = toJson(afterData);
            DetectionConfigAuditEntity audit = DetectionConfigAuditEntity.of(
                    targetType, targetId, targetName,
                    ConfigAuditAction.CREATE,
                    null, null, afterSnapshot,
                    changedBy, ipAddress
            );
            auditRepository.save(audit);
            log.info("Audit log created: {} {} {} by {}", ConfigAuditAction.CREATE, targetType, targetId, changedBy);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage(), e);
        }
    }

    /**
     * 수정 감사 로그 기록
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUpdate(ConfigAuditTargetType targetType, String targetId, String targetName,
                          Object beforeData, Object afterData, List<String> changedFields,
                          String changedBy, String ipAddress) {
        try {
            String beforeSnapshot = toJson(beforeData);
            String afterSnapshot = toJson(afterData);
            String changedFieldsJson = toJson(changedFields);

            DetectionConfigAuditEntity audit = DetectionConfigAuditEntity.of(
                    targetType, targetId, targetName,
                    ConfigAuditAction.UPDATE,
                    changedFieldsJson, beforeSnapshot, afterSnapshot,
                    changedBy, ipAddress
            );
            auditRepository.save(audit);
            log.info("Audit log created: {} {} {} by {} - fields: {}",
                    ConfigAuditAction.UPDATE, targetType, targetId, changedBy, changedFields);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage(), e);
        }
    }

    /**
     * 삭제 감사 로그 기록
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDelete(ConfigAuditTargetType targetType, String targetId, String targetName,
                          Object beforeData, String changedBy, String ipAddress) {
        try {
            String beforeSnapshot = toJson(beforeData);
            DetectionConfigAuditEntity audit = DetectionConfigAuditEntity.of(
                    targetType, targetId, targetName,
                    ConfigAuditAction.DELETE,
                    null, beforeSnapshot, null,
                    changedBy, ipAddress
            );
            auditRepository.save(audit);
            log.info("Audit log created: {} {} {} by {}", ConfigAuditAction.DELETE, targetType, targetId, changedBy);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage(), e);
        }
    }

    /**
     * 특정 대상의 변경 이력 조회
     */
    @Transactional(readOnly = true)
    public List<DetectionConfigAuditDto> getAuditHistory(ConfigAuditTargetType targetType, String targetId) {
        return auditRepository.findByTargetTypeAndTargetIdOrderByChangedAtDesc(targetType, targetId)
                .stream()
                .map(DetectionConfigAuditDto::from)
                .toList();
    }

    /**
     * 특정 대상의 변경 이력 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<DetectionConfigAuditDto.Summary> getAuditHistory(
            ConfigAuditTargetType targetType, String targetId, Pageable pageable) {
        return auditRepository.findByTargetTypeAndTargetIdOrderByChangedAtDesc(targetType, targetId, pageable)
                .map(DetectionConfigAuditDto.Summary::from);
    }

    /**
     * 특정 사용자의 변경 이력 조회
     */
    @Transactional(readOnly = true)
    public Page<DetectionConfigAuditDto.Summary> getAuditHistoryByUser(String userId, Pageable pageable) {
        return auditRepository.findByChangedByOrderByChangedAtDesc(userId, pageable)
                .map(DetectionConfigAuditDto.Summary::from);
    }

    /**
     * 기간별 변경 이력 조회
     */
    @Transactional(readOnly = true)
    public Page<DetectionConfigAuditDto.Summary> getAuditHistoryByDateRange(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditRepository.findByDateRange(startDate, endDate, pageable)
                .map(DetectionConfigAuditDto.Summary::from);
    }

    /**
     * 타입별 + 기간별 변경 이력 조회
     */
    @Transactional(readOnly = true)
    public Page<DetectionConfigAuditDto.Summary> getAuditHistoryByTypeAndDateRange(
            ConfigAuditTargetType targetType, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditRepository.findByTargetTypeAndDateRange(targetType, startDate, endDate, pageable)
                .map(DetectionConfigAuditDto.Summary::from);
    }

    /**
     * 최근 변경 이력 조회
     */
    @Transactional(readOnly = true)
    public List<DetectionConfigAuditDto.Summary> getRecentAuditHistory() {
        return auditRepository.findTop50ByOrderByChangedAtDesc()
                .stream()
                .map(DetectionConfigAuditDto.Summary::from)
                .toList();
    }

    /**
     * 통합 검색 (모든 필터 선택적 적용)
     */
    @Transactional(readOnly = true)
    public Page<DetectionConfigAuditDto.Summary> searchAuditHistory(AuditSearchRequest request, Pageable pageable) {
        return auditQueryRepository.searchAuditHistory(request, pageable)
                .map(DetectionConfigAuditDto.Summary::from);
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return null;
        }
    }
}

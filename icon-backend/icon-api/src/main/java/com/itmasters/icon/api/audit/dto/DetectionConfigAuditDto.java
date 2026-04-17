package com.itmasters.icon.api.audit.dto;

import com.itmasters.icon.api.audit.adapter.out.persistence.DetectionConfigAuditEntity;
import com.itmasters.icon.api.audit.domain.ConfigAuditAction;
import com.itmasters.icon.api.audit.domain.ConfigAuditTargetType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DetectionConfigAuditDto {

    private Long auditId;
    private ConfigAuditTargetType targetType;
    private String targetId;
    private String targetName;
    private ConfigAuditAction action;
    private String changedFields;
    private String beforeSnapshot;
    private String afterSnapshot;
    private String changeReason;
    private String changedBy;
    private LocalDateTime changedAt;
    private String ipAddress;

    public static DetectionConfigAuditDto from(DetectionConfigAuditEntity entity) {
        DetectionConfigAuditDto dto = new DetectionConfigAuditDto();
        dto.auditId = entity.getAuditId();
        dto.targetType = entity.getTargetType();
        dto.targetId = entity.getTargetId();
        dto.targetName = entity.getTargetName();
        dto.action = entity.getAction();
        dto.changedFields = entity.getChangedFields();
        dto.beforeSnapshot = entity.getBeforeSnapshot();
        dto.afterSnapshot = entity.getAfterSnapshot();
        dto.changeReason = entity.getChangeReason();
        dto.changedBy = entity.getChangedBy();
        dto.changedAt = entity.getChangedAt();
        dto.ipAddress = entity.getIpAddress();
        return dto;
    }

    /**
     * 목록 조회용 간단한 DTO
     */
    @Getter
    public static class Summary {
        private Long auditId;
        private ConfigAuditTargetType targetType;
        private String targetId;
        private String targetName;
        private ConfigAuditAction action;
        private String changedBy;
        private LocalDateTime changedAt;

        public static Summary from(DetectionConfigAuditEntity entity) {
            Summary dto = new Summary();
            dto.auditId = entity.getAuditId();
            dto.targetType = entity.getTargetType();
            dto.targetId = entity.getTargetId();
            dto.targetName = entity.getTargetName();
            dto.action = entity.getAction();
            dto.changedBy = entity.getChangedBy();
            dto.changedAt = entity.getChangedAt();
            return dto;
        }
    }
}

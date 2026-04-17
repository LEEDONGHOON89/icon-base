package com.itmasters.icon.api.audit.dto;

import com.itmasters.icon.api.audit.domain.ConfigAuditAction;
import com.itmasters.icon.api.audit.domain.ConfigAuditTargetType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 감사 로그 통합 검색 요청 DTO
 */
@Getter
@Setter
public class AuditSearchRequest {

    private ConfigAuditTargetType targetType;

    private ConfigAuditAction action;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    private String search;
}

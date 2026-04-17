package com.itmasters.icon.engine.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이벤트 조회를 위한 쿼리 파라미터 DTO
 */
@Getter
@Builder
public class EventQuery {
    private String groupKey;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String ruleId; // 특정 룰 ID로 필터링 (단일)
    private List<String> ruleIds; // 여러 룰 ID로 필터링 (목록)
    // 필요에 따라 EventStreamRepositoryCustom의 다른 조회 메소드에 필요한 필드 추가 가능
}

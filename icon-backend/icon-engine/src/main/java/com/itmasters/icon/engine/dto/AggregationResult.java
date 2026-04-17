package com.itmasters.icon.engine.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AggregationResult {
    // pass=false일 때는 “집계 조건이 만족되지 않았다”는 뜻이라 탐지가 발생하지 않음
    boolean pass;
    BigDecimal matchedValue;
    LocalDateTime detectedAt;
    // Anchor event identifier for traceability (event_stream.mapped_storage_id)
    Long anchorMappedStorageId;
    // 비즈니스 트랜잭션 ID (event_stream.transaction_id)
    String transactionId;
    // group_by_fields 기준의 실제 그룹키 값 (예: beneficiary_owner_id의 실제 값 "EMP030")
    // null이면 기존 groupKey(파라미터) 사용
    String actualGroupKey;
}

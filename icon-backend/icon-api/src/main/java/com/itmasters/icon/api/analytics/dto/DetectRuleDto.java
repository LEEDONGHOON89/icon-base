package com.itmasters.icon.api.analytics.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectRuleDto {
    private String groupKey;
    private String ruleId;
    private String ruleName;
    private String operator;
    private Integer windowMinutes;
    private BigDecimal matchedCount;
    private BigDecimal thresholdCount;
    private LocalDateTime detectedAt;
    private Long mappedStorageId;
}

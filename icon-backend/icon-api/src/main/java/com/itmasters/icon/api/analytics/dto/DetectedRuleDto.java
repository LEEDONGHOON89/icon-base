package com.itmasters.icon.api.analytics.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectedRuleDto {
    private Long detectRuleId;
    private String ruleId;
    private String ruleName;
    private String groupKey;
    private Long mappedStorageId;
    private Long rowNumber;
    private LocalDateTime detectedAt;
    private Map<String, Object> matchedFields;
}

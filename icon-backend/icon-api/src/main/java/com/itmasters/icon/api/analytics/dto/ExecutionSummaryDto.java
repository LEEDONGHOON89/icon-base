package com.itmasters.icon.api.analytics.dto;

import com.itmasters.icon.common.domain.type.DataSourceType;
import com.itmasters.icon.common.domain.type.ExecutionMode;
import com.itmasters.icon.common.domain.type.ExecutionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionSummaryDto {
    private Long landingRecordId;
    private Long mappedStorageId;
    private Long execDsMpId;
    private String dataSourceId;
    private DataSourceType sourceType;
    private ExecutionMode executionMode;
    private ExecutionStatus status;
    private LocalDateTime startDt;
    private LocalDateTime completeAt;
    private Integer totalRows;
    private String executedBy;
    private Integer rowIndex;
    private String batchKey;
    private LocalDateTime extractedAt;
    private String ingestionStatus;
    private String ingestionMessage;
    private long ruleCount;
    private long aggregateCount;
    private long scenarioCount;
}

package com.itmasters.icon.api.analytics.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionPageDto {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private List<ExecutionSummaryDto> items;
}

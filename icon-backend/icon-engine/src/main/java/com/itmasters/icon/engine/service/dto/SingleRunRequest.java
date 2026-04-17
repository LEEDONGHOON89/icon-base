package com.itmasters.icon.engine.service.dto;

import lombok.Data;

import java.util.Map;

@Data
public class SingleRunRequest {
    private String dataSourceId;
    private Map<String, Object> row;
    private String executedBy;
}

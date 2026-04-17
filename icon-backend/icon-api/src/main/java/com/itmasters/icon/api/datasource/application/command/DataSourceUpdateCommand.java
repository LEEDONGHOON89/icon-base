package com.itmasters.icon.api.datasource.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 데이터 소스 수정 커맨드
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceUpdateCommand {
    
    private String dataSourceId;
    private String name;
    private String description;
    private Boolean isActive;
}
package com.itmasters.icon.api.datasource.application.command;

import com.itmasters.icon.common.domain.type.DataSourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 데이터 소스 생성 커맨드
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceCreateCommand {
    
    private String name;
    private String description;
    private DataSourceType sourceType;
}
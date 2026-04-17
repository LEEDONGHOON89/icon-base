package com.itmasters.icon.api.datasource.application.result;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DataSourceEntity;
import com.itmasters.icon.common.domain.type.DataSourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 데이터 소스 조회 결과
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceResult {
    
    private String id;
    private String name;
    private String description;
    private DataSourceType sourceType;
    private boolean isActive;
    private Long mappedRuleCount;
    
    public static DataSourceResult from(DataSourceEntity dataSourceEntity) {
        return DataSourceResult.builder()
                .id(dataSourceEntity.getDataSourceId())
                .name(dataSourceEntity.getName())
                .description(dataSourceEntity.getDescription())
                .sourceType(dataSourceEntity.getSourceType())
                .isActive(dataSourceEntity.isActive())
                .build();
    }
    
    public static DataSourceResult fromWithRuleCount(DataSourceEntity dataSourceEntity, Long ruleCount) {
        DataSourceResult result = from(dataSourceEntity);
        result.mappedRuleCount = ruleCount;
        return result;
    }
}
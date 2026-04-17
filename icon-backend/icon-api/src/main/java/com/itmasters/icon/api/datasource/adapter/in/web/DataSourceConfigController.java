package com.itmasters.icon.api.datasource.adapter.in.web;

import com.itmasters.icon.api.datasource.application.service.DataSourceConfigService;
import com.itmasters.icon.api.datasource.dto.DataSourceConfigDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/data-sources/{dataSourceId}/config")
@Tag(name = "데이터 소스 설정 API", description = "데이터 소스 타입별 연결 설정 조회/저장 API")
public class DataSourceConfigController {

    private final DataSourceConfigService dataSourceConfigService;

    @GetMapping
    @Operation(summary = "데이터 소스 설정 조회", description = "데이터 소스 타입에 맞는 설정을 반환합니다")
    public DataSourceConfigDto getConfig(@PathVariable String dataSourceId) {
        return dataSourceConfigService.getConfig(dataSourceId);
    }

    @PutMapping
    @Operation(summary = "데이터 소스 설정 저장", description = "데이터 소스 타입에 맞는 설정을 저장/업데이트합니다")
    public DataSourceConfigDto saveConfig(@PathVariable String dataSourceId,
                                          @RequestBody DataSourceConfigDto request) {
        return dataSourceConfigService.saveConfig(dataSourceId, request);
    }
}


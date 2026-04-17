package com.itmasters.icon.api.datasource.adapter.in.web;

import com.itmasters.icon.api.common.response.ResponseList;
import com.itmasters.icon.api.datasource.dto.DataSourceDto;
import com.itmasters.icon.api.datasource.application.service.DataSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 데이터 소스 관리 API
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/data-sources")
@Tag(name = "데이터 소스 관리 API", description = "데이터 소스 목록/단건 조회, 등록, 수정, 삭제 API")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    /**
     * 데이터 소스 생성
     */
    @PostMapping
    @Operation(summary = "데이터 소스 생성", description = "새로운 데이터 소스를 생성합니다.")
    @ResponseStatus(HttpStatus.CREATED)
    public DataSourceDto.Response createDataSource(
            @Valid @RequestBody DataSourceDto.CreateRequest request) {

        log.info("Creating data source: {}", request.getName());

        DataSourceDto.CreateCommand command = request.toCommand();

        DataSourceDto.Info result = dataSourceService.createDataSource(command);

        return DataSourceDto.Response.from(result);
    }

    /**
     * 데이터 소스 수정
     */
    @PutMapping("/{dataSourceId}")
    @Operation(summary = "데이터 소스 수정", description = "기존 데이터 소스 정보를 수정합니다.")
    public DataSourceDto.Response updateDataSource(
            @Parameter(description = "데이터 소스 ID") @PathVariable String dataSourceId,
            @Valid @RequestBody DataSourceDto.UpdateRequest request) {
        
        DataSourceDto.UpdateCommand command = request.toCommand(dataSourceId);
                
        DataSourceDto.Info result = dataSourceService.updateDataSource(command);
        return DataSourceDto.Response.from(result);
    }

    /**
     * 데이터 소스 단건 조회
     */
    @GetMapping("/{dataSourceId}")
    @Operation(summary = "데이터 소스 단건 조회", description = "특정 데이터 소스의 상세 정보를 조회합니다.")
    public DataSourceDto.Response getDataSource(
            @Parameter(description = "데이터 소스 ID") @PathVariable String dataSourceId) {

        DataSourceDto.Info result = dataSourceService.getDataSource(dataSourceId);

        return DataSourceDto.Response.from(result);
    }

    /**
     * 전체 데이터 소스 목록 조회
     */
    @GetMapping
    @Operation(summary = "전체 데이터 소스 목록 조회", description = "모든 데이터 소스를 조회합니다.")
    public ResponseList<DataSourceDto.Response> getAllDataSources() {

        List<DataSourceDto.Info> results = dataSourceService.getAllDataSources();

        List<DataSourceDto.Response> responses = results.stream()
                .map(DataSourceDto.Response::from)
                .collect(Collectors.toList());
        
        return new ResponseList<>(responses);
    }

    /**
     * 데이터 소스 삭제
     */
    @DeleteMapping("/{dataSourceId}")
    @Operation(summary = "데이터 소스 삭제", description = "데이터 소스를 삭제합니다. 매핑된 규칙이 없어야 삭제 가능합니다.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDataSource(
            @Parameter(description = "데이터 소스 ID") @PathVariable String dataSourceId) {

        log.info("Deleting data source: {}", dataSourceId);

        dataSourceService.deleteDataSource(dataSourceId);
    }

    /**
     * 데이터 소스 활성화
     */
    @PutMapping("/{dataSourceId}/activate")
    @Operation(summary = "데이터 소스 활성화", description = "비활성화된 데이터 소스를 활성화합니다.")
    public DataSourceDto.Response activateDataSource(
            @Parameter(description = "데이터 소스 ID") @PathVariable String dataSourceId) {

        log.info("Activating data source: {}", dataSourceId);

        DataSourceDto.Info result = dataSourceService.activateDataSource(dataSourceId);

        return DataSourceDto.Response.from(result);
    }

    /**
     * 데이터 소스 비활성화
     */
    @PutMapping("/{dataSourceId}/deactivate")
    @Operation(summary = "데이터 소스 비활성화", description = "활성화된 데이터 소스를 비활성화합니다. 기본 데이터 소스는 비활성화할 수 없습니다.")
    public DataSourceDto.Response deactivateDataSource(
            @Parameter(description = "데이터 소스 ID") @PathVariable String dataSourceId) {

        log.info("Deactivating data source: {}", dataSourceId);

        DataSourceDto.Info result = dataSourceService.deactivateDataSource(dataSourceId);

        return DataSourceDto.Response.from(result);
    }

    // 기본 데이터 소스 설정 API 제거
}
package com.itmasters.icon.api.datasourceschema.adapter.in.web;

import com.itmasters.icon.api.common.response.ResponseList;
import com.itmasters.icon.api.datasourceschema.application.service.DataSourceOriginalSchemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 데이터소스 원본 스키마 관리 API
 */
@Slf4j
@Tag(name = "DataSourceOriginalSchema", description = "데이터소스 원본 스키마 관리 API")
@RestController
@RequiredArgsConstructor
public class DataSourceOriginalSchemaController {

    private final DataSourceOriginalSchemaService originalSchemaService;

    @GetMapping("/api/v1/data-sources/{dataSourceId}/original-schemas")
    @Operation(summary = "원본 스키마 목록 조회", description = "데이터소스의 원본 스키마 목록을 조회합니다")
    public ResponseList<DataSourceOriginalSchemaDto.Response> getOriginalSchemas(@PathVariable String dataSourceId) {

        List<DataSourceOriginalSchemaDto.Response> result = originalSchemaService.getAllOriginalSchemas(dataSourceId);
        return new ResponseList<>(result);
    }

    @PutMapping("/api/v1/data-sources/{dataSourceId}/original-schemas/bulk")
    @Operation(summary = "원본 스키마 일괄 수정", description = "여러 원본 스키마를 한 번에 수정합니다")
    public ResponseList<DataSourceOriginalSchemaDto.Response> updateOriginalSchemas(
            @Parameter(description = "데이터소스 ID", required = true)
            @PathVariable String dataSourceId,
            @Valid @RequestBody DataSourceOriginalSchemaDto.BulkUpdateRequest request) {

        log.info("원본 스키마 일괄 수정 요청: dataSourceId={}, 스키마 수={}", dataSourceId, request.getSchemas().size());

        List<DataSourceOriginalSchemaDto.Response> result = originalSchemaService.updateOriginalSchemasBulk(
                dataSourceId,
                request.getSchemas()
        );
        return new ResponseList<>(result);
    }

    @PutMapping("/api/v1/original-schemas/{schemaId}/mapping")
    @Operation(summary = "원본 스키마 표준 필드 매핑 업데이트", description = "개별 원본 스키마의 표준 필드 매핑을 업데이트합니다")
    public DataSourceOriginalSchemaDto.Response updateStandardFieldMapping(
            @Parameter(description = "스키마 ID", required = true)
            @PathVariable String schemaId,
            @Valid @RequestBody DataSourceOriginalSchemaDto.StandardFieldMappingRequest request) {

        log.info("스키마 표준 필드 매핑 업데이트: schemaId={}, standardFieldId={}", schemaId, request.getStandardFieldId());

        return originalSchemaService.updateStandardFieldMapping(schemaId, request);
    }
}
package com.itmasters.icon.api.parser.adapter.in.web;

import com.itmasters.icon.api.parser.application.service.DataSourceParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// [2026-04-20] 데이터소스-파서 연결 REST 컨트롤러
@RestController
@RequestMapping("/api/v1/data-sources/{dataSourceId}/parsers")
@RequiredArgsConstructor
@Tag(name = "DataSourceParser", description = "데이터소스-파서 연결 관리 API")
public class DataSourceParserController {

    private final DataSourceParserService dataSourceParserService;

    @GetMapping
    @Operation(summary = "데이터소스에 연결된 파서 목록 조회")
    public List<ParserDto.LinkResponse> getLinkedParsers(
            @PathVariable String dataSourceId) {
        return dataSourceParserService.getLinkedParsers(dataSourceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "데이터소스에 파서 연결")
    public ParserDto.LinkResponse linkParser(
            @PathVariable String dataSourceId,
            @Valid @RequestBody ParserDto.LinkRequest request) {
        return dataSourceParserService.linkParser(dataSourceId, request);
    }

    @DeleteMapping("/{parserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "데이터소스에서 파서 연결 해제")
    public void unlinkParser(
            @PathVariable String dataSourceId,
            @PathVariable String parserId) {
        dataSourceParserService.unlinkParser(dataSourceId, parserId);
    }
}

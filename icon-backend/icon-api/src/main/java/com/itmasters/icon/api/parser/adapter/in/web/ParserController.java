package com.itmasters.icon.api.parser.adapter.in.web;

import com.itmasters.icon.api.parser.application.service.ParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// [2026-04-20] 파서 CRUD REST 컨트롤러
@RestController
@RequestMapping("/api/v1/parsers")
@RequiredArgsConstructor
@Tag(name = "Parser", description = "파서 관리 API")
public class ParserController {

    private final ParserService parserService;

    @GetMapping
    @Operation(summary = "파서 전체 목록 조회")
    public List<ParserDto.SummaryResponse> getAllParsers() {
        return parserService.getAllParsers();
    }

    @GetMapping("/active")
    @Operation(summary = "활성 파서 목록 조회")
    public List<ParserDto.SummaryResponse> getActiveParsers() {
        return parserService.getActiveParsers();
    }

    @GetMapping("/{parserId}")
    @Operation(summary = "파서 상세 조회")
    public ParserDto.Response getParser(@PathVariable String parserId) {
        return parserService.getParser(parserId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "파서 생성")
    public ParserDto.Response createParser(@Valid @RequestBody ParserDto.CreateRequest request) {
        return parserService.createParser(request);
    }

    @PutMapping("/{parserId}")
    @Operation(summary = "파서 수정")
    public ParserDto.Response updateParser(
            @PathVariable String parserId,
            @Valid @RequestBody ParserDto.UpdateRequest request) {
        return parserService.updateParser(parserId, request);
    }

    @DeleteMapping("/{parserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "파서 삭제")
    public void deleteParser(@PathVariable String parserId) {
        parserService.deleteParser(parserId);
    }
}

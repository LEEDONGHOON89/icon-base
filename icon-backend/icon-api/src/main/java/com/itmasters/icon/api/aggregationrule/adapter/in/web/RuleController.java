package com.itmasters.icon.api.aggregationrule.adapter.in.web;

import com.itmasters.icon.api.aggregationrule.application.service.RuleService;
import com.itmasters.icon.api.aggregationrule.dto.RuleDto;
import com.itmasters.icon.api.common.response.ResponseList;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 룰 관리 API
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Rule 관리 API", description = "룰 목록/단건 조회, 등록, 수정, 삭제 API")
@Validated
public class RuleController {

    private final RuleService ruleService;

    @GetMapping("/api/v1/rules")
    @Operation(summary = "룰 목록 조회")
    public ResponseList<RuleDto.Response> getRules() {
        List<RuleDto.Response> rules = ruleService.findAll();
        return new ResponseList<>(rules);
    }

    @GetMapping("/api/v1/rules/{id}")
    @Operation(summary = "단일 룰 조회")
    public RuleDto.Response getRule(@PathVariable String id) {
        return ruleService.findById(id);
    }

    @GetMapping("/api/v1/rules/active")
    @Operation(summary = "활성화된 룰 조회")
    public ResponseList<RuleDto.Response> getActiveRules() {
        List<RuleDto.Response> rules = ruleService.findActiveRules();
        return new ResponseList<>(rules);
    }

    @PostMapping("/api/v1/rules")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "신규 룰 등록")
    public RuleDto.Response createRule(@Valid @RequestBody RuleDto.CreateRequest request) {
        return ruleService.createRule(request);
    }

    @PutMapping("/api/v1/rules/{id}")
    @Operation(summary = "기존 룰 정보 수정")
    public RuleDto.Response updateRule(
            @PathVariable String id,
            @Valid @RequestBody RuleDto.UpdateRequest request) {
        return ruleService.updateRule(id, request);
    }

    @DeleteMapping("/api/v1/rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "룰 삭제")
    public void deleteRule(@PathVariable String id) {
        ruleService.deleteRule(id);
    }
}

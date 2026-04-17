package com.itmasters.icon.api.standardfield.adapter.in.web;

import com.itmasters.icon.api.common.response.ResponseList;
import com.itmasters.icon.api.standardfield.adapter.in.web.dto.StandardFieldDto;
import com.itmasters.icon.api.standardfield.application.service.StandardFieldService;
import com.itmasters.icon.common.domain.rule.FieldCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "표준 필드 관리 API", description = "표준 필드 조회 및 관리 API")
public class StandardFieldController {
    
    private final StandardFieldService standardFieldService;
    
    @GetMapping("/api/v1/standard-fields")
    @Operation(summary = "표준 필드 목록 조회", description = "모든 표준 필드 목록을 조회합니다")
    public ResponseList<StandardFieldDto.Response> getAllStandardFields() {
        List<StandardFieldDto.Response> fields = standardFieldService.getAllStandardFields();
        return new ResponseList<>(fields);
    }
    
    @GetMapping("/api/v1/standard-fields/categories")
    @Operation(summary = "카테고리별 표준 필드 조회", description = "카테고리별로 그룹핑된 표준 필드를 조회합니다")
    public Map<FieldCategory, List<StandardFieldDto.Response>> getStandardFieldsByCategory() {
        return standardFieldService.getStandardFieldsByCategory();
    }
    
    @GetMapping("/api/v1/standard-fields/search")
    @Operation(summary = "표준 필드 검색", description = "키워드로 표준 필드를 검색합니다")
    public ResponseList<StandardFieldDto.Response> searchStandardFields(@RequestParam String keyword) {
        List<StandardFieldDto.Response> fields = standardFieldService.searchStandardFields(keyword);
        return new ResponseList<>(fields);
    }
    
    @GetMapping("/api/v1/standard-fields/{fieldId}")
    @Operation(summary = "표준 필드 상세 조회", description = "특정 표준 필드의 상세 정보를 조회합니다")
    public StandardFieldDto.Response getStandardField(@PathVariable String fieldId) {
        return standardFieldService.getStandardField(fieldId);
    }
    
    @GetMapping("/api/v1/standard-fields/data-type/{dataType}")
    @Operation(summary = "데이터 타입별 표준 필드 조회", description = "특정 데이터 타입의 표준 필드를 조회합니다")
    public ResponseList<StandardFieldDto.Response> getStandardFieldsByDataType(@PathVariable String dataType) {
        List<StandardFieldDto.Response> fields = standardFieldService.getStandardFieldsByDataType(dataType);
        return new ResponseList<>(fields);
    }
}
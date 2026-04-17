package com.itmasters.icon.api.metadata.adapter.in.web;

import com.itmasters.icon.api.common.response.ResponseList;
import com.itmasters.icon.api.metadata.adapter.in.web.dto.DataSourceTypeMetadataResponse;
import com.itmasters.icon.api.metadata.adapter.in.web.dto.EntityTypeMetadataResponse;
import com.itmasters.icon.api.metadata.adapter.in.web.dto.FieldMetadataResponse;
import com.itmasters.icon.api.metadata.adapter.in.web.dto.RiskLevelMetadataResponse;
import com.itmasters.icon.api.metadata.adapter.in.web.dto.RuleDomainMetadataResponse;
import com.itmasters.icon.api.metadata.application.service.MetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "메타데이터 관리 API", description = "시스템 메타데이터 조회 API")
public class MetadataController {
    
    private final MetadataService metadataService;
    
    @GetMapping("/api/v1/metadata/rule-fields")
    @Operation(summary = "룰 필드 메타데이터 조회", description = "룰 생성 시 사용 가능한 필드 목록을 조회합니다")
    public ResponseList<FieldMetadataResponse> getRuleFields() {
        List<FieldMetadataResponse> fields = metadataService.getAvailableFields();
        return new ResponseList<>(fields);
    }


    
    @GetMapping("/api/v1/metadata/data-source-types")
    @Operation(summary = "데이터 소스 타입 메타데이터 조회", description = "데이터 소스 타입 목록을 조회합니다")
    public ResponseList<DataSourceTypeMetadataResponse> getDataSourceTypes() {
        List<DataSourceTypeMetadataResponse> types = metadataService.getDataSourceTypes();
        return new ResponseList<>(types);
    }

    @GetMapping("/api/v1/metadata/rule-domains")
    @Operation(summary = "룰 도메인 메타데이터 조회", description = "룰 도메인 목록을 조회합니다")
    public ResponseList<RuleDomainMetadataResponse> getRuleDomains() {
        List<RuleDomainMetadataResponse> domains = metadataService.getRuleDomains();
        return new ResponseList<>(domains);
    }

    @GetMapping("/api/v1/metadata/risk-levels")
    @Operation(summary = "위험 레벨 메타데이터 조회", description = "시나리오에 적용할 수 있는 위험 레벨 목록을 조회합니다")
    public ResponseList<RiskLevelMetadataResponse> getRiskLevels() {
        List<RiskLevelMetadataResponse> riskLevels = metadataService.getRiskLevels();
        return new ResponseList<>(riskLevels);
    }

    @GetMapping("/api/v1/metadata/entity-types")
    @Operation(summary = "엔티티 타입 메타데이터 조회", description = "시나리오의 주요 엔티티 타입 목록을 조회합니다")
    public ResponseList<EntityTypeMetadataResponse> getEntityTypes() {
        List<EntityTypeMetadataResponse> entityTypes = metadataService.getEntityTypes();
        return new ResponseList<>(entityTypes);
    }
}

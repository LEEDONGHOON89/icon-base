package com.itmasters.icon.api.dataprofile.adapter.in.web;

import com.itmasters.icon.api.common.response.ResponseList;
import com.itmasters.icon.api.dataprofile.adapter.in.web.dto.DataProfileDto;
import com.itmasters.icon.api.dataprofile.application.port.in.DataProfileUseCase;
import com.itmasters.icon.api.dataprofile.application.service.DataProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 데이터 프로파일 API 컨트롤러
 */
@Tag(name = "Data Profile", description = "데이터 프로파일 관리 API")
@RestController
@RequiredArgsConstructor
public class DataProfileController {
    
    private final DataProfileService dataProfileService;
    
    @Operation(summary = "프로파일 생성", description = "새로운 데이터 프로파일을 생성합니다 (detect_key 지원)")
    @PostMapping("/api/v1/data-profiles")
    public DataProfileDto.Response createProfile(@Valid @RequestBody DataProfileDto.CreateRequest request) {
        // detect_key를 포함한 새로운 메서드 호출
        return dataProfileService.createProfile(request);
    }
    
    @Operation(summary = "프로파일 수정", description = "기존 데이터 프로파일을 수정합니다 (detect_key 지원)")
    @PutMapping("/api/v1/data-profiles/{profileId}")
    public DataProfileDto.Response updateProfile(
            @PathVariable String profileId,
            @Valid @RequestBody DataProfileDto.UpdateRequest request) {
        
        // detect_key를 포함한 새로운 메서드 호출
        return dataProfileService.updateProfile(profileId, request);
    }
    
    @Operation(summary = "프로파일 조회", description = "프로파일 ID로 상세 정보를 조회합니다")
    @GetMapping("/api/v1/data-profiles/{profileId}")
    public DataProfileDto.Response getProfile(@PathVariable String profileId) {
        return dataProfileService.getProfile(profileId);
    }
    
    @Operation(summary = "데이터소스별 프로파일 목록 조회", description = "특정 데이터소스의 모든 프로파일을 조회합니다")
    @GetMapping("/api/v1/data-sources/{dataSourceId}/profiles")
    public ResponseList<DataProfileDto.Response> getProfilesByDataSource(@PathVariable String dataSourceId) {
        List<DataProfileDto.Response> responses = dataProfileService.getProfilesByDataSource(dataSourceId);
        return new ResponseList<>(responses);
    }
    
    @Operation(summary = "프로파일 활성화/비활성화", description = "프로파일의 활성화 상태를 토글합니다")
    @PatchMapping("/api/v1/data-profiles/{profileId}/toggle")
    public DataProfileDto.Response toggleProfile(@PathVariable String profileId) {
        return dataProfileService.toggleProfile(profileId);
    }
    
    @Operation(summary = "프로파일 삭제", description = "데이터 프로파일을 삭제합니다")
    @DeleteMapping("/api/v1/data-profiles/{profileId}")
    public void deleteProfile(@PathVariable String profileId) {
        dataProfileService.deleteProfile(profileId);
    }
}
package com.itmasters.icon.engine.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Step1 데이터 처리 결과 DTO
 * 데이터소스 처리 및 실행 로그 생성 결과를 담는 클래스
 */
@Getter
public class DataProcessingStepResultDto {
    private final Long execDsMpId;
    private final List<Map<String, Object>> data;
    private final boolean success;
    private final String errorMessage;
    
    private DataProcessingStepResultDto(Long execDsMpId, List<Map<String, Object>> data, 
                                    boolean success, String errorMessage) {
        this.execDsMpId = execDsMpId;
        this.data = data;
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    public static DataProcessingStepResultDto success(Long execDsMpId, List<Map<String, Object>> data) {
        return new DataProcessingStepResultDto(execDsMpId, data, true, null);
    }
    
    public static DataProcessingStepResultDto failed(Long execDsMpId, String errorMessage) {
        return new DataProcessingStepResultDto(execDsMpId, new ArrayList<>(), false, errorMessage);
    }
}
package com.itmasters.icon.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 데이터 처리 결과 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class DataProcessingResultDto {

    private final boolean success;
    private final List<Map<String, Object>> rawData;
    private final List<Map<String, Object>> mappedData;
    private final SchemaValidationResult validation;
    private final String errorMessage;

    public static DataProcessingResultDto success(List<Map<String, Object>> rawData,
                                                  List<Map<String, Object>> mappedData,
                                                  SchemaValidationResult validation) {
        return DataProcessingResultDto.builder()
                .success(true)
                .rawData(rawData != null ? List.copyOf(rawData) : List.of())
                .mappedData(mappedData != null ? List.copyOf(mappedData) : List.of())
                .validation(validation)
                .errorMessage(null)
                .build();
    }

    public static DataProcessingResultDto failed(String errorMessage) {
        return DataProcessingResultDto.builder()
                .success(false)
                .rawData(List.of())
                .mappedData(List.of())
                .validation(null)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * @deprecated mappedData 사용을 명시적으로 호출하기 위해 getMappedData() 사용 권장
     */
    @Deprecated
    public List<Map<String, Object>> getData() {
        return mappedData;
    }

    public int getDataCount() {
        return mappedData != null ? mappedData.size() : 0;
    }
}

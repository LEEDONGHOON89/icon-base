package com.itmasters.icon.engine.dto;

import com.itmasters.icon.engine.adapter.out.persistence.entity.ExecDsMpEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Step1 처리 결과 DTO - 원본 추적 정보 포함
 * 
 * 데이터 읽기 및 mapped_storages 저장 단계의 결과:
 * - 각 데이터 행은 mapped_storage_id와 함께 저장
 * - Event Stream 생성 시 원본 추적성 보장
 * - Raw 데이터와 메타데이터 분리로 명확한 책임 구분
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class Step1Result {
    
    /** 실행 ID (exec_ds_mp_id) */
    private final Long execDsMpId;
    
    /** 데이터소스 ID */
    private final String dataSourceId;
    
    /** 읽어온 원본 데이터 + 추적 정보 */
    private final List<MappedDataRow> mappedDataRows;
    
    /** 처리 성공 여부 */
    private final boolean success;
    
    /** 에러 메시지 (실패 시) */
    private final String errorMessage;
    
    /** 처리된 데이터 건수 */
    private final int totalRows;
    
    /**
     * 성공 결과 생성
     */
    public static Step1Result success(ExecDsMpEntity execDsMp, List<MappedDataRow> mappedDataRows) {
        return success(execDsMp, mappedDataRows, mappedDataRows != null ? mappedDataRows.size() : 0);
    }

    public static Step1Result success(ExecDsMpEntity execDsMp,
                                      List<MappedDataRow> mappedDataRows,
                                      int totalRows) {
        return Step1Result.builder()
                .execDsMpId(execDsMp.getExecDsMpId())
                .dataSourceId(execDsMp.getDataSourceId())
                .mappedDataRows(mappedDataRows != null ? mappedDataRows : List.of())
                .success(true)
                .totalRows(totalRows)
                .build();
    }

    public static Step1Result successWithLanding(ExecDsMpEntity execDsMp, int totalRows) {
        return success(execDsMp, List.of(), totalRows);
    }
    
    /**
     * 실패 결과 생성
     */
    public static Step1Result failed(ExecDsMpEntity execDsMp, String errorMessage) {
        return Step1Result.builder()
                .execDsMpId(execDsMp.getExecDsMpId())
                .dataSourceId(execDsMp.getDataSourceId())
                .mappedDataRows(List.of())
                .success(false)
                .errorMessage(errorMessage)
                .totalRows(0)
                .build();
    }
    
    /**
     * 데이터가 있는지 확인
     */
    public boolean hasData() {
        return mappedDataRows != null && !mappedDataRows.isEmpty();
    }
    
    /**
     * 하위 호환성을 위한 Raw 데이터 추출 메서드
     * 
     * @deprecated MappedDataRow 사용을 권장 (원본 추적 정보 포함)
     */
    @Deprecated
    public List<Map<String, Object>> getRawData() {
        if (mappedDataRows == null) {
            return new ArrayList<>();
        }
        return mappedDataRows.stream()
                .map(MappedDataRow::getRawData)
                .toList();
    }

    public Step1Result withMappedData(List<MappedDataRow> mappedDataRows) {
        List<MappedDataRow> rows = mappedDataRows != null ? mappedDataRows : List.of();
        return this.toBuilder()
                .mappedDataRows(rows)
                .totalRows(rows.size())
                .success(true)
                .errorMessage(null)
                .build();
    }
}

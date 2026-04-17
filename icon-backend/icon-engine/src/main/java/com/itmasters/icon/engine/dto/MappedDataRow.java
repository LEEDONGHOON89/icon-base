package com.itmasters.icon.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 매핑된 데이터 행 DTO - Event Stream 저장 시 원본 추적 정보 포함
 *
 * 설계 원칙:
 * - 각 데이터 행은 어느 mapped_storages에서 왔는지 추적 가능
 * - Raw 데이터 + 메타데이터 조합으로 완전한 추적성 제공
 * - Event Stream 저장 시 mapped_storage_id 연결 정보 유지
 * - landing_record_id로 트랜잭션 단위 추적 가능 (개별 레코드 추적용)
 */
@Getter
@Builder
@AllArgsConstructor
public class MappedDataRow {

    /**
     * 원본 추적 ID - mapped_storages 테이블의 PK
     */
    private final Long mappedDataStorageId;

    /**
     * 트랜잭션 단위 추적 ID - landing_records 테이블의 PK
     * 개별 레코드 단위로 처리 상태를 추적할 때 사용
     */
    private final Long landingRecordId;

    /**
     * 트랜잭션 ID - 비즈니스 트랜잭션 추적용
     * DataSource의 transaction_id_field 설정에서 추출된 값
     */
    private final String transactionId;

    /**
     * Raw 데이터 맵 - 원본 CSV/JSON의 모든 필드 포함
     */
    private final Map<String, Object> rawData;

    /**
     * 정적 팩토리 메서드 - 원본 데이터와 추적 ID 결합 (transaction_id 포함)
     */
    public static MappedDataRow of(Long mappedDataStorageId, Long landingRecordId, String transactionId, Map<String, Object> rawData) {
        return new MappedDataRow(mappedDataStorageId, landingRecordId, transactionId, rawData);
    }

    /**
     * 정적 팩토리 메서드 - 호환성 유지 (transaction_id 없이)
     * @deprecated transaction_id를 포함하는 of(Long, Long, String, Map) 사용 권장
     */
    @Deprecated
    public static MappedDataRow of(Long mappedDataStorageId, Long landingRecordId, Map<String, Object> rawData) {
        return new MappedDataRow(mappedDataStorageId, landingRecordId, null, rawData);
    }

    /**
     * 특정 필드 값 추출 헬퍼 메서드
     */
    public Object getFieldValue(String fieldName) {
        return rawData.get(fieldName);
    }
    
    /**
     * 필드 존재 여부 확인
     */
    public boolean hasField(String fieldName) {
        return rawData.containsKey(fieldName);
    }
    
    /**
     * 디버깅용 문자열 표현
     */
    @Override
    public String toString() {
        return String.format("MappedDataRow{id=%d, fields=%d}", 
                mappedDataStorageId, rawData.size());
    }
}

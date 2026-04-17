package com.itmasters.icon.engine.mapping;

import java.util.List;
import java.util.Map;

/**
 * 필드 매핑 엔진 인터페이스
 * DataSourceSchema를 기반으로 원본 필드명을 표준 필드명으로 매핑
 *
 * Note: ProfileSchema 기반 매핑은 사용되지 않아 제거됨 (2025-10-26)
 */
public interface FieldMappingEngine {

    /**
     * DataSourceSchema 기반 여러 row 일괄 매핑
     *
     * @param sourceRows 원본 데이터 목록
     * @param dataSourceId 데이터소스 ID
     * @return 매핑된 데이터 목록 (DataSourceSchema의 필드명 사용)
     */
    List<Map<String, Object>> mapByDataSource(List<Map<String, Object>> sourceRows, String dataSourceId);
}
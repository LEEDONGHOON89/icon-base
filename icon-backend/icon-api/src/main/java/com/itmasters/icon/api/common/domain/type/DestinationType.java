package com.itmasters.icon.api.common.domain.type;

/**
 * 데이터 목적지 타입
 * profiles 테이블의 destination_type 컬럼에 사용되는 값
 */
public enum DestinationType {
    /**
     * event_stream 테이블에만 저장
     */
    EVENT_STREAM,

    /**
     * entity_attributes 테이블에만 저장
     */
    ENTITY,

    /**
     * event_stream + entity_attributes 테이블 양쪽에 저장
     */
    BOTH
}

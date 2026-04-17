package com.itmasters.icon.common.domain;

/**
 * 도메인 엔티티 타입
 * entity_attributes, event_stream 등에서 사용되는 엔티티 타입
 */
public enum DomainEntityType {
    /**
     * 고객
     */
    CUSTOMER,

    /**
     * 계좌
     */
    ACCOUNT,

    /**
     * 디바이스
     */
    DEVICE,

    /**
     * 인증
     */
    AUTHENTICATION,

    /**
     * 직원 (내부통제용)
     */
    EMPLOYEE,

    /**
     * 이체 (이체승인 등)
     */
    TRANSFER,

    /**
     * 제재 대상자
     */
    SANCTIONED_ENTITY,

    /**
     * 접근로그
     */
    ACCESS,

    /**
     * 법인정보
     */
    CORPORATE,

    /**
     * 파생상품
     */
    DERIVATIVE,

    /**
     * 펀드 (집합투자기구)
     */
    FUND,

    /**
     * 증권 (주식, 채권 등)
     */
    SECURITY,

    /**
     * 보유 현황 (고객-증권 관계)
     */
    HOLDING;

    /**
     * 문자열로부터 DomainEntityType을 찾음
     *
     * @param value 엔티티 타입 문자열 (대소문자 구분 없음)
     * @return DomainEntityType 또는 null
     */
    public static DomainEntityType fromString(String value) {
        if (value == null) {
            return null;
        }

        try {
            return DomainEntityType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

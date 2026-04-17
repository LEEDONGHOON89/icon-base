package com.itmasters.icon.common.domain.type;

/**
 * 프로파일 용도 정의
 * 스키마 프로파일의 분석 관점을 나타내는 공통 코드
 */
public enum ProfilePurpose {
    DEFAULT("기본"),
    INGEST("데이터 수집"),
    SECURITY("보안 분석"),
    PERFORMANCE("성능 분석"),
    BUSINESS("비즈니스 분석"),
    COMPLIANCE("컴플라이언스");

    private final String description;

    ProfilePurpose(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
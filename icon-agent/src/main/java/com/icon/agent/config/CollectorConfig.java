package com.icon.agent.config;

/**
 * 모든 수집기 설정의 기본 인터페이스.
 * [2026-02-25] 다양한 타입의 수집기를 통합 목록으로 지원하기 위해 생성
 */
public interface CollectorConfig {
    
    /**
     * 수집기의 고유 식별자를 반환한다.
     * [2026-02-25] 모든 수집기 타입에 필수
     */
    String getId();
    
    /**
     * 수집기 타입(FILE, JDBC 등)을 반환한다.
     * [2026-02-25] config.yaml에서 수집기 타입 구분에 사용
     */
    String getType();
    
    /**
     * 수집기의 표시 이름을 반환한다.
     * [2026-02-25] 모든 수집기 타입에 필수
     */
    String getName();
    
    /**
     * 수집기 활성화 여부를 반환한다.
     * [2026-02-25] 모든 수집기 타입에 필수
     */
    boolean isEnabled();
    
    /**
     * 폴링 주기(밀리초)를 반환한다.
     * [2026-02-25] 모든 수집기 타입에 필수
     */
    long getPollIntervalMs();
    
    /**
     * 한 번의 폴링 주기에 읽을 최대 라인/행 수를 반환한다.
     * [2026-02-25] 모든 수집기 타입에 필수
     */
    int getMaxLinesPerPoll();

    /**
     * 단일 레코드 content 최대 바이트 크기를 반환한다. (0이면 제한 없음)
     * [2026-02-25] 3번: 레코드 크기 제한 — 초과 시 잘라내고 [TRUNCATED] 마커 추가
     */
    int getMaxRecordBytes();
}

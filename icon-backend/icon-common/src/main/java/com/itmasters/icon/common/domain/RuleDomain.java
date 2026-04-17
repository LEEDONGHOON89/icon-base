package com.itmasters.icon.common.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 룰 도메인 정의 및 원자 룰 정책
 * 
 * == 설계 원칙 ==
 * 1. 원자 룰 정책: 각 룰은 더 이상 분해할 수 없는 최소 단위의 조건
 * 2. 시나리오 기반: 여러 원자 룰들의 AND 조합으로 복잡한 탐지 로직 구현
 * 3. group_key 중심: 고객별 소량 데이터 처리로 성능 최적화
 * 
 * == 원자 룰 vs 시나리오 ==
 * • 원자 룰 예시: "10만원 이상", "30분 내 3회 이상", "만65세 이상"
 * • 시나리오 예시: "30분 내 10만원 이상, 3회 이상 이체" (2개 원자 룰의 AND 조합)
 * 
 * == EventStream 구조 ==
 * • 순수 원본 데이터 저장: event_data JSON에 모든 정보 포함
 * • 룰 실행 시 JSON에서 필요한 날짜 필드 동적 추출
 * • 각 도메인별 fieldDatetime으로 해당 날짜 필드 지정(서로 겹치면 안됨)
 */
@Getter
@AllArgsConstructor
public enum RuleDomain {
    // == 핵심 비즈니스 도메인 ==
    LOGIN("로그인/인증", "로그인 실패, 인증 상태 등", "login_datetime"),
    ATM("ATM 거래", "ATM 출금, 입금, 거래 이력 등", "transaction_datetime"),
    FINANCIAL_TRANSACTION("금융거래", "이체, 입출금, 거액거래, 대출 등", "transaction_date"),
    DEVICE_SECURITY("디바이스보안", "기기 변경, 디바이스 이력, 새 단말 등", "event_datetime"),
    CUSTOMER("고객", "고객 나이, 등급, 상태 등", "created_at"),
    ACCOUNT("계좌", "계좌 잔액, 개설, 상태 등", "updated_at"),
    FREQUENCY("빈도", "시간 범위 내 발생 횟수 (예: 30분 내 3회)", "occurrence_time");
    
    private final String label;
    private final String description;
    private final String fieldDatetime;

}

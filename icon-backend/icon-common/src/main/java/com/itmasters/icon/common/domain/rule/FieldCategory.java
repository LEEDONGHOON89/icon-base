package com.itmasters.icon.common.domain.rule;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 룰 필드의 카테고리
 */
@Getter
@RequiredArgsConstructor
public enum FieldCategory {
    // 기본 카테고리
    TRANSACTION("거래 관련", "거래 금액, 유형 등"),
    CUSTOMER("고객 정보", "고객 나이, 위험도 등"),
    ACCOUNT("계좌 관련", "계좌 상태, 개설일 등"),
    AUTH("인증 관련", "인증서, OTP, 생체인증 등"),
    SECURITY("보안 관련", "인증, 접속 정보 등"),
    DATETIME("날짜/시간", "생성일, 수정일 등"),
    TEMPORAL("시간 관련", "시간대, 타임스탬프 등"),
    SYSTEM("시스템 관련", "서버, DB, 테이블 정보 등"),
    IDENTIFIER("식별자 관련", "ID, UUID, 고유번호 등"),
    FRAUD("사기 관련", "사기 탐지, 위험 점수 등"),
    ECOMMERCE("전자상거래", "주문, 결제, 배송 등"),
    RISK("위험 관련", "위험도, 신용도 등"),
    NETWORK("네트워크 관련", "IP, 포트, 프로토콜 등"),
    ACTIVITY("활동 관련", "로그인, 접속, 이벤트 등"),
    LOCATION("위치 관련", "GPS, 주소, 국가 등"),
    
    // 금융권 특화 카테고리
    ACCESS("접속/로그인 관련", "IP, 로그인 방법 등"),
    DEVICE("디바이스 관련", "디바이스 유형, OS 등"),
    CERTIFICATE("인증서/OTP 관련", "인증 관련 정보"),
    LOAN("대출 관련", "대출 관련 정보"),
    OPEN_BANKING("오픈뱅킹 관련", "오픈뱅킹 거래 정보"),
    ATM("ATM 관련", "ATM 거래 정보"),
    BLACKLIST("블랙/그레이리스트", "차단 목록 관련"),

    // 내부통제 특화 카테고리
    TRANSFER_APPROVAL("이체 승인 관련", "승인자, 승인단계, 자기승인 여부 등"),
    TRANSFER_LIMIT("이체 한도 관련", "일일 한도, 잔여 한도, 한도 초과 여부 등"),
    EMPLOYEE("직원 정보", "직원ID, 부서, 직급, 재직상태, 권한레벨 등");
    
    private final String label;
    private final String description;
}
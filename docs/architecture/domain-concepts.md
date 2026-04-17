# ICON - 도메인 개념 정의

> 작성: 2026-04-16

---

## 1. 핵심 개념 계층

```
데이터소스 → (프로파일 매핑) → 표준 필드
                                    ↓
                              센서(S_)  ─── 엔티티 필드
                                    ↓          ↓
                              룰(AGG_) ────────┘
                                    ↓
                            시나리오(AND/OR)
                                    ↓
                               탐지 결과
                                    ↓
                            탐지 조치 (BLOCK 등)
```

---

## 2. 데이터소스 (DataSource)

### 2.1 개요
원본 데이터 연결 설정. 어떤 외부 시스템에서 데이터를 수집할지 정의한다.

### 2.2 지원 타입
| 타입 | 설명 |
|---|---|
| PostgreSQL | JDBC 기반 DB 연결 |
| MySQL | JDBC 기반 DB 연결 |
| CSV | 파일 기반 수집 |
| JSON | 파일 기반 수집 |
| Kafka | 스트림 수집 |
| API | REST API 수집 |

### 2.3 프로파일 (Profile)
- 원본 필드 → 표준 필드 매핑 규칙
- 예: `customer_id` → `entity_id`, `amount` → `transaction_amount`
- 1개 데이터소스에 여러 프로파일 가능
- **제약**: 타입 생성 후 변경 불가, 룰 연결된 데이터소스 삭제 불가

---

## 3. 표준 필드 (Standard Field)

### 3.1 개요
이벤트(건별 발생 데이터)의 속성을 정의하는 시스템 레벨 필드. **읽기 전용**.

### 3.2 데이터 타입 색상 코딩
| 타입 | 색상 |
|---|---|
| STRING | 파랑 |
| NUMBER | 초록 |
| BOOLEAN | 보라 |
| DATE | 주황 |
| DATETIME | 핑크 |
| TIMESTAMP | 남색 |
| ARRAY | 노랑 |
| OBJECT | 빨강 |

### 3.3 주요 표준 필드 예시
| 필드 ID | 타입 | 설명 |
|---|---|---|
| `entity_id` | STRING | 엔티티 식별자 |
| `entity_type` | STRING | 엔티티 종류 |
| `event_timestamp` | DATETIME | 이벤트 발생 시각 |
| `transaction_amount` | NUMBER | 거래 금액 |
| `ip_address` | STRING | IP 주소 |
| `risk_score` | NUMBER | 위험 점수 |

---

## 4. 엔티티 필드 (Entity Field)

### 4.1 개요
엔티티 프로파일(누적/갱신되는 정적 속성)을 정의하는 필드. **읽기 전용**.

### 4.2 표준 필드와의 차이
| 구분 | 표준 필드 | 엔티티 필드 |
|---|---|---|
| 대상 | 이벤트 (건별 발생) | 엔티티 (누적/갱신) |
| 갱신 | 이벤트마다 새로 생성 | SYNC-1에서 누적 갱신 |
| 참조 방식 | `field_id` 직접 | `entity.field_id` 형식 |
| 예시 | `transaction_amount` | `entity.risk_score > 80` |

### 4.3 엔티티 타입별 주요 필드
**User/Customer**
- `risk_score` - 위험 점수
- `total_transaction_count` - 총 거래 건수
- `is_vip` - VIP 여부
- `is_blocked` - 차단 여부
- `login_failure_count_30d` - 최근 30일 로그인 실패 횟수
- `high_risk_countries` - 고위험 국가 접속 이력

**Account**
- `account_age_days` - 계좌 개설 경과일
- `average_transaction_amount` - 평균 거래 금액
- `dormant_days` - 휴면 기간
- `linked_users` - 연결된 사용자 수
- `is_corporate` - 법인 계좌 여부

**IP Address / Device**
- `is_vpn` - VPN 여부
- `is_tor` - Tor 네트워크 여부
- `threat_score` - 위협 점수
- `associated_users` - 연결된 사용자 수
- `suspicious_activity_count` - 의심 활동 횟수

---

## 5. 센서 (Sensor)

### 5.1 개요
단일 이벤트에 대한 Predicate 조건 필터. 탐지 파이프라인의 첫 번째 계층.

### 5.2 ID 규칙
- `S_` 접두사 필수
- 대문자, 숫자, 언더스코어만 허용
- 생성 후 변경 불가 (수정 시 새로 생성)
- 예: `S_LOGIN_FAIL`, `S_LARGE_TRANSFER`

### 5.3 조건 설정 방식
- **Builder 모드**: UI에서 필드/연산자/값 선택 (권장)
- **JSON 모드**: Predicate 조건 JSON 직접 입력

### 5.4 참조 필드
- 조건 필드: `standard_fields` 테이블에서 동적 조회
- 낙관적 잠금(Optimistic Locking)으로 동시 편집 충돌 방지

---

## 6. 룰 (Rule / Aggregation Rule)

### 6.1 개요
센서 탐지 결과를 시간창(Window) 기반으로 집계하는 중간 계층. 시나리오 판정의 입력이 된다.

### 6.2 ID 규칙
- `AGG_` 접두사 필수
- 예: `AGG_MULTIPLE_LOGIN_FAIL`, `AGG_HIGH_AMOUNT_TRANSFER`

### 6.3 평가 모드
| 모드 | 설명 |
|---|---|
| `WINDOW` | 시간창 기반 집계 (대부분의 경우) |
| `SINGLE_ROW` | 단일 이벤트 즉시 평가 |

### 6.4 집계 연산자
| 연산자 | 설명 |
|---|---|
| `COUNT_WITHIN` | 시간창 내 이벤트 횟수 ≥ 임계값 |
| `SUM_WITHIN` | 시간창 내 특정 필드 합계 ≥ 임계값 |
| `DISTINCT_COUNT_WITHIN` | 시간창 내 고유값 개수 ≥ 임계값 |
| `SEQUENCE_WITHIN` | A 센서 이벤트 후 B 센서 이벤트 발생 패턴 |

### 6.5 예시
```
AGG_MULTIPLE_LOGIN_FAIL
= S_LOGIN_FAIL 센서 기준
  COUNT_WITHIN(account_id, 10분, 5회 이상)
→ "10분 내 동일 계좌 로그인 실패 5회 이상"
```

---

## 7. 시나리오 (Scenario)

### 7.1 개요
여러 AGG_ 룰을 AND/OR 로직으로 조합하여 최종 탐지 여부를 판정하는 최상위 계층.

### 7.2 구성 요소
| 항목 | 설명 |
|---|---|
| 이름 | 시스템 내 유일, 수정 가능 |
| 설명 | 시나리오 설명 |
| 위험수준 | BLOCK / REVIEW / INTENSIVE / MONITOR |
| 탐지영역 | 소속 도메인 |
| 주 엔티티 타입 | CUSTOMER 등 |
| 중복제거 윈도우(분) | 동일 엔티티 중복 탐지 방지 기간 |
| 엔티티 필터 | 특정 엔티티 속성 조건 (모두 AND) |

### 7.3 엔티티 필터 예시
```
customer_age >= 65   → 65세 이상 노년층 고객만 이 시나리오 적용
is_vip = true        → VIP 고객만 적용
```

### 7.4 최소 요건
- 최소 1개 이상의 AGG_ 룰 필요

### 7.5 탐지 결과 유형
| 결과 | 설명 |
|---|---|
| 전체통과 | 모든 조건 만족 |
| 부분통과 | 일부 조건 만족 |

### 7.6 심각도 평가 가이드
- 전체통과 + 고위험 시나리오 + 신규고객 → **즉시 계좌정지 검토**

---

## 8. 엔티티 (Entity)

### 8.1 개요
정적 프로파일 데이터. 이벤트(동적 시계열)와 구분되는 누적/갱신 속성의 집합.

### 8.2 엔티티 타입 5종
| 타입 | 설명 |
|---|---|
| CUSTOMER | 고객 |
| ACCOUNT | 계좌 |
| DEVICE | 기기 |
| EMPLOYEE | 임직원 |
| AUTHENTICATION | 인증 정보 |

### 8.3 엔티티 관계
엔티티 간 관계를 정의하여 관계 그래프 분석에 활용한다.

**관계 타입**:
| 타입 | 예시 |
|---|---|
| OWNS | CUSTOMER OWNS ACCOUNT |
| USES | CUSTOMER USES DEVICE |
| ACCESSES | CUSTOMER ACCESSES AUTHENTICATION |

**배열 필드 지원**: 이벤트 1건 → 여러 관계 동시 추출 가능

### 8.4 복합키 검색
엔티티 행적 검색 시 복합키 사용 가능:
```
EMP004|330-444-555666   (파이프 구분)
```

---

## 9. 도메인 (Domain)

### 9.1 개요
시나리오를 그룹핑하는 단위. 대시보드 통계, 필터링에 활용.

### 9.2 ID 규칙
- 대문자 영문, 숫자, 언더스코어만 허용
- 생성 후 변경 불가
- 예: `FRAUD`, `MONEY_LAUNDERING`, `INSIDER_TRADING`

### 9.3 속성
- 아이콘명, 색상(HEX), 표시 순서, 활성화 토글

---

## 10. 위험수준 (Risk Level)

| 수준 | 의미 | 처리 기준 |
|---|---|---|
| **BLOCK** | 즉시 차단 | 1시간 내 처리 필수 |
| **REVIEW** | 담당자 검토 | 당일 처리 |
| **INTENSIVE** | 집중 모니터링 | 주 단위 처리 |
| **MONITOR** | 일반 모니터링 | 월 단위 처리 |

---

## 11. 탐지 조치 (Detect Action)

### 11.1 조치 상태
| 상태 | 색상 | 설명 |
|---|---|---|
| PENDING (미처리) | 회색 | 조치 대기 |
| APPROVED (승인) | 파랑 | 이상거래 확인 승인 |
| REJECTED (거절) | 빨강 | 오탐 처리 |
| COMPLETED (완료) | 초록 | 처리 완료 |

### 11.2 컴플라이언스
- 사유 기록 필수 (감사 추적, audit trail)
- 모든 조치 내역은 `detect_actions` 테이블에 보관

---

## 12. AI 서포트

### 12.1 현재 기능 (구현됨)
- **RAG 기반 룰 작성 지원**: 활성화된 룰 이름/설명/조건을 AI에 전송하여 맥락 기반 질의 가능
- 빠른 질문 템플릿: 시스템개요 / 탐지영역설정 / 룰작성가이드 / 시나리오설정
- 최대 100개 세션, 마크다운 렌더링 지원

### 12.2 AI 모니터링 (미개발, 로드맵)
- Phase 1 (Q2 2026): 채팅 UI
- Phase 2 (Q3 2026): 비지도학습 패턴 (Isolation Forest, DBSCAN)
- Phase 3 (Q4 2026): 예측 분석
- Phase 4 (2027 H1): 실시간 스코어링 (XGBoost/LightGBM)
- Phase 5 (2027 H2): 딥러닝 + 자동화 / SHAP 설명가능 AI

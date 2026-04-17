# ICON - 주요 DB 테이블 목록

> 작성: 2026-04-16 (PostgreSQL icon DB 직접 확인)  
> DB: PostgreSQL 18, DB명: `icon`, 총 44개 테이블

---

## 1. 탐지 정책 정의 (Policy)

센서·룰·시나리오의 **정의** 테이블 (탐지 결과와 구분)

| 테이블 | 설명 | 주요 컬럼 |
|---|---|---|
| `sensors` | 센서(S_) 정의 | `sensor_id`, `sensor_name`, `where_json`, `is_active` |
| `rules` | 룰(AGG_) 정의 | `rule_id`, `operator`, `predicate_sensor_id`, `window_minutes`, `threshold_count`, `threshold_amount`, `group_by_fields`, `evaluation_mode` |
| `scenarios` | 시나리오 정의 | `scenario_id`, `scenario_name`, `risk_level_id`, `detection_area_id`, `primary_entity_type`, `dedup_minutes`, `entity_filter_json` |
| `scenario_rules` | 시나리오-룰 매핑 (AND/OR 조합) | `scenario_id`, `rule_id`, `operator`, `order_no`, `override_params_json` |
| `derived_rules` | 파생 룰 | - |
| `detection_areas` | 탐지 도메인 영역 | - |
| `risk_levels` | 위험수준 마스터 | `risk_level_id`, `level_code`, `level_name`, `action_type`, `color_code` |

---

## 2. 탐지 결과 (Detection Result)

| 테이블 | 설명 | 파이프라인 단계 |
|---|---|---|
| `detect_rules` | 센서/룰 탐지 결과 | DET-2-1 / DET-2-2 |
| `detect_scenarios` | 시나리오 탐지 결과 | DET-2-3 |
| `detect_sensors` | 센서 탐지 이력 | DET-2-1 |
| `detect_actions` | 탐지 조치 워크플로우 (PENDING→APPROVED 등) | - |

---

## 3. 수집 파이프라인 (Pipeline)

| 테이블 | 설명 | 파이프라인 단계 |
|---|---|---|
| `exec_ds_mp` | 데이터소스 실행 이력 (시뮬레이션/수동실행) | - |
| `landing_records` | 원본 수집 레코드 | PREP-1 |
| `mapped_storages` | 표준 필드 매핑 결과 | PREP-3 |
| `event_stream` | 이벤트 스트림 데이터 | DET-1 |
| `event_stream_groups` | 이벤트 스트림 그룹 | DET-1 |

> `exec_ds_mp` 주요 컬럼: `data_source_id`, `execution_mode`, `execution_context`, `status`, `total_rows`, `executed_by`  
> `landing_records` 주요 컬럼: `exec_ds_mp_id`, `data_source_id`, `raw_payload`, `row_index`, `ingestion_status`

---

## 4. 에이전트 관리

| 테이블 | 설명 | 주요 컬럼 |
|---|---|---|
| `agents` | 에이전트 등록 정보 | `agent_id`, `hostname`, `ip_address`, `os_info`, `agent_version`, `status` |
| `agent_sessions` | 에이전트 접속 세션 이력 | `session_id`, `agent_id`, `remote_address`, `connected_at`, `last_heartbeat_at`, `disconnect_reason` |
| `agent_collector_configs` | 에이전트 수집기 설정 | `collector_config_id`, `target_config_id`, `collector_type`, `data_source_id`, `poll_interval_ms`, `max_lines_per_poll` |
| `agent_target_configs` | 에이전트 타겟(백엔드) 연결 설정 | `agent_id`, `rpc_endpoint`, `tls_*`, `queue_capacity`, `max_batch_size`, `max_batch_ms` |

---

## 5. 데이터소스

| 테이블 | 설명 | 주요 컬럼 |
|---|---|---|
| `data_sources` | 데이터소스 정의 | - |
| `data_source_schemas` | 데이터소스 원본 필드 스키마 | - |
| `profiles` | 데이터소스 프로파일 (원본→표준 필드 매핑) | `profile_id`, `data_source_id`, `profile_purpose`, `entity_type`, `entity_id_field`, `group_key`, `timestamp_key`, `store_fields`, `event_type_mapping` |
| `ds_api_config` | API 데이터소스 연결 설정 | - |
| `ds_api_log` | API 데이터소스 수집 로그 | - |
| `ds_database_config` | DB 데이터소스 연결 설정 | - |
| `ds_database_log` | DB 데이터소스 수집 로그 | - |
| `ds_file_system_config` | 파일 데이터소스 설정 | - |
| `ds_file_system_log` | 파일 데이터소스 수집 로그 | - |

---

## 6. 엔티티 / 관계

| 테이블 | 설명 | 주요 컬럼 |
|---|---|---|
| `entity_attributes` | 엔티티 누적 속성 (프로파일, SYNC-1 갱신) | - |
| `entity_fields` | 엔티티 필드 정의 | - |
| `entity_relation_fields` | 관계 필드 정의 | - |
| `entity_relation_rules` | 관계 추출 규칙 (PREP-5-A) | - |
| `entity_relations` | 엔티티 관계 데이터 | - |
| `entity_source_records` | 엔티티 추출 이력 | - |
| `entity_update_rules` | 엔티티 업데이트 규칙 | - |
| `pattern_relations` | 패턴 기반 관계 발견 결과 (PREP-5-B) | `from_entity_type`, `relation_type`, `to_entity_type`, `confidence_score`, `approval_status`, `reviewed_by` |

---

## 7. 표준 필드 / 마스터

| 테이블 | 설명 |
|---|---|
| `standard_fields` | 표준 필드 정의 (센서 조건 필드 동적 조회 기준) |

---

## 8. AI 서포트

| 테이블 | 설명 |
|---|---|
| `ai_chat_sessions` | AI 채팅 세션 (최대 100개) |
| `ai_chat_messages` | AI 채팅 메시지 |

---

## 9. 인증 / 사용자

| 테이블 | 설명 | 주요 컬럼 |
|---|---|---|
| `users` | 사용자 정보 | `user_id`, `login_id`, `password`, `user_name`, `email`, `is_active`, `last_login_at` |
| `refresh_tokens` | JWT 리프레시 토큰 | `token`, `user_id`, `expire_dt` |

---

## 10. 감사 / 엔진

| 테이블 | 설명 |
|---|---|
| `detection_config_audit` | 설정 변경 감사 로그 (변경이력 화면) |
| `engine_execution_warnings` | 엔진 실행 경고 |

---

## 테이블명 오류 주의

| 잘못된 이름 (문서 오기재) | 실제 테이블명 |
|---|---|
| `landing_raw_records` | `landing_records` |

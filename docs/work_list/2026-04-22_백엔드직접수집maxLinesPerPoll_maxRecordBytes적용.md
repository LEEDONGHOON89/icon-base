# 2026-04-22 백엔드 직접 수집 시 maxLinesPerPoll / maxRecordBytes 적용

## 변경일자
2026-04-22

## 변경내용
FILE_SYSTEM_REALTIME / DATABASE 데이터소스에서 에이전트 없이 백엔드가 직접 수집할 때
`maxLinesPerPoll`(폴 당 최대 처리 건수)과 `maxRecordBytes`(레코드 최대 바이트)가 적용되지 않던 문제 수정.

## 변경 전 vs 변경 후

| 항목 | 변경 전 | 변경 후 |
|---|---|---|
| FILE_SYSTEM_REALTIME 백엔드 직접 | 제한 없이 전체 줄 수집 | maxLinesPerPoll 건수 제한 + maxRecordBytes 초과 시 잘라내기 |
| DATABASE 백엔드 직접 | batchSize 행 수 제한만 | maxLinesPerPoll 우선, maxRecordBytes 초과 시 문자열 값 잘라내기 |

## 변경상세

### 1. EngineDsFileSystemConfigEntity.java
**파일**: `icon-backend/icon-engine/.../entity/EngineDsFileSystemConfigEntity.java`

```java
@Column(name = "max_lines_per_poll")
private Integer maxLinesPerPoll;   // NULL → 기본값 1000

@Column(name = "max_record_bytes")
private Integer maxRecordBytes;    // NULL/0 → 제한 없음
```

### 2. EngineDsDatabaseConfigEntity.java
**파일**: `icon-backend/icon-engine/.../entity/EngineDsDatabaseConfigEntity.java`

```java
@Column(name = "max_lines_per_poll")
private Integer maxLinesPerPoll;   // NULL/0 → batchSize 기준

@Column(name = "max_record_bytes")
private Integer maxRecordBytes;    // NULL/0 → 제한 없음
```

### 3. FileSystemRealtimeService.java
**파일**: `icon-backend/icon-engine/.../realtime/FileSystemRealtimeService.java`

- `readNewLines()` 시그니처에 `EngineDsFileSystemConfigEntity config` 파라미터 추가
- 루프 조건: `while (linesRead < maxLines && ...)`
- 각 라인 파싱 전 `truncateLineIfNeeded()` 적용 (maxRecordBytes 초과 시 잘라내기)
- `truncateLineIfNeeded()` 헬퍼 메서드 추가 (icon-agent 방식과 동일)

### 4. DatabaseService.java
**파일**: `icon-backend/icon-engine/.../service/DatabaseService.java`

- `ps.setMaxRows()`: maxLinesPerPoll 우선, 없으면 batchSize
- 루프 조건: `while (rs.next() && (maxLines <= 0 || records.size() < maxLines))`
- `truncateRowIfNeeded()` 헬퍼 메서드 추가:
  - 행 전체 바이트 크기 추정 후 초과 시 문자열 컬럼 비율 잘라내기
  - 숫자/날짜 등 비문자열 값은 변경하지 않음

## 기본값 동작
| 설정 | NULL/0 인 경우 동작 |
|---|---|
| `maxLinesPerPoll` (FILE) | 1,000줄 제한 |
| `maxLinesPerPoll` (DB) | `batchSize` 값 사용 (기본 1,000) |
| `maxRecordBytes` | 제한 없음 |

## DB 마이그레이션
불필요 — `max_lines_per_poll`, `max_record_bytes` 컬럼은 이미 DB에 존재.
엔티티 필드만 추가하여 JPA가 읽어오도록 함.

# TODO-010: Windows에서 Excel 저장 시 CSV 파일 처음부터 재수집되는 오류 개선

**상태**: 🔲 미착수  
**분류**: 에이전트 버그  
**작성일**: 2026-04-23

---

## 증상

Windows 환경에서 에이전트 기동 중, CSV 파일을 Excel로 열어 수정 후 저장하면
`lastPosition`(이전 수집 위치)이 존재함에도 불구하고 **처음(offset=0)부터 재수집**이 발생한다.

---

## 근본 원인

### 1. Windows의 파일 식별 키 생성 방식

`FileCollector.extractFileKey()` 에서 파일 식별 키를 다음 방식으로 생성한다.

```java
Object key = attrs.fileKey();
if (key != null) {
    return key.toString();           // Linux/macOS: inode 번호 → 파일 교체에도 안정적
}
// Windows 폴백: fileKey()가 null 반환 → 경로 + 생성시각 조합
return path.toAbsolutePath().toRealPath().toString()
         + "@" + attrs.creationTime().toMillis();
```

| OS | 파일 키 | 특성 |
|---|---|---|
| Linux / macOS | inode 번호 | 내용 변경에도 동일 유지 |
| **Windows** | `절대경로 @ 생성시각(ms)` | 파일 교체 시 creationTime 변경 → 키 변경 |

### 2. Excel의 Atomic Save 동작

Excel은 Windows에서 파일 저장 시 **임시파일 교체 방식(Atomic Save)** 을 사용한다.

```
[저장 전] data.csv  (creationTime: 10:00:00.000)
    ↓
1. 임시 파일 ~$data.tmp 에 새 내용 기록
2. 기존 data.csv 삭제
3. ~$data.tmp → data.csv 로 rename
    ↓
[저장 후] data.csv  (creationTime: 10:15:30.500 ← 새 파일이므로 갱신됨!)
```

### 3. 파일 키 불일치 → 로테이션 오인 → offset=0 리셋

```
저장 전 fileKey: C:\data\data.csv@1713865200000  → positions 맵에 offset=1500 저장
저장 후 fileKey: C:\data\data.csv@1713866130500  → positions 맵에 없음!
```

`FileCollector.pollFile()` 의 로테이션 감지 로직이 작동하면서:

```java
PositionRecord pos = positions.get(currentKey);  // null (새 키로 조회 실패)

if (pos == null) {
    PositionRecord byPath = findByPath(path.toString()); // 경로로 구 레코드 발견
    if (byPath != null && !byPath.getFileKey().equals(currentKey)) {
        // 경로 동일 + 키 다름 → 파일 로테이션으로 판단
        positions.remove(byPath.getFileKey());  // 구 레코드(offset=1500) 삭제
    }
    // offset=0 으로 새 레코드 생성 → 처음부터 수집
    pos = new PositionRecord(currentKey, path.toString(), 0);
}
```

**에이전트 입장에서 Excel의 저장(Atomic Save)과 실제 파일 로테이션이 동일하게 보임**:
- 공통점: 경로 동일, 파일 키(creationTime 포함) 변경

---

## 재현 조건

- OS: Windows
- 파일 유형: CSV (File Collector 대상)
- 행위: 에이전트 기동 중 Excel로 해당 CSV 파일 수정 후 저장

Linux / macOS에서는 inode 기반 키를 사용하므로 동일 현상 미발생.

---

## 관련 파일

| 파일 | 관련 로직 |
|---|---|
| `icon-agent/.../collector/FileCollector.java` | `extractFileKey()`, `pollFile()` |
| `icon-agent/.../store/FilePositionStore.java` | 위치 저장/로드 (`positions.dat`) |
| `icon-agent/.../store/PositionRecord.java` | fileKey, filePath, offset 필드 |

---

## 개선 방향 (참고)

> 소스 수정 시 아래 방향 중 검토

1. **파일 크기 비교 추가**: 새 파일 크기가 구 offset보다 크면 로테이션이 아닌 수정으로 판단
2. **creationTime 허용 오차 적용**: 일정 시간 내 creationTime 변경은 Atomic Save로 간주
3. **Windows BasicFileAttributes.fileKey() 대체**: `DosFileAttributes` 또는 NIO2의 파일 ID 활용 검토
4. **로테이션 vs 수정 판별 조건 강화**: 파일 크기, 수정 시각(lastModifiedTime), 내용 일치 여부 복합 판단

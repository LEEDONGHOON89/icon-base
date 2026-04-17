package com.icon.agent.monitor;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 에이전트 모니터링 설정.
 * [2026-02-25] 시스템 리소스 및 에이전트 내부 상태 모니터링 기능 추가
 *
 * 수집 지표:
 *   - CPU 사용률 (JVM 프로세스)
 *   - Java Heap 사용량/최대값
 *   - 시스템 전체 메모리 사용량
 *   - GC 횟수 및 누적 소요 시간
 *   - JVM Thread 수
 *   - JVM Uptime
 *   - Target별 RecordQueue 깊이
 *   - Target별 Spool 파일 수
 *   - Target별 WebSocket 연결 상태
 *   - 스풀 디렉토리 디스크 사용량
 *   - Open File Descriptor 수 (Linux)
 */
public class MonitoringConfig {

    // [2026-02-25] 모니터링 활성화 여부 (false이면 수집하지 않음)
    @JsonProperty("enabled")
    private boolean enabled = true;

    // [2026-02-25] 메트릭 수집 주기 (밀리초, 기본 30초)
    @JsonProperty("intervalMs")
    private long intervalMs = 30000;

    // [2026-02-25] 모니터링 로그 파일 경로 (JSON Lines 형식으로 저장)
    //              이후 FileCollector로 수집 → 내부통제시스템 연동 가능
    @JsonProperty("logPath")
    private String logPath = "logs/agent-metrics.log";

    // [2026-02-25] 스풀 디렉토리 루트 (디스크 사용량 계산 대상)
    @JsonProperty("spoolRootPath")
    private String spoolRootPath = "data";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public String getSpoolRootPath() {
        return spoolRootPath;
    }

    public void setSpoolRootPath(String spoolRootPath) {
        this.spoolRootPath = spoolRootPath;
    }
}

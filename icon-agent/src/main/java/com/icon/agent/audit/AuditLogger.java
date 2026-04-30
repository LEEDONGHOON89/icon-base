package com.icon.agent.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * [2026-04-21] 에이전트 감사 로그 유틸리티.
 *
 * 금융권 컴플라이언스 요건에 따라 에이전트의 주요 동작을 별도 감사 로그 파일에
 * JSON Lines 형식으로 기록한다.
 *
 * 출력 파일: logs/audit.log (일별 롤링, 1년 보존)
 * 형식:
 *   {"ts":"2026-04-21T14:22:24.441","event":"AGENT_START","agentId":"agent1","host":"PC-01"}
 *   {"ts":"2026-04-21T14:22:25.100","event":"TARGET_CONNECTED","targetId":"icon-backend","endpoint":"ws://..."}
 *
 * 설계 원칙:
 *   - 감사 로그 실패가 에이전트 주 흐름에 영향을 주지 않음 (예외 무시)
 *   - 운영 로그(collector-agent.log)와 완전 분리 (additivity=false)
 *   - 정적(static) 메서드만 노출 — 인스턴스 생성 불필요
 *
 * 감사 이벤트 목록:
 *   AGENT_START          에이전트 기동
 *   AGENT_STOP           에이전트 종료
 *   TARGET_CONNECTED     서버 WebSocket 연결 성공
 *   TARGET_DISCONNECTED  서버 WebSocket 연결 끊김
 *   COLLECTOR_STARTED    수집기 시작
 *   COLLECTOR_STOPPED    수집기 중지
 *   CONFIG_CHANGED       설정 변경 (COLLECTORS_SYNC / CONFIG_UPDATE 수신)
 *   SPOOL_LIMIT_EXCEEDED 스풀 크기 한도 초과 — 오래된 파일 삭제(데이터 유실)
 */
public final class AuditLogger {

    // logback.xml의 "agent.audit" 로거로 라우팅 → logs/audit.log
    private static final Logger AUDIT = LoggerFactory.getLogger("agent.audit");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    // ── 이벤트 상수 ──────────────────────────────────────────────────────────
    public static final String AGENT_START          = "AGENT_START";
    public static final String AGENT_STOP           = "AGENT_STOP";
    public static final String TARGET_CONNECTED     = "TARGET_CONNECTED";
    public static final String TARGET_DISCONNECTED  = "TARGET_DISCONNECTED";
    public static final String COLLECTOR_STARTED    = "COLLECTOR_STARTED";
    public static final String COLLECTOR_STOPPED    = "COLLECTOR_STOPPED";
    public static final String CONFIG_CHANGED       = "CONFIG_CHANGED";
    public static final String SPOOL_LIMIT_EXCEEDED = "SPOOL_LIMIT_EXCEEDED";

    private AuditLogger() { /* static utility */ }

    // ── 공개 편의 메서드 ────────────────────────────────────────────────────

    /** 에이전트 기동 */
    public static void agentStart(String agentId) {
        ObjectNode extra = JSON.createObjectNode();
        extra.put("agentId", agentId != null ? agentId : "");
        extra.put("host", hostname());
        write(AGENT_START, null, extra);
    }

    /** 에이전트 종료 */
    public static void agentStop(String agentId) {
        ObjectNode extra = JSON.createObjectNode();
        extra.put("agentId", agentId != null ? agentId : "");
        write(AGENT_STOP, null, extra);
    }

    /** 서버 WebSocket 연결 성공 */
    public static void targetConnected(String targetId, String endpoint) {
        ObjectNode extra = JSON.createObjectNode();
        extra.put("endpoint", endpoint != null ? endpoint : "");
        write(TARGET_CONNECTED, targetId, extra);
    }

    /** 서버 WebSocket 연결 끊김 */
    public static void targetDisconnected(String targetId, String reason) {
        ObjectNode extra = JSON.createObjectNode();
        extra.put("reason", reason != null ? reason : "");
        write(TARGET_DISCONNECTED, targetId, extra);
    }

    /** 수집기 시작 — detail: 파일경로 또는 JDBC URL 등 식별 정보 */
    public static void collectorStarted(String targetId, String collectorId,
                                        String type, String detail) {
        ObjectNode extra = JSON.createObjectNode();
        extra.put("collectorId", collectorId != null ? collectorId : "");
        extra.put("type",        type        != null ? type        : "");
        extra.put("detail",      detail      != null ? detail      : "");
        write(COLLECTOR_STARTED, targetId, extra);
    }

    /** 수집기 중지 */
    public static void collectorStopped(String targetId, String collectorId, String type) {
        ObjectNode extra = JSON.createObjectNode();
        extra.put("collectorId", collectorId != null ? collectorId : "");
        extra.put("type",        type        != null ? type        : "");
        write(COLLECTOR_STOPPED, targetId, extra);
    }

    /** 설정 변경 — changeType: "COLLECTORS_SYNC" | "CONFIG_UPDATE" */
    public static void configChanged(String targetId, String changeType, String detail) {
        ObjectNode extra = JSON.createObjectNode();
        extra.put("changeType", changeType != null ? changeType : "");
        extra.put("detail",     detail     != null ? detail     : "");
        write(CONFIG_CHANGED, targetId, extra);
    }

    /**
     * 스풀 한도 초과 — 오래된 파일 삭제(데이터 유실) 발생.
     * limitType: "MAX_FILES" | "MAX_SIZE_MB"
     */
    public static void spoolLimitExceeded(String targetId, String limitType, int deletedFiles) {
        ObjectNode extra = JSON.createObjectNode();
        extra.put("limitType",    limitType != null ? limitType : "");
        extra.put("deletedFiles", deletedFiles);
        write(SPOOL_LIMIT_EXCEEDED, targetId, extra);
    }

    // ── 내부 구현 ────────────────────────────────────────────────────────────

    private static void write(String event, String targetId, ObjectNode extra) {
        try {
            ObjectNode node = JSON.createObjectNode();
            node.put("ts",    LocalDateTime.now().format(TS_FMT));
            node.put("event", event);
            if (targetId != null && !targetId.isBlank()) {
                node.put("targetId", targetId);
            }
            if (extra != null) {
                node.setAll(extra);
            }
            AUDIT.info(JSON.writeValueAsString(node));
        } catch (Exception ignored) {
            // 감사 로그 실패는 주 흐름에 영향을 주지 않음
        }
    }

    private static String hostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}

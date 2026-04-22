package com.icon.agent.shutdown;

import com.icon.agent.admin.AdminServer;
import com.icon.agent.audit.AuditLogger;
import com.icon.agent.health.HealthServer;
import com.icon.agent.monitor.AgentMonitor;
import com.icon.agent.target.TargetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles graceful shutdown of the collector agent.
 * Orchestrates stopping targets, collectors, flushes queues, and stops the
 * health server.
 * [2026-02-25] AgentMonitor 종료 처리 추가
 */
public class ShutdownManager {

    private static final Logger log = LoggerFactory.getLogger(ShutdownManager.class);

    private final TargetManager targetManager;
    private final HealthServer healthServer;
    // [2026-04-21] AdminServer 참조 추가
    private final AdminServer adminServer;
    // [2026-02-25] AgentMonitor 참조 추가 — 종료 시 스케줄러 정리
    private final AgentMonitor agentMonitor;
    // [2026-04-21] 감사 로그용 agentId
    private String agentId;

    public ShutdownManager(TargetManager targetManager, HealthServer healthServer,
                           AdminServer adminServer, AgentMonitor agentMonitor) {
        this.targetManager = targetManager;
        this.healthServer = healthServer;
        this.adminServer = adminServer;
        this.agentMonitor = agentMonitor;
    }

    // [2026-04-21] agentId 주입 — 감사 로그 AGENT_STOP에 사용
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public void register() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "shutdown-hook"));
    }

    public void shutdown() {
        log.info("Graceful shutdown initiated...");
        // [2026-04-21] 감사 로그: 에이전트 종료
        AuditLogger.agentStop(agentId);

        // 1. Stop health server + admin server
        if (healthServer != null) healthServer.stop();
        // [2026-04-21] AdminServer 종료
        if (adminServer != null) adminServer.stop();

        // 2. Stop all targets (this triggers internal stop: collectors -> spool flush
        // -> rpc close)
        if (targetManager != null) {
            targetManager.stopAll();
        }

        // [2026-02-25] 3. AgentMonitor 중지 (타겟 종료 후 마지막 메트릭 수집 완료 후 종료)
        if (agentMonitor != null) {
            agentMonitor.stop();
        }

        log.info("Shutdown complete. Goodbye.");
    }
}

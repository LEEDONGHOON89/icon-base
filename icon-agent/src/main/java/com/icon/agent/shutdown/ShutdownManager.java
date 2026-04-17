package com.icon.agent.shutdown;

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
    // [2026-02-25] AgentMonitor 참조 추가 — 종료 시 스케줄러 정리
    private final AgentMonitor agentMonitor;

    public ShutdownManager(TargetManager targetManager, HealthServer healthServer, AgentMonitor agentMonitor) {
        this.targetManager = targetManager;
        this.healthServer = healthServer;
        this.agentMonitor = agentMonitor;
    }

    public void register() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "shutdown-hook"));
    }

    public void shutdown() {
        log.info("Graceful shutdown initiated...");

        // 1. Stop health server (no longer accepting status checks)
        if (healthServer != null) {
            healthServer.stop();
        }

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

package com.icon.agent;

import com.icon.agent.config.AgentConfig;
import com.icon.agent.config.ConfigManager;
import com.icon.agent.health.HealthServer;
import com.icon.agent.monitor.AgentMonitor;
import com.icon.agent.shutdown.ShutdownManager;
import com.icon.agent.target.TargetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main entry point for the Java Lightweight Collector Agent.
 *
 * Usage: java -jar collector-agent.jar -c config.yaml
 * [2026-02-25] AgentMonitor 추가 — 시스템 리소스 및 에이전트 내부 상태 모니터링
 */
public class CollectorAgent {

    private static final Logger log = LoggerFactory.getLogger(CollectorAgent.class);

    public static void main(String[] args) {
        String configPath = "config.yaml";

        // Simple arg parsing
        for (int i = 0; i < args.length; i++) {
            if ("-c".equals(args[i]) && i + 1 < args.length) {
                configPath = args[i + 1];
            }
        }

        log.info("Starting Java Collector Agent...");

        try {
            // 1. Load config
            ConfigManager configManager = new ConfigManager(configPath);
            AgentConfig config = configManager.getConfig();

            // 2. Initialize Target Manager (ConfigManager for CONFIG_UPDATE -> config.yaml)
            TargetManager targetManager = new TargetManager(config, configManager);

            // 3. Initialize Health Server
            HealthServer healthServer = new HealthServer(config.getAgent().getHealthPort(), targetManager);

            // [2026-02-25] 4. 에이전트 모니터링 초기화
            //              CPU/Heap/메모리/GC/Queue/Spool 등 주기적 수집 → logs/agent-metrics.log
            AgentMonitor agentMonitor = new AgentMonitor(config.getMonitoring(), targetManager);

            // 5. Register Shutdown Manager
            // [2026-02-25] AgentMonitor도 ShutdownManager에 등록하여 종료 시 정리
            ShutdownManager shutdownManager = new ShutdownManager(targetManager, healthServer, agentMonitor);
            shutdownManager.register();

            // 6. Start Health Server
            healthServer.start();

            // 7. Start all targets
            targetManager.startAll();

            // [2026-02-25] 8. 모니터링 시작 (타겟 시작 이후 — Queue/Spool 참조 가능)
            agentMonitor.start();

            log.info("Agent is UP and running.");

            // Keep main thread alive
            while (true) {
                Thread.sleep(10000);
            }

        } catch (IOException e) {
            log.error("Failed to load configuration: {}", e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            log.error("Fatal error during agent initialization: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}

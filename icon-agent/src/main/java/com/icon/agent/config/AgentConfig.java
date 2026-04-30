package com.icon.agent.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.icon.agent.monitor.MonitoringConfig;
import java.util.List;

/**
 * Top-level agent configuration.
 * [2026-02-25] monitoring 섹션 추가 — 시스템 모니터링 설정
 * [2026-03-11] agentId 추가 — config.yaml 최상단 agentId 필드 지원 (내부통제 연동 시 사용)
 */
public class AgentConfig {

    @JsonProperty("agent")
    private AgentSettings agent = new AgentSettings();

    /** 에이전트 식별자 (config.yaml 최상단, 내부통제시스템 등록용) */
    @JsonProperty("agentId")
    private String agentId;

    @JsonProperty("targets")
    private List<TargetConfig> targets = new java.util.ArrayList<>();

    // [2026-02-25] 모니터링 설정 추가 (없으면 기본값 사용)
    @JsonProperty("monitoring")
    private MonitoringConfig monitoring = new MonitoringConfig();

    public AgentSettings getAgent() {
        return agent;
    }

    public void setAgent(AgentSettings agent) {
        this.agent = agent;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public List<TargetConfig> getTargets() {
        return targets;
    }

    public void setTargets(List<TargetConfig> targets) {
        this.targets = targets;
    }

    // [2026-02-25] monitoring getter/setter 추가
    public MonitoringConfig getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(MonitoringConfig monitoring) {
        this.monitoring = monitoring;
    }

    public static class AgentSettings {
        @JsonProperty("healthPort")
        private int healthPort = 8080;

        // [2026-04-21] 관리 CLI API 포트 (agent-cli.ps1 전용, localhost-only)
        @JsonProperty("adminPort")
        private int adminPort = 8081;

        public int getHealthPort() {
            return healthPort;
        }

        public void setHealthPort(int healthPort) {
            this.healthPort = healthPort;
        }

        public int getAdminPort() {
            return adminPort;
        }

        public void setAdminPort(int adminPort) {
            this.adminPort = adminPort;
        }
    }
}

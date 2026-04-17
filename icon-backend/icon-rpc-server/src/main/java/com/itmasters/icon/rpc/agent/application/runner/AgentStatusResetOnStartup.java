package com.itmasters.icon.rpc.agent.application.runner;

import com.itmasters.icon.rpc.agent.adapter.out.persistence.repository.AgentJpaRepository;
import com.itmasters.icon.rpc.agent.adapter.out.persistence.repository.AgentSessionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 내부통제시스템 기동 시 에이전트 연결 상태를 DB와 일치시키기 위해 ACTIVE/CONNECTED 를 DISCONNECTED 로 초기화한다. */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AgentStatusResetOnStartup implements ApplicationRunner {

    /** 세션 끊김 사유: 서버 재기동 */
    private static final String DISCONNECT_REASON = "server_restart";

    private final AgentSessionJpaRepository agentSessionJpaRepository;
    private final AgentJpaRepository agentJpaRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int sessions = agentSessionJpaRepository.disconnectAllSessions(DISCONNECT_REASON);
        int agents = agentJpaRepository.markAllActiveAsDisconnected();
        if (sessions > 0 || agents > 0) {
            log.info("[RPC] Startup: reset agent status - sessions disconnected={}, agents marked disconnected={}",
                    sessions, agents);
        }
    }
}
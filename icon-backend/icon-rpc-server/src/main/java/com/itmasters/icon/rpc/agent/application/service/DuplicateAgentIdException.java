package com.itmasters.icon.rpc.agent.application.service;

/**
 * [2026-04-21] 동일 agentId로 이미 활성 세션이 존재할 때 발생.
 * AgentRpcWebSocketHandler가 이 예외를 잡아 HANDSHAKE_NACK을 반환하고
 * 신규 연결을 닫는다 — 기존 에이전트 세션을 보호.
 */
public class DuplicateAgentIdException extends RuntimeException {

    private final String agentId;

    public DuplicateAgentIdException(String agentId, String existingAddress) {
        super("agentId=" + agentId + " already has an active session from " + existingAddress);
        this.agentId = agentId;
    }

    public String getAgentId() {
        return agentId;
    }
}

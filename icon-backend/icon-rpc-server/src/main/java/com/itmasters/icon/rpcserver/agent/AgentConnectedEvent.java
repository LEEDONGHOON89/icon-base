package com.itmasters.icon.rpcserver.agent;

/**
 * [2026-04-21] 에이전트 HANDSHAKE 완료 이벤트.
 *
 * AgentRpcWebSocketHandler 가 HANDSHAKE_ACK 전송 성공 후
 * ApplicationEventPublisher 를 통해 발행한다.
 *
 * icon-api 모듈의 AgentSnapshotService 가 @EventListener 로 수신하여
 * 해당 에이전트의 전체 수집기 스냅샷(COLLECTORS_SYNC)을 즉시 푸시한다.
 */
public class AgentConnectedEvent {

    private final String agentId;

    public AgentConnectedEvent(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentId() {
        return agentId;
    }
}

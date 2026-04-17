// [2026-03-06] icon-gateway.agent → icon-rpc-server 서브모듈명 변경에 따른 패키지 이동
package com.itmasters.icon.rpcserver.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * Agent WebSocket 엔드포인트 설정
 *
 * /rpc 경로로 접속하는 Agent의 WebSocket 연결을
 * AgentRpcWebSocketHandler에 위임한다.
 * 대용량 배치 수신을 위해 텍스트/바이너리 메시지 버퍼 크기를 4MB로 설정.
 */
@Slf4j
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    /** 대용량 배치 수신 허용 (1009 메시지 버퍼 초과 오류 방지) */
    private static final int MAX_MESSAGE_BUFFER_SIZE = 4 * 1024 * 1024;

    private final AgentRpcWebSocketHandler agentRpcWebSocketHandler;

    public WebSocketConfig(AgentRpcWebSocketHandler agentRpcWebSocketHandler) {
        this.agentRpcWebSocketHandler = agentRpcWebSocketHandler;
        log.info("WebSocketConfig 초기화. Handler: {}", agentRpcWebSocketHandler);
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(MAX_MESSAGE_BUFFER_SIZE);
        container.setMaxBinaryMessageBufferSize(MAX_MESSAGE_BUFFER_SIZE);
        log.info("WebSocket 메시지 버퍼 크기 설정: {} bytes (텍스트/바이너리)", MAX_MESSAGE_BUFFER_SIZE);
        return container;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("/rpc WebSocket 핸들러 등록");
        // [2026-03-06] Agent RPC 수신 엔드포인트 — wss://<host>/rpc
        registry.addHandler(agentRpcWebSocketHandler, "/rpc")
                .setAllowedOrigins("*");
    }
}

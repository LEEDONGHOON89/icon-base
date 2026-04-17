package com.icon.agent.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * RPC (WebSocket) endpoint configuration per target.
 * [2026-03-05] compress 필드 추가 — GZIP 압축 후 binary frame 전송 옵션.
 */
public class RpcConfig {

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("tls")
    private TlsConfig tls;

    /** Initial reconnect delay in ms. */
    @JsonProperty("reconnectBaseMs")
    private long reconnectBaseMs = 1000;

    /** Maximum reconnect delay in ms. */
    @JsonProperty("reconnectMaxMs")
    private long reconnectMaxMs = 60000;

    /** ACK timeout in ms before declaring batch failed. */
    @JsonProperty("ackTimeoutMs")
    private long ackTimeoutMs = 30000;

    /**
     * [2026-03-05] 배치 전송 시 GZIP 압축 여부.
     * true  → GZIP 압축 후 WebSocket binary frame 으로 전송
     * false → 압축 없이 WebSocket text frame 으로 전송 (기본값)
     * 서버 구분: text frame = JSON, binary frame = GZIP(JSON)
     */
    @JsonProperty("compress")
    private boolean compress = false;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public TlsConfig getTls() {
        return tls;
    }

    public void setTls(TlsConfig tls) {
        this.tls = tls;
    }

    public long getReconnectBaseMs() {
        return reconnectBaseMs;
    }

    public void setReconnectBaseMs(long reconnectBaseMs) {
        this.reconnectBaseMs = reconnectBaseMs;
    }

    public long getReconnectMaxMs() {
        return reconnectMaxMs;
    }

    public void setReconnectMaxMs(long reconnectMaxMs) {
        this.reconnectMaxMs = reconnectMaxMs;
    }

    public long getAckTimeoutMs() {
        return ackTimeoutMs;
    }

    public void setAckTimeoutMs(long ackTimeoutMs) {
        this.ackTimeoutMs = ackTimeoutMs;
    }

    public boolean isCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }
}

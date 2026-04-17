package com.icon.agent.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TLS configuration: keystore (client cert for mTLS) and truststore (server
 * CA).
 */
public class TlsConfig {

    @JsonProperty("keystorePath")
    private String keystorePath;

    @JsonProperty("keystorePassword")
    private String keystorePassword;

    @JsonProperty("truststorePath")
    private String truststorePath;

    @JsonProperty("truststorePassword")
    private String truststorePassword;

    /** Keystore type, default PKCS12. Works on Windows + Linux. */
    @JsonProperty("keystoreType")
    private String keystoreType = "PKCS12";

    /**
     * 내부/개발용: true 이면 서버 인증서 검증(호스트명·SAN 포함) 생략.
     * SAN 없음·IP 접속·자체서명 인증서 등에서 SSLHandshakeException 방지. 운영 환경에서는 false 권장.
     */
    @JsonProperty("insecureTrustAll")
    private boolean insecureTrustAll = false;

    public String getKeystorePath() {
        return keystorePath;
    }

    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getTruststorePath() {
        return truststorePath;
    }

    public void setTruststorePath(String truststorePath) {
        this.truststorePath = truststorePath;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }

    public String getKeystoreType() {
        return keystoreType;
    }

    public void setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
    }

    public boolean isInsecureTrustAll() {
        return insecureTrustAll;
    }

    public void setInsecureTrustAll(boolean insecureTrustAll) {
        this.insecureTrustAll = insecureTrustAll;
    }

    /** Returns true if mTLS (client cert) is configured. */
    public boolean hasMtls() {
        return keystorePath != null && !keystorePath.isBlank();
    }
}

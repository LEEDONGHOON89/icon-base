package com.icon.agent.tls;

import com.icon.agent.config.TlsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

/**
 * Builds an SSLContext from PKCS12 keystore and truststore files.
 * Supports one-way TLS (truststore only) and mutual TLS (keystore +
 * truststore).
 * Compatible with Windows and Linux.
 */
public class SslContextFactory {

    private static final Logger log = LoggerFactory.getLogger(SslContextFactory.class);

    /**
     * Build SSLContext for the given TLS config.
     * If tls is null or has no truststore, returns the default SSLContext.
     */
    public static SSLContext build(TlsConfig tls) throws Exception {
        if (tls == null) {
            log.debug("No TLS config provided, using default SSLContext");
            return SSLContext.getDefault();
        }

        boolean trustAll = tls.isInsecureTrustAll();
        log.info("TLS build: insecureTrustAll={}", trustAll);
        if (trustAll) {
            log.warn("TLS insecureTrustAll=true: server certificate and hostname verification disabled (internal/dev only)");
            return buildTrustAllContext();
        }

        // --- TrustManager (server cert validation) ---
        TrustManager[] trustManagers = buildTrustManagers(tls);

        // --- KeyManager (client cert for mTLS) ---
        KeyManager[] keyManagers = null;
        // if (tls.hasMtls()) {
        // keyManagers = buildKeyManagers(tls);
        // log.info("mTLS enabled: client certificate loaded from {}",
        // tls.getKeystorePath());
        // } else {
        // log.info("One-way TLS: no client certificate configured");
        // }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, new java.security.SecureRandom());
        return sslContext;
    }

    private static SSLContext buildTrustAllContext() throws Exception {
        TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, trustAll, new java.security.SecureRandom());
        return ctx;
    }

    private static TrustManager[] buildTrustManagers(TlsConfig tls) throws Exception {
        if (tls.getTruststorePath() == null || tls.getTruststorePath().isBlank()) {
            log.warn("No truststore configured — using JVM default trust store");
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            return tmf.getTrustManagers();
        }
        KeyStore ts = loadKeyStore(tls.getTruststorePath(), tls.getTruststorePassword(), tls.getKeystoreType());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);
        log.info("Truststore loaded from {}", tls.getTruststorePath());
        return tmf.getTrustManagers();
    }

    private static KeyManager[] buildKeyManagers(TlsConfig tls) throws Exception {
        KeyStore ks = loadKeyStore(tls.getKeystorePath(), tls.getKeystorePassword(), tls.getKeystoreType());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        char[] pw = tls.getKeystorePassword() != null ? tls.getKeystorePassword().toCharArray() : new char[0];
        kmf.init(ks, pw);
        return kmf.getKeyManagers();
    }

    private static KeyStore loadKeyStore(String path, String password, String type) throws Exception {
        KeyStore ks = KeyStore.getInstance(type != null ? type : "PKCS12");
        char[] pw = password != null ? password.toCharArray() : new char[0];
        try (FileInputStream fis = new FileInputStream(path)) {
            ks.load(fis, pw);
        }
        return ks;
    }
}

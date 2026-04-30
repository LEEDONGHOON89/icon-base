package com.icon.agent.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icon.agent.config.RpcConfig;
import com.icon.agent.config.TargetConfig;
import com.icon.agent.config.TlsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [2026-04-21] CLI로 추가된 target 연결 정보를 data/targets.json에 영속화.
 * collectors는 서버 COLLECTORS_SYNC로 런타임에 복원되므로 저장하지 않음.
 *
 * 저장 형식: RPC(endpoint/compress/reconnect/TLS) + 배치/큐/스풀/속도제한 전체 설정
 */
public class TargetStore {

    private static final Logger log = LoggerFactory.getLogger(TargetStore.class);
    private static final Path STORE = Path.of("data", "targets.json");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<Map<String, Object>>> LIST_TYPE = new TypeReference<>() {};

    public static List<TargetConfig> load() {
        if (!Files.exists(STORE)) return new ArrayList<>();
        try {
            List<Map<String, Object>> entries = JSON.readValue(STORE.toFile(), LIST_TYPE);
            List<TargetConfig> result = new ArrayList<>();
            for (Map<String, Object> e : entries) {
                TargetConfig tc = toTargetConfig(e);
                if (tc != null) result.add(tc);
            }
            log.info("data/targets.json 에서 {} 개 target 복원", result.size());
            return result;
        } catch (IOException ex) {
            log.error("data/targets.json 읽기 실패: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }

    // [2026-04-21] TLS/배치/큐/스풀/속도제한 전체 설정 저장
    public static void save(List<TargetConfig> targets) {
        try {
            Files.createDirectories(STORE.getParent());
            List<Map<String, Object>> entries = new ArrayList<>();
            for (TargetConfig tc : targets) {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("id", tc.getId());

                RpcConfig rpc = tc.getRpc();
                if (rpc != null) {
                    e.put("endpoint",        rpc.getEndpoint() != null ? rpc.getEndpoint() : "");
                    e.put("compress",        rpc.isCompress());
                    e.put("reconnectBaseMs", rpc.getReconnectBaseMs());
                    e.put("reconnectMaxMs",  rpc.getReconnectMaxMs());
                    e.put("ackTimeoutMs",    rpc.getAckTimeoutMs());

                    TlsConfig tls = rpc.getTls();
                    if (tls != null) {
                        Map<String, Object> tlsMap = new LinkedHashMap<>();
                        if (tls.getKeystorePath()       != null) tlsMap.put("keystorePath",       tls.getKeystorePath());
                        if (tls.getKeystorePassword()   != null) tlsMap.put("keystorePassword",   tls.getKeystorePassword());
                        if (tls.getTruststorePath()     != null) tlsMap.put("truststorePath",     tls.getTruststorePath());
                        if (tls.getTruststorePassword() != null) tlsMap.put("truststorePassword", tls.getTruststorePassword());
                        tlsMap.put("keystoreType",    tls.getKeystoreType());
                        tlsMap.put("insecureTrustAll", tls.isInsecureTrustAll());
                        if (!tlsMap.isEmpty()) e.put("tls", tlsMap);
                    }
                } else {
                    e.put("endpoint", "");
                    e.put("compress", false);
                }

                e.put("queueCapacity",       tc.getQueueCapacity());
                e.put("maxBatchSize",         tc.getMaxBatchSize());
                e.put("maxBatchMs",           tc.getMaxBatchMs());
                e.put("maxBatchBytes",        tc.getMaxBatchBytes());
                e.put("maxSpoolFiles",        tc.getMaxSpoolFiles());
                e.put("maxSpoolSizeMb",       tc.getMaxSpoolSizeMb());
                e.put("maxBatchesPerSecond",  tc.getMaxBatchesPerSecond());
                entries.add(e);
            }
            JSON.writerWithDefaultPrettyPrinter().writeValue(STORE.toFile(), entries);
            // [2026-04-22] 저장 완료 확인 로그 추가
            log.info("data/targets.json 저장 완료 — {} 개 target", entries.size());
        } catch (IOException ex) {
            log.error("data/targets.json 저장 실패: {}", ex.getMessage());
        }
    }

    // [2026-04-21] TLS/배치/큐/스풀/속도제한 전체 설정 복원
    @SuppressWarnings("unchecked")
    private static TargetConfig toTargetConfig(Map<String, Object> e) {
        String id       = (String) e.get("id");
        String endpoint = (String) e.get("endpoint");
        if (id == null || id.isBlank() || endpoint == null || endpoint.isBlank()) return null;

        RpcConfig rpc = new RpcConfig();
        rpc.setEndpoint(endpoint);
        rpc.setCompress(Boolean.TRUE.equals(e.get("compress")));
        if (e.get("reconnectBaseMs") instanceof Number n) rpc.setReconnectBaseMs(n.longValue());
        if (e.get("reconnectMaxMs")  instanceof Number n) rpc.setReconnectMaxMs(n.longValue());
        if (e.get("ackTimeoutMs")    instanceof Number n) rpc.setAckTimeoutMs(n.longValue());

        Map<String, Object> tlsMap = (Map<String, Object>) e.get("tls");
        if (tlsMap != null) {
            TlsConfig tls = new TlsConfig();
            tls.setKeystorePath      ((String) tlsMap.get("keystorePath"));
            tls.setKeystorePassword  ((String) tlsMap.get("keystorePassword"));
            tls.setTruststorePath    ((String) tlsMap.get("truststorePath"));
            tls.setTruststorePassword((String) tlsMap.get("truststorePassword"));
            if (tlsMap.get("keystoreType") instanceof String ks) tls.setKeystoreType(ks);
            tls.setInsecureTrustAll(Boolean.TRUE.equals(tlsMap.get("insecureTrustAll")));
            rpc.setTls(tls);
        }

        TargetConfig tc = new TargetConfig();
        tc.setId(id);
        tc.setRpc(rpc);
        if (e.get("queueCapacity")      instanceof Number n) tc.setQueueCapacity(n.intValue());
        if (e.get("maxBatchSize")        instanceof Number n) tc.setMaxBatchSize(n.intValue());
        if (e.get("maxBatchMs")          instanceof Number n) tc.setMaxBatchMs(n.longValue());
        if (e.get("maxBatchBytes")       instanceof Number n) tc.setMaxBatchBytes(n.longValue());
        if (e.get("maxSpoolFiles")       instanceof Number n) tc.setMaxSpoolFiles(n.intValue());
        if (e.get("maxSpoolSizeMb")      instanceof Number n) tc.setMaxSpoolSizeMb(n.longValue());
        if (e.get("maxBatchesPerSecond") instanceof Number n) tc.setMaxBatchesPerSecond(n.intValue());
        return tc;
    }
}

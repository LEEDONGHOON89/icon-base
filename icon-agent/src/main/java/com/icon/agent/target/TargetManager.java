package com.icon.agent.target;

import com.icon.agent.admin.TargetStore;
import com.icon.agent.config.AgentConfig;
import com.icon.agent.config.ConfigManager;
import com.icon.agent.config.TargetConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Creates and manages all TargetContexts.
 * Failure in one target does not affect others.
 */
public class TargetManager {

    private static final Logger log = LoggerFactory.getLogger(TargetManager.class);

    private final List<TargetContext> contexts = new ArrayList<>();

    public TargetManager(AgentConfig config) {
        this(config, null);
    }

    // [2026-04-21] targets.json을 단일 저장소로 통일
    //              targets.json 우선 로드 → config.yaml 전용 항목은 targets.json으로 마이그레이션
    public TargetManager(AgentConfig config, ConfigManager configManager) {
        // 1) targets.json 로드 (주 저장소)
        List<TargetConfig> stored = TargetStore.load();
        Map<String, TargetConfig> byId = new LinkedHashMap<>();
        stored.forEach(tc -> byId.put(tc.getId(), tc));

        // 2) config.yaml에만 있는 target → targets.json으로 마이그레이션
        boolean migrated = false;
        for (TargetConfig tc : config.getTargets()) {
            if (!byId.containsKey(tc.getId())) {
                byId.put(tc.getId(), tc);
                migrated = true;
                log.info("config.yaml target '{}' → targets.json 마이그레이션", tc.getId());
            }
        }
        List<TargetConfig> merged = new ArrayList<>(byId.values());
        if (migrated) {
            TargetStore.save(merged);
        }

        // 3) 모든 target에 storeSync 적용 — CONFIG_UPDATE 수신 시 targets.json 갱신
        Runnable storeSync = () -> TargetStore.save(getManagedTargetConfigs());
        for (TargetConfig tc : merged) {
            contexts.add(new TargetContext(tc, configManager, storeSync));
        }
    }

    public void startAll() {
        log.info("Starting {} target(s)", contexts.size());
        for (TargetContext ctx : contexts) {
            try {
                ctx.start();
            } catch (Exception e) {
                log.error("Failed to start target '{}': {}", ctx.getTargetId(), e.getMessage(), e);
            }
        }
    }

    public void stopAll() {
        log.info("Stopping all targets");
        for (TargetContext ctx : contexts) {
            try {
                ctx.stop();
            } catch (Exception e) {
                log.error("Error stopping target '{}': {}", ctx.getTargetId(), e.getMessage(), e);
            }
        }
    }

    public List<TargetContext> getContexts() {
        return Collections.unmodifiableList(contexts);
    }

    // [2026-04-21] CLI로 추가된 target 목록 (TargetStore 저장용)
    public List<TargetConfig> getManagedTargetConfigs() {
        return contexts.stream()
                .map(TargetContext::getConfig)
                .collect(Collectors.toList());
    }

    /**
     * [2026-04-21] 런타임 target 추가 — CLI target add 명령에서 호출.
     * storeSync: CONFIG_UPDATE 수신 시 targets.json 재저장 콜백 (nullable)
     * @return "ok" | "already-exists"
     */
    public synchronized String addTarget(TargetConfig config, ConfigManager configManager, Runnable storeSync) {
        boolean exists = contexts.stream()
                .anyMatch(c -> c.getTargetId().equals(config.getId()));
        if (exists) return "already-exists";

        TargetContext ctx = new TargetContext(config, configManager, storeSync);
        try {
            ctx.start();
            contexts.add(ctx);
            log.info("Target 동적 추가: {}", config.getId());
        } catch (Exception e) {
            log.error("Target 추가 실패 '{}': {}", config.getId(), e.getMessage(), e);
        }
        return "ok";
    }

    /**
     * [2026-04-21] 런타임 target 제거 — CLI target remove 명령에서 호출.
     * @return 제거 성공 여부
     */
    public synchronized boolean removeTarget(String id) {
        TargetContext ctx = contexts.stream()
                .filter(c -> c.getTargetId().equals(id))
                .findFirst().orElse(null);
        if (ctx == null) return false;

        try {
            ctx.stop();
        } catch (Exception e) {
            log.warn("Target 중지 중 오류 '{}': {}", id, e.getMessage());
        }
        contexts.remove(ctx);
        log.info("Target 동적 제거: {}", id);
        return true;
    }
}

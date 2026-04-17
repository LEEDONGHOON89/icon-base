package com.icon.agent.target;

import com.icon.agent.config.AgentConfig;
import com.icon.agent.config.ConfigManager;
import com.icon.agent.config.TargetConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public TargetManager(AgentConfig config, ConfigManager configManager) {
        for (TargetConfig tc : config.getTargets()) {
            contexts.add(new TargetContext(tc, configManager));
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
}

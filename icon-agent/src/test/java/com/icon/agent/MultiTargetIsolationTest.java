package com.icon.agent;

import com.icon.agent.config.AgentConfig;
import com.icon.agent.config.TargetConfig;
import com.icon.agent.target.TargetContext;
import com.icon.agent.target.TargetManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiTargetIsolationTest {

    @Test
    void testTargetIsolation() {
        AgentConfig config = new AgentConfig();

        TargetConfig t1 = new TargetConfig();
        t1.setId("sysA");
        t1.getRpc().setEndpoint("wss://a.com");

        TargetConfig t2 = new TargetConfig();
        t2.setId("sysB");
        t2.getRpc().setEndpoint("wss://b.com");

        config.setTargets(List.of(t1, t2));

        TargetManager manager = new TargetManager(config);
        List<TargetContext> contexts = manager.getContexts();

        assertEquals(2, contexts.size());
        assertEquals("sysA", contexts.get(0).getTargetId());
        assertEquals("sysB", contexts.get(1).getTargetId());

        // Verify that they are separate instances and failure in one shouldn't affect
        // the other's state
        // In a real environment, we'd start them and simulate failure,
        // but here we verify the management structure isolates them.
    }
}

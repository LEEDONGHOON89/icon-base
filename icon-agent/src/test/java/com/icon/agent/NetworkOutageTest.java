package com.icon.agent;

import com.icon.agent.batch.Batch;
import com.icon.agent.collector.Record;
import com.icon.agent.config.RpcConfig;
import com.icon.agent.rpc.RpcClient;
import com.icon.agent.spool.SpoolManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NetworkOutageTest {

    @TempDir
    Path tempDir;

    @Test
    void testSpoolingOnNetworkOutage() throws Exception {
        String targetId = "sysA";
        RpcConfig rpcConfig = new RpcConfig();
        rpcConfig.setEndpoint("wss://localhost/ws");

        SSLContext ssl = SSLContext.getDefault();

        // Real SpoolManager pointing to temp dir
        SpoolManager spoolManager = new SpoolManager(tempDir.toString(), targetId);

        // RpcClient is what we are testing (interaction with Spooling)
        RpcClient rpcClient = new RpcClient(targetId, rpcConfig, ssl, spoolManager);

        // Mock connection as down
        // RpcClient.send() checks connected.get() which is false by default

        Batch batch = new Batch(targetId, List.of(
                new Record(targetId, Record.Source.FILE, "log", "data1", Map.of())));

        // Try to send while disconnected
        boolean success = rpcClient.send(batch);

        assertFalse(success, "Send should fail when disconnected");
        assertEquals(1, spoolManager.depth(), "Batch should be spooled when disconnected");

        // Verify spool entry exists on disk
        var pending = spoolManager.listPending();
        assertEquals(1, pending.size());
        Batch readBatch = spoolManager.read(pending.get(0));
        assertEquals(batch.getBatchId(), readBatch.getBatchId());
    }
}

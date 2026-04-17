package com.icon.agent.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icon.agent.target.TargetContext;
import com.icon.agent.target.TargetManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight HTTP server to expose agent health and per-target metrics.
 * Built using JDK's com.sun.net.httpserver to avoid external dependencies.
 */
public class HealthServer {

    private static final Logger log = LoggerFactory.getLogger(HealthServer.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final int port;
    private final TargetManager targetManager;
    private HttpServer server;

    public HealthServer(int port, TargetManager targetManager) {
        this.port = port;
        this.targetManager = targetManager;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", new HealthHandler());
        server.setExecutor(null); // use default executor
        server.start();
        log.info("Health server started on port {}", port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            log.info("Health server stopped");
        }
    }

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "UP");
            response.put("timestamp", System.currentTimeMillis());

            Map<String, Object> targets = new LinkedHashMap<>();
            for (TargetContext ctx : targetManager.getContexts()) {
                Map<String, Object> targetStats = new LinkedHashMap<>();
                targetStats.put("rpcConnected", ctx.isRpcConnected());
                targetStats.put("queueDepth", ctx.getQueueDepth());
                targetStats.put("spoolDepth", ctx.getSpoolDepth());
                targets.put(ctx.getTargetId(), targetStats);
            }
            response.put("targets", targets);

            byte[] jsonBytes = JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(response);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jsonBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jsonBytes);
            }
        }
    }
}

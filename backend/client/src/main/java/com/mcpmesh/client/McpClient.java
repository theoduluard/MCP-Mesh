package com.mcpmesh.client;

import com.mcpmesh.client.requests.CallToolRequest;
import com.mcpmesh.client.requests.InitRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class McpClient {

    private static final Logger log = LoggerFactory.getLogger(McpClient.class);
    private static final String PROTOCOL_VERSION = "2024-11-05";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private Process process;
    private BufferedWriter stdin;
    private BufferedReader stdout;
    private BufferedReader stderr;
    private final Object writeLock = new Object();
    private final AtomicInteger requestId = new AtomicInteger(0);
    private final Map<Integer, CompletableFuture<JsonNode>> pendingRequest = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public void connect(List<String> command) throws IOException {
        process = new ProcessBuilder(command)
                .redirectErrorStream(false)
                .start();
        stdin = process.outputWriter();
        stdout = process.inputReader();
        stderr = process.errorReader();

        Thread reader = new Thread(new StdoutReader(stdout, mapper, pendingRequest));
        reader.setName("mcp-client-reader");
        reader.setDaemon(true);
        reader.start();

        Thread stderrDrain = new Thread(this::drainStderr);
        stderrDrain.setName("mcp-client-stderr");
        stderrDrain.setDaemon(true);
        stderrDrain.start();
    }

    private void drainStderr() {
        try {
            String line;
            while ((line = stderr.readLine()) != null) {
                log.warn("[mcp-server] {}", line);
            }
        } catch (IOException e) {
            // flux is closed when server process stopped
        }
    }

    public JsonNode initialize() throws Exception {
        InitRequest initRequest = new InitRequest(
                PROTOCOL_VERSION,
                JsonNodeFactory.instance.objectNode(),
                new InitRequest.ClientInfo("mcp-mesh-client", "0.0.1")
        );
        JsonNode result = sendRequest("initialize", initRequest);
        sendInitializedNotification();
        return result;
    }

    private void sendInitializedNotification() throws IOException {
        ObjectNode notification = JsonNodeFactory.instance.objectNode();
        notification.put("jsonrpc", "2.0");
        notification.put("method", "notifications/initialized");
        writeMessage(notification);
    }

    public JsonNode listTools() throws Exception {
        return sendRequest("tools/list", null);
    }

    public JsonNode callTool(String name, Map<String, Object> arguments) throws Exception {
        return sendRequest("tools/call", new CallToolRequest(name, arguments));
    }

    private JsonNode sendRequest(String method, Object params) throws Exception {
        int id = requestId.incrementAndGet();
        ObjectNode request = JsonNodeFactory.instance.objectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        if (params != null) {
            JsonNode paramsNode = params instanceof JsonNode node ? node : mapper.valueToTree(params);
            request.set("params", paramsNode);
        }

        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingRequest.put(id, future);
        try {
            writeMessage(request);
            return future.get(REQUEST_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof JsonRpcException jsonRpcException) {
                throw jsonRpcException;
            }
            throw e;
        } finally {
            pendingRequest.remove(id);
        }
    }

    private void writeMessage(ObjectNode message) throws IOException {
        String json = mapper.writeValueAsString(message);
        synchronized (writeLock) {
            stdin.write(json);
            stdin.write("\n");
            stdin.flush();
        }
    }

    public void close() throws IOException {
        IOException closedException = new IOException("Client MCP fermé");
        pendingRequest.values().forEach(f -> f.completeExceptionally(closedException));
        pendingRequest.clear();
        stdin.close();
        stdout.close();
        stderr.close();
        if (process != null) {
            process.destroy();
        }
    }
}

package com.mcpmesh.client;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class McpClientTest {

    private McpClient client;

    @BeforeEach
    public void setUp() throws IOException {
        String javaBin = System.getProperty("java.home") + "/bin/java";
        String classpath = System.getProperty("java.class.path");
        List<String> command = List.of(javaBin, "-cp", classpath, "com.mcpmesh.client.FakeMcpServer");

        client = new McpClient();
        client.connect(command);
    }

    @Test
    public void initializeTest() throws Exception {
        JsonNode initNode = client.initialize();

        assertEquals("2024-11-05", initNode.get("protocolVersion").asString());
        assertEquals("fake-mcp-server", initNode.get("serverInfo").get("name").asString());
        assertEquals("0.0.1", initNode.get("serverInfo").get("version").asString());
    }

    @Test
    public void listToolsTest() throws Exception {
        client.initialize();
        JsonNode tools = client.listTools();

        assertEquals(1, tools.get("tools").asArray().size());
    }

    @Test
    public void callToolTest() throws Exception {
        client.initialize();
        String toolName = client.listTools()
                .get("tools")
                .get(0)
                .get("name")
                .asString();
        JsonNode toolResp = client.callTool(toolName, Map.of("message", "World"));

        assertEquals("World", toolResp.get("content").get(0).get("text").asString());
    }

    @Test
    public void callUnknownToolTest() throws Exception {
        client.initialize();

        JsonRpcException exception = assertThrows(JsonRpcException.class,
                () -> client.callTool("does-not-exist", Map.of()));

        assertEquals(-32602, exception.getCode());
        assertEquals("Unknown tool: does-not-exist", exception.getMessage());
    }

    @Test
    public void concurrentCallToolTest() throws Exception {
        client.initialize();

        CompletableFuture<JsonNode> slowCall = CompletableFuture.supplyAsync(() -> callToolUnchecked("slow"));
        CompletableFuture<JsonNode> fastCall = CompletableFuture.supplyAsync(() -> callToolUnchecked("fast"));

        JsonNode slowResult = slowCall.get(5, TimeUnit.SECONDS);
        JsonNode fastResult = fastCall.get(5, TimeUnit.SECONDS);

        assertEquals("slow", slowResult.get("content").get(0).get("text").asString());
        assertEquals("fast", fastResult.get("content").get(0).get("text").asString());
    }

    @Test
    public void closeCancelsPendingRequestsTest() throws Exception {
        client.initialize();

        CompletableFuture<JsonNode> pendingCall = CompletableFuture.supplyAsync(() -> callToolUnchecked("slow"));

        Awaitility.await().during(100, TimeUnit.MILLISECONDS);
        client.close();

        // si close() n'annule pas activement la requête en attente, ce get() atteint son
        // timeout AVANT le timeout interne de 10s de McpClient -> TimeoutException, pas ExecutionException
        assertThrows(ExecutionException.class, () -> pendingCall.get(2, TimeUnit.SECONDS));
    }

    private JsonNode callToolUnchecked(String message) {
        try {
            return client.callTool("echo", Map.of("message", message));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    public void tearDown() throws IOException {
        client.close();
    }
}

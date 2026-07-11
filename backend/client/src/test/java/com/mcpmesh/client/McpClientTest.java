package com.mcpmesh.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    @AfterEach
    public void tearDown() throws IOException {
        client.close();
    }
}

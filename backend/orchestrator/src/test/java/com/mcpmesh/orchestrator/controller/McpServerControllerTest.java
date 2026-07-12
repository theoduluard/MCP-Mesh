package com.mcpmesh.orchestrator.controller;

import com.mcpmesh.client.FakeMcpServer;
import com.mcpmesh.orchestrator.dto.ConnectServerRequest;
import com.mcpmesh.orchestrator.dto.ConnectServerResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class McpServerControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void connectTest() {
        List<String> command = List.of(
                "java", "-cp", System.getProperty("java.class.path"), FakeMcpServer.class.getName()
        );
        ConnectServerRequest request = new ConnectServerRequest(command);

        ConnectServerResponse response = restTemplate.postForObject("/servers/connect", request, ConnectServerResponse.class);

        assertEquals("2024-11-05", response.result().get("protocolVersion").asString());
        assertEquals("fake-mcp-server", response.result().get("serverInfo").get("name").asString());
    }
}

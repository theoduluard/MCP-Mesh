package com.mcpmesh.orchestrator.controller;

import com.mcpmesh.client.FakeMcpServer;
import com.mcpmesh.orchestrator.dto.*;
import com.mcpmesh.orchestrator.exception.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class McpServerControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private ConnectServerResponse connectToFakeServer() {
        List<String> command = List.of(
                "java", "-cp", System.getProperty("java.class.path"), FakeMcpServer.class.getName()
        );
        ConnectServerRequest request = new ConnectServerRequest(command);

        return restTemplate.postForObject("/servers/connect", request, ConnectServerResponse.class);
    }

    @Test
    void connectTest() {
       ConnectServerResponse response = connectToFakeServer();

        assertNotNull(response);
        assertNotNull(response.serverId());
        assertDoesNotThrow(() -> UUID.fromString(response.serverId()));
    }

    @Test
    void listToolsTest() {
        String serverId = connectToFakeServer().serverId();

        ListToolsResponse response = restTemplate.getForObject("/servers/" + serverId + "/tools", ListToolsResponse.class);

        assertNotNull(response);
        assertEquals(1, response.result().get("tools").asArray().size());
        assertEquals("echo", response.result().get("tools").get(0).get("name").asString());
    }

    @Test
    void callToolTest() {
        String serverId = connectToFakeServer().serverId();
        CallToolHttpRequest request = new CallToolHttpRequest(Map.of("message", "World"));

        CallToolHttpResponse response = restTemplate.postForObject(
                "/servers/" + serverId + "/tools/echo/call", request, CallToolHttpResponse.class);

        assertNotNull(response);
        assertEquals("World", response.result().get("content").get(0).get("text").asString());
    }

    @Test
    void listToolsWithUnknownServerIdTest() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity("/servers/unknown-id/tools", ErrorResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void disconnectTest() {
        String serverId = connectToFakeServer().serverId();

        ResponseEntity<Void> response = restTemplate.exchange("/servers/" + serverId, HttpMethod.DELETE, null, Void.class);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        ResponseEntity<ErrorResponse> afterDisconnect = restTemplate.getForEntity("/servers/" + serverId + "/tools", ErrorResponse.class);
        assertEquals(HttpStatus.NOT_FOUND, afterDisconnect.getStatusCode());
    }
}

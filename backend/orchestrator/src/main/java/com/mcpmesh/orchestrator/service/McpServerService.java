package com.mcpmesh.orchestrator.service;

import com.mcpmesh.client.McpClient;
import com.mcpmesh.orchestrator.dto.ConnectServerRequest;
import com.mcpmesh.orchestrator.dto.CallToolHttpRequest;
import com.mcpmesh.orchestrator.exception.ServerNotFoundException;
import com.mcpmesh.orchestrator.util.McpServerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.Set;
import java.util.UUID;

@Service
public class McpServerService {

    private static final Logger log = LoggerFactory.getLogger(McpServerService.class);
    private final McpServerRegistry registry;

    public McpServerService(McpServerRegistry registry) {
        this.registry = registry;
    }

    public String connect(ConnectServerRequest conRequest) throws Exception {
        McpClient client = new McpClient();
        try {
            client.connect(conRequest.command());
            client.initialize();
        } catch (Exception e){
            log.error("Error while connecting to mcp client: {}", e.getMessage());
            client.close();
            throw e;
        }

        String id = UUID.randomUUID().toString();
        registry.addClient(id, client);
        return id;
    }

    public JsonNode listTools(String serverId) throws Exception {
        McpClient client = registry.getClient(serverId);
        if (client == null) {
            throw new ServerNotFoundException(serverId);
        }
        return client.listTools();
    }

    public JsonNode callTool(String serverId, String toolName, CallToolHttpRequest toolRequest) throws Exception {
        McpClient client = registry.getClient(serverId);
        if (client == null) {
            throw new ServerNotFoundException(serverId);
        }
        return client.callTool(toolName, toolRequest.arguments());
    }

    public void disconnect(String serverId) {
        McpClient client = registry.getClient(serverId);
        if (client == null) {
            throw new ServerNotFoundException(serverId);
        }
        client.close();
        registry.removeClient(serverId);
    }

    public Set<String> listServers() {
        return registry.getAllServerIds();
    }
}

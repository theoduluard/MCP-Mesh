package com.mcpmesh.orchestrator.util;

import com.mcpmesh.client.McpClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class McpServerRegistry {

    private final Map<String, McpClient> mcpClients = new ConcurrentHashMap<>();

    public void addClient(String id, McpClient client) {
        mcpClients.putIfAbsent(id, client);
    }

    public void removeClient(String id) {
        mcpClients.remove(id);
    }

    public McpClient getClient(String id) {
        return mcpClients.get(id);
    }

}

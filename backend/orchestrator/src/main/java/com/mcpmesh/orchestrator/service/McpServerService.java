package com.mcpmesh.orchestrator.service;

import com.mcpmesh.client.McpClient;
import com.mcpmesh.orchestrator.dto.ConnectServerRequest;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
public class McpServerService {

    public JsonNode connect(ConnectServerRequest conRequest) throws Exception {
        McpClient client = new McpClient();
        JsonNode init;
        try {
            client.connect(conRequest.command());
            init = client.initialize();
        } finally {
            client.close();
        }
        return init;
    }

}

package com.mcpmesh.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class FakeMcpServer {

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(System.out, true);

        String line;
        while ((line = in.readLine()) != null) {
            JsonNode request = mapper.readTree(line);
            JsonNode idNode = request.get("id");

            if (idNode == null) {
                continue;
            }

            String method = request.get("method").asString();
            Map<String, JsonNode> params = extractParams(request.get("params"));

            new Thread(() -> handleRequest(idNode, method, params, mapper, out)).start();
        }
    }

    private static void handleRequest(JsonNode idNode, String method, Map<String, JsonNode> params,
                                        ObjectMapper mapper, PrintWriter out) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", idNode);

        try {
            JsonNode result = getResultForMethod(method, params, mapper);
            response.set("result", result);
        } catch (ToolNotFoundException e) {
            ObjectNode error = mapper.createObjectNode();
            error.put("code", e.getCode());
            error.put("message", e.getMessage());
            response.set("error", error);
        }

        out.println(mapper.writeValueAsString(response));
    }

    private static JsonNode getResultForMethod(String method, Map<String, JsonNode> params, ObjectMapper mapper) {
        ObjectNode result = mapper.createObjectNode();

        if ("initialize".equals(method)) {
            result.put("protocolVersion", "2024-11-05");
            result.set("capabilities", mapper.createObjectNode());
            ObjectNode serverInfo = mapper.createObjectNode();
            serverInfo.put("name", "fake-mcp-server");
            serverInfo.put("version", "0.0.1");
            result.set("serverInfo", serverInfo);
        }

        else if ("tools/list".equals(method)) {
            ObjectNode tool = mapper.createObjectNode();
            tool.put("name", "echo");
            tool.put("title", "Echo Tool");
            tool.put("description", "Echoes back the input string");

            ObjectNode inputSchema = mapper.createObjectNode();
            inputSchema.put("$schema", "http://json-schema.org/draft-07/schema#");
            inputSchema.put("type", "object");
            ObjectNode properties = mapper.createObjectNode();
            ObjectNode message = mapper.createObjectNode();
            message.put("type", "string");
            message.put("description", "Message to echo");
            properties.set("message", message);
            inputSchema.set("properties", properties);
            ArrayNode required = mapper.createArrayNode();
            required.add("message");
            inputSchema.set("required", required);
            tool.set("inputSchema", inputSchema);

            ArrayNode tools = mapper.createArrayNode();
            tools.add(tool);
            result.set("tools", tools);
        }

        else if ("tools/call".equals(method)) {
            String toolName = params.get("name").asString();
            if (!"echo".equals(toolName)) {
                throw new ToolNotFoundException(toolName);
            }

            JsonNode arguments = params.get("arguments");
            String message = arguments.get("message").asString();

            if ("slow".equals(message)) {
                try {
                    Thread.sleep(300); // simulate a slow server-side process
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            ObjectNode textContent = mapper.createObjectNode();
            textContent.put("type", "text");
            textContent.put("text", message);

            ArrayNode content = mapper.createArrayNode();
            content.add(textContent);

            result.set("content", content);
            result.put("isError", false);
        }

        return result;
    }

    private static Map<String, JsonNode> extractParams(JsonNode paramsNode) {
        Map<String, JsonNode> params = new HashMap<>();
        if (paramsNode == null) {
            return params;
        }
        for (Map.Entry<String, JsonNode> entry : paramsNode.properties()) {
            params.put(entry.getKey(), entry.getValue());
        }
        return params;
    }

    private static class ToolNotFoundException extends RuntimeException {

        ToolNotFoundException(String toolName) {
            super("Unknown tool: " + toolName);
        }

        int getCode() {
            return -32602;
        }
    }
}

package com.mcpmesh.client;

import tools.jackson.databind.JsonNode;

public class JsonRpcException extends RuntimeException {

    private final int code;
    private final JsonNode data;

    public JsonRpcException(int code, String message, JsonNode data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public static JsonRpcException fromNode(JsonNode errorNode) {
        int code = errorNode.path("code").asInt();
        String message = errorNode.path("message").asString("Erreur JSON-RPC inconnue");
        JsonNode data = errorNode.get("data");
        return new JsonRpcException(code, message, data);
    }

    public int getCode() {
        return code;
    }

    public JsonNode getData() {
        return data;
    }
}

package com.mcpmesh.client.requests;

import tools.jackson.databind.JsonNode;

public record InitRequest(String protocolVersion, JsonNode capabilities, ClientInfo clientInfo) {

    public record ClientInfo(String name, String version) {
    }
}

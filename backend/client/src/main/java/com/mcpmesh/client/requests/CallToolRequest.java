package com.mcpmesh.client.requests;

import java.util.Map;

public record CallToolRequest(String name, Map<String, Object> arguments) {
}

package com.mcpmesh.orchestrator.exception;

public class ServerNotFoundException extends RuntimeException {
    public ServerNotFoundException(String serverId) {
        super("Unknown server: " + serverId);
    }
}

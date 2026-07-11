package com.mcpmesh.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class StdoutReader implements Runnable {

    private final BufferedReader stdout;
    private final ObjectMapper mapper;
    private final Map<Integer, CompletableFuture<JsonNode>> pendingRequest;

    public StdoutReader(BufferedReader stdout, ObjectMapper mapper, Map<Integer, CompletableFuture<JsonNode>> pendingRequest) {
        this.stdout = stdout;
        this.mapper = mapper;
        this.pendingRequest = pendingRequest;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = stdout.readLine()) != null) {
                if (!line.isBlank()) {
                    dispatch(line);
                }
            }
        } catch (IOException e) {
            // flux fermé, le process serveur s'est probablement arrêté
        } finally {
            failAllPending();
        }
    }

    private void dispatch(String line) {
        JsonNode message;
        try {
            message = mapper.readTree(line);
        } catch (Exception e) {
            return; // ligne non-JSON parasite, on l'ignore
        }

        JsonNode idNode = message.get("id");
        if (idNode == null || idNode.isNull()) {
            return; // notification serveur, pas encore gérée
        }

        CompletableFuture<JsonNode> future = pendingRequest.get(idNode.asInt());
        if (future == null) {
            return; // réponse à une requête inconnue ou déjà expirée
        }

        JsonNode error = message.get("error");
        if (error != null) {
            future.completeExceptionally(JsonRpcException.fromNode(error));
        } else {
            future.complete(message.get("result"));
        }
    }

    private void failAllPending() {
        IOException cause = new IOException("Flux stdout du serveur MCP fermé");
        pendingRequest.values().forEach(f -> f.completeExceptionally(cause));
        pendingRequest.clear();
    }
}

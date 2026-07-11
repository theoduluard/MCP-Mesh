package com.mcpmesh.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class StdoutReaderTest {

    private ObjectMapper mapper;
    private ConcurrentHashMap<Integer, CompletableFuture<JsonNode>> pendingRequest;
    private CompletableFuture<JsonNode> future;

    @BeforeEach
    public void setUp() {
        mapper = new ObjectMapper();
        pendingRequest = new ConcurrentHashMap<>();
        future = new CompletableFuture<>();
        pendingRequest.put(1, future);
    }

    @Test
    public void onSuccessTest() throws Exception {
        BufferedReader readerBuffer = new BufferedReader(
                new StringReader("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"foo\":\"bar\"}}\n")
        );
        StdoutReader reader = new StdoutReader(readerBuffer, mapper, pendingRequest);

        reader.run();

        JsonNode result = future.get();
        assertEquals("bar", result.get("foo").asString());
    }

    @Test
    public void onFailureTest() {
        BufferedReader readerBuffer = new BufferedReader(
                new StringReader("{\"jsonrpc\":\"2.0\",\"id\":1,\"error\":{\"code\":-32601,\"message\":\"Method not found\",\"data\":{\"field\":\"message\"}}}\n")
        );
        StdoutReader reader = new StdoutReader(readerBuffer, mapper, pendingRequest);

        reader.run();

        ExecutionException executionException = assertThrows(ExecutionException.class, future::get);
        JsonRpcException jsonRpcException = assertInstanceOf(JsonRpcException.class, executionException.getCause());
        assertEquals(-32601, jsonRpcException.getCode());
        assertEquals("Method not found", jsonRpcException.getMessage());
        assertEquals("message", jsonRpcException.getData().get("field").asString());
    }

    @Test
    public void onUnknownIdTest() throws Exception {
        BufferedReader readerBuffer = new BufferedReader(
                new StringReader(
                        """
                        {"jsonrpc":"2.0","id":99,"result":{"foo":"unknown"}}
                        {"jsonrpc":"2.0","id":1,"result":{"foo":"bar"}}
                        """
                )
        );
        StdoutReader reader = new StdoutReader(readerBuffer, mapper, pendingRequest);

        reader.run();

        JsonNode result = future.get();
        assertEquals("bar", result.get("foo").asString());
    }

    @Test
    public void onNotificationTest() throws Exception {
        BufferedReader readerBuffer = new BufferedReader(
                new StringReader(
                        """
                        {"jsonrpc":"2.0","method":"notifications/something"}
                        {"jsonrpc":"2.0","id":1,"result":{"foo":"bar"}}
                        """
                )
        );
        StdoutReader reader = new StdoutReader(readerBuffer, mapper, pendingRequest);

        reader.run();

        JsonNode result = future.get();
        assertEquals("bar", result.get("foo").asString());
    }
}

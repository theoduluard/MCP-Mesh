package com.mcpmesh.orchestrator.controller;

import com.mcpmesh.orchestrator.dto.*;
import com.mcpmesh.orchestrator.service.McpServerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/servers")
public class McpServerController {

    private final McpServerService service;

    public McpServerController(McpServerService service) {
        this.service = service;
    }

    @PostMapping("/connect")
    public ConnectServerResponse connect(@RequestBody ConnectServerRequest conRequest) throws Exception {
        return new ConnectServerResponse(service.connect(conRequest));
    }

    @GetMapping("/{serverId}/tools")
    public ListToolsResponse listTools(@PathVariable String serverId) throws Exception {
        return new ListToolsResponse(service.listTools(serverId));
    }

    @PostMapping("/{serverId}/tools/{toolName}/call")
    public CallToolHttpResponse callTool(@PathVariable String serverId, @PathVariable String toolName,
                                         @RequestBody CallToolHttpRequest request) throws Exception {
        return new CallToolHttpResponse(service.callTool(serverId, toolName, request));
    }

    @DeleteMapping("/{serverId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnect(@PathVariable String serverId) {
        service.disconnect(serverId);
    }
}

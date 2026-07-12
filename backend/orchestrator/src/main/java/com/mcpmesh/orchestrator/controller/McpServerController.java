package com.mcpmesh.orchestrator.controller;

import com.mcpmesh.orchestrator.dto.ConnectServerRequest;
import com.mcpmesh.orchestrator.dto.ConnectServerResponse;
import com.mcpmesh.orchestrator.service.McpServerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

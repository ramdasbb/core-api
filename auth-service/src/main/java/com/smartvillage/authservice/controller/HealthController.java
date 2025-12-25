package com.smartvillage.authservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(
    name = "Health",
    description = "Health check and status endpoints"
)
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*", maxAge = 3600)
public class HealthController {

    /**
     * GET /health - Health check endpoint (no authentication required)
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Service health status endpoint")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "auth-service");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /status - Status check endpoint (no authentication required)
     */
    @GetMapping("/status")
    @Operation(summary = "Status check", description = "Service status endpoint")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "RUNNING");
        response.put("service", "auth-service");
        response.put("version", "1.0.0");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}

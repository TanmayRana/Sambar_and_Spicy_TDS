package com.sambarspicy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping
public class HealthCheckController {

    @GetMapping({"/health", "/api/v1/health"})
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "sambar-spicy-backend",
            "timestamp", Instant.now().toString(),
            "message", "Sambar & Spicy Service is healthy and operating normally."
        ));
    }
}

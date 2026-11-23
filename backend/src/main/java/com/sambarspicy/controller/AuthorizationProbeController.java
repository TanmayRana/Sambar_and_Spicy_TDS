package com.sambarspicy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller providing probe endpoints to verify the Role-Based Permission Matrix:
 * ADMIN, BILLING, READ_ONLY access rules.
 */
@RestController
@RequestMapping("/api")
public class AuthorizationProbeController {

    @GetMapping("/admin/probe")
    public ResponseEntity<Map<String, String>> adminGetProbe() {
        return ResponseEntity.ok(Map.of("module", "admin", "action", "GET", "status", "authorized"));
    }

    @GetMapping("/billing/probe")
    public ResponseEntity<Map<String, String>> billingGetProbe() {
        return ResponseEntity.ok(Map.of("module", "billing", "action", "GET", "status", "authorized"));
    }

    @PostMapping("/billing/probe")
    public ResponseEntity<Map<String, String>> billingPostProbe() {
        return ResponseEntity.ok(Map.of("module", "billing", "action", "POST", "status", "authorized"));
    }

    @GetMapping("/inventory/probe")
    public ResponseEntity<Map<String, String>> inventoryGetProbe() {
        return ResponseEntity.ok(Map.of("module", "inventory", "action", "GET", "status", "authorized"));
    }

    @PostMapping("/inventory/probe")
    public ResponseEntity<Map<String, String>> inventoryPostProbe() {
        return ResponseEntity.ok(Map.of("module", "inventory", "action", "POST", "status", "authorized"));
    }

    @GetMapping("/reports/probe")
    public ResponseEntity<Map<String, String>> reportsGetProbe() {
        return ResponseEntity.ok(Map.of("module", "reports", "action", "GET", "status", "authorized"));
    }

    @PostMapping("/reports/probe")
    public ResponseEntity<Map<String, String>> reportsPostProbe() {
        return ResponseEntity.ok(Map.of("module", "reports", "action", "POST", "status", "authorized"));
    }

    @GetMapping("/menu/probe")
    public ResponseEntity<Map<String, String>> menuGetProbe() {
        return ResponseEntity.ok(Map.of("module", "menu", "action", "GET", "status", "authorized"));
    }

    @PostMapping("/menu/probe")
    public ResponseEntity<Map<String, String>> menuPostProbe() {
        return ResponseEntity.ok(Map.of("module", "menu", "action", "POST", "status", "authorized"));
    }
}

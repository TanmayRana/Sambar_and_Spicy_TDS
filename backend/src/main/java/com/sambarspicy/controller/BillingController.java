package com.sambarspicy.controller;

import com.sambarspicy.dto.BillingDtos.*;
import com.sambarspicy.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/bills", "/api/v1/bills"})
public class BillingController {
    private final BillingService service;
    public BillingController(BillingService service) { this.service = service; }

    @GetMapping("/capabilities")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING','READ_ONLY')")
    public Map<String, Boolean> capabilities() { return Map.of("round_off", true); }

    @GetMapping("/held")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING','READ_ONLY')")
    public List<Detail> held() { return service.held(); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING','READ_ONLY')")
    public Detail get(@PathVariable Long id) { return service.get(id); }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BILLING','READ_ONLY')")
    public List<Detail> list(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.list(from, to);
    }

    @PostMapping("/hold")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING')")
    public Detail hold(@Valid @RequestBody BillRequest request, Principal user) {
        return service.hold(request, user.getName());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING')")
    public Detail cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest request, Principal user) {
        return service.cancel(id, request.reason(), user.getName());
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING')")
    public Detail checkout(@Valid @RequestBody CheckoutRequest request,
                           @RequestHeader("Idempotency-Key") String key, Principal user) {
        return service.checkout(request, key, user.getName());
    }

    @PostMapping("/{id}/reprint")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING')")
    public ResponseEntity<Void> reprint(@PathVariable Long id, Principal user) {
        service.reprint(id, user.getName());
        return ResponseEntity.noContent().build();
    }
}

package com.sambarspicy.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sambarspicy.exception.ResourceNotFoundException;
import com.sambarspicy.model.AuditLog;
import com.sambarspicy.model.StoreSetting;
import com.sambarspicy.model.User;
import com.sambarspicy.repository.AuditLogRepository;
import com.sambarspicy.repository.StoreSettingRepository;
import com.sambarspicy.repository.UserRepository;
import com.sambarspicy.service.PublicMenuChanged;
import org.springframework.context.ApplicationEventPublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;

@RestController
public class StoreController {
    private final StoreSettingRepository stores;
    private final UserRepository users;
    private final AuditLogRepository audits;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher events;

    public StoreController(StoreSettingRepository stores, UserRepository users,
                           AuditLogRepository audits, ObjectMapper mapper, ApplicationEventPublisher events) {
        this.stores = stores;
        this.users = users;
        this.audits = audits;
        this.mapper = mapper;
        this.events = events;
    }

    public record StoreDto(
            Integer id,
            @JsonProperty("store_name") String storeName,
            String address,
            String phone,
            @JsonProperty("currency_code") String currencyCode,
            @JsonProperty("business_timezone") String businessTimezone,
            @JsonProperty("receipt_footer") String receiptFooter,
            @JsonProperty("updated_by") Integer updatedBy,
            @JsonProperty("updated_at") LocalDateTime updatedAt) {}

    public record StoreUpdate(
            @JsonProperty("store_name") @Size(max = 120) String storeName,
            @Size(max = 500) String address,
            @Size(max = 25) String phone,
            @JsonProperty("receipt_footer") @Size(max = 255) String receiptFooter) {}

    @GetMapping({"/api/store", "/api/v1/store"})
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING', 'READ_ONLY')")
    @Transactional(readOnly = true)
    public StoreDto getStore() {
        return toDto(findStore());
    }

    @PatchMapping({"/api/admin/store", "/api/v1/admin/store"})
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(rollbackFor = JsonProcessingException.class)
    public StoreDto updateStore(@Valid @RequestBody StoreUpdate input, Principal principal)
            throws JsonProcessingException {
        StoreSetting store = findStore();
        User actor = users.findByUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        StoreDto before = toDto(store);
        if (input.storeName() != null) {
            if (input.storeName().isBlank()) throw new IllegalArgumentException("Store name is required");
            store.setStoreName(input.storeName().trim());
        }
        if (input.address() != null) store.setAddress(input.address().trim());
        if (input.phone() != null) store.setPhone(input.phone().trim());
        if (input.receiptFooter() != null) {
            store.setReceiptFooter(input.receiptFooter().isBlank() ? null : input.receiptFooter().trim());
        }
        store.setUpdatedBy(actor);
        stores.saveAndFlush(store);
        StoreDto after = toDto(store);
        audits.save(new AuditLog(actor, "UPDATE", "store_settings", "1",
                mapper.writeValueAsString(before), mapper.writeValueAsString(after), null));
        events.publishEvent(new PublicMenuChanged());
        return after;
    }

    private StoreSetting findStore() {
        return stores.findById(1).orElseThrow(() -> new ResourceNotFoundException("Store settings not found"));
    }

    private StoreDto toDto(StoreSetting store) {
        return new StoreDto(store.getId(), store.getStoreName(), store.getAddress(), store.getPhone(),
                store.getCurrencyCode(), store.getBusinessTimezone(), store.getReceiptFooter(),
                store.getUpdatedBy() == null ? null : store.getUpdatedBy().getId(), store.getUpdatedAt());
    }
}

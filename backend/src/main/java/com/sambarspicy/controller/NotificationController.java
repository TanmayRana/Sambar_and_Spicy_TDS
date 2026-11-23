package com.sambarspicy.controller;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.sambarspicy.exception.ResourceNotFoundException;
import com.sambarspicy.model.Notification;
import com.sambarspicy.model.User;
import com.sambarspicy.repository.NotificationRepository;
import com.sambarspicy.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping({"/api/notifications", "/api/v1/notifications"})
public class NotificationController {
    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationController(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record NotificationDto(Long id, Long inventoryItemId, String severity, String status,
                                  String message, BigDecimal triggeredQty, LocalDateTime createdAt,
                                  LocalDateTime lastNotifiedAt, Integer readBy, LocalDateTime readAt,
                                  LocalDateTime resolvedAt, Long openInventoryItemId) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'READ_ONLY')")
    @Transactional(readOnly = true)
    public List<NotificationDto> list() {
        return notifications.findAll(Sort.by(Sort.Direction.DESC, "createdAt", "id"))
                .stream().map(this::toDto).toList();
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public NotificationDto markRead(@PathVariable Long id, Principal principal) {
        Notification notification = notifications.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (notification.getReadAt() == null) {
            User user = users.findByUsername(principal.getName())
                    .filter(User::getIsActive)
                    .orElseThrow(() -> new ResourceNotFoundException("Active user not found"));
            notification.setReadBy(user);
            notification.setReadAt(LocalDateTime.now(ZoneOffset.UTC));
            notifications.saveAndFlush(notification);
        }
        return toDto(notification);
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(notification.getId(), notification.getInventoryItem().getId(),
                notification.getSeverity().name(), notification.getStatus().name(), notification.getMessage(),
                notification.getTriggeredQty(), notification.getCreatedAt(), notification.getLastNotifiedAt(),
                notification.getReadBy() == null ? null : notification.getReadBy().getId(),
                notification.getReadAt(), notification.getResolvedAt(), notification.getOpenInventoryItemId());
    }
}

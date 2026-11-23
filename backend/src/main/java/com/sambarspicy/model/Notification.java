package com.sambarspicy.model;

import com.sambarspicy.enums.NotificationSeverity;
import com.sambarspicy.enums.NotificationStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Inventory stock alert notification (Low-Stock WARNING / Critical CRITICAL).
 * MySQL generated column open_inventory_item_id enforces at most one OPEN alert per stock item.
 */
@Entity
@Table(
    name = "notifications",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_one_open_stock_alert", columnNames = {"open_inventory_item_id"})
    },
    indexes = {
        @Index(name = "idx_notifications_status", columnList = "status, created_at"),
        @Index(name = "idx_notifications_inventory", columnList = "inventory_item_id, created_at")
    }
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 12)
    private NotificationSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private NotificationStatus status = NotificationStatus.OPEN;

    @Column(name = "message", nullable = false, length = 255)
    private String message;

    @Column(name = "triggered_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal triggeredQty;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_notified_at", nullable = false)
    private LocalDateTime lastNotifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "read_by")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private User readBy;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "open_inventory_item_id", insertable = false, updatable = false)
    private Long openInventoryItemId;

    public Notification() {
    }

    public Notification(InventoryItem inventoryItem, NotificationSeverity severity, String message, BigDecimal triggeredQty) {
        this.inventoryItem = inventoryItem;
        this.severity = severity;
        this.status = NotificationStatus.OPEN;
        this.message = message;
        this.triggeredQty = triggeredQty;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.lastNotifiedAt == null) {
            this.lastNotifiedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public NotificationSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(NotificationSeverity severity) {
        this.severity = severity;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigDecimal getTriggeredQty() {
        return triggeredQty;
    }

    public void setTriggeredQty(BigDecimal triggeredQty) {
        this.triggeredQty = triggeredQty;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastNotifiedAt() {
        return lastNotifiedAt;
    }

    public void setLastNotifiedAt(LocalDateTime lastNotifiedAt) {
        this.lastNotifiedAt = lastNotifiedAt;
    }

    public User getReadBy() {
        return readBy;
    }

    public void setReadBy(User readBy) {
        this.readBy = readBy;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Long getOpenInventoryItemId() {
        return openInventoryItemId;
    }

    public void setOpenInventoryItemId(Long openInventoryItemId) {
        this.openInventoryItemId = openInventoryItemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

package com.sambarspicy.model;

import com.sambarspicy.enums.InventoryUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Raw material / inventory master item.
 * Tracks stock balance, warning/critical thresholds, and weighted-average unit cost.
 */
@Entity
@Table(
    name = "inventory_items",
    indexes = {
        @Index(name = "idx_inventory_active_qty", columnList = "is_active, current_qty")
    }
)
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 150, unique = true)
    private String name;

    @Column(name = "sku", length = 60, unique = true)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 16)
    private InventoryUnit unit;

    @Column(name = "current_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal currentQty = BigDecimal.ZERO;

    @Column(name = "min_limit_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal minLimitQty = BigDecimal.ZERO;

    @Column(name = "critical_limit_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal criticalLimitQty = BigDecimal.ZERO;

    @Column(name = "avg_unit_cost", nullable = false, precision = 14, scale = 4)
    private BigDecimal avgUnitCost = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public InventoryItem() {
    }

    public InventoryItem(String name, InventoryUnit unit, BigDecimal minLimitQty, BigDecimal criticalLimitQty) {
        this.name = name;
        this.unit = unit;
        this.minLimitQty = minLimitQty;
        this.criticalLimitQty = criticalLimitQty;
        this.currentQty = BigDecimal.ZERO;
        this.avgUnitCost = BigDecimal.ZERO;
        this.isActive = true;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public InventoryUnit getUnit() {
        return unit;
    }

    public void setUnit(InventoryUnit unit) {
        this.unit = unit;
    }

    public BigDecimal getCurrentQty() {
        return currentQty;
    }

    public void setCurrentQty(BigDecimal currentQty) {
        this.currentQty = currentQty;
    }

    public BigDecimal getMinLimitQty() {
        return minLimitQty;
    }

    public void setMinLimitQty(BigDecimal minLimitQty) {
        this.minLimitQty = minLimitQty;
    }

    public BigDecimal getCriticalLimitQty() {
        return criticalLimitQty;
    }

    public void setCriticalLimitQty(BigDecimal criticalLimitQty) {
        this.criticalLimitQty = criticalLimitQty;
    }

    public BigDecimal getAvgUnitCost() {
        return avgUnitCost;
    }

    public void setAvgUnitCost(BigDecimal avgUnitCost) {
        this.avgUnitCost = avgUnitCost;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryItem that = (InventoryItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

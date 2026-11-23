package com.sambarspicy.model;

import com.sambarspicy.enums.StockMovementType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable append-only inventory ledger recording all physical stock additions and deductions.
 * Cost amount is a database-generated stored column.
 */
@Entity
@Table(
    name = "stock_movements",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_movement_purchase_line", columnNames = {"purchase_line_id"}),
        @UniqueConstraint(name = "uq_movement_daily_type", columnNames = {"daily_entry_id", "movement_type"})
    },
    indexes = {
        @Index(name = "idx_mov_item_time", columnList = "inventory_item_id, recorded_at"),
        @Index(name = "idx_mov_date_type", columnList = "business_date, movement_type")
    }
)
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private StockMovementType movementType;

    @Column(name = "quantity_delta", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityDelta;

    @Column(name = "cost_per_unit_snapshot", nullable = false, precision = 14, scale = 4)
    private BigDecimal costPerUnitSnapshot;

    @Column(name = "cost_amount", precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal costAmount;

    @Column(name = "balance_after", nullable = false, precision = 14, scale = 3)
    private BigDecimal balanceAfter;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_line_id", unique = true)
    private StockPurchaseLine purchaseLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_entry_id")
    private StockDailyEntry dailyEntry;

    @Column(name = "notes", length = 255)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by", nullable = false)
    @JdbcTypeCode(SqlTypes.TINYINT)
    private User recordedBy;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    public StockMovement() {
    }

    public StockMovement(InventoryItem inventoryItem, LocalDate businessDate, StockMovementType movementType,
                         BigDecimal quantityDelta, BigDecimal costPerUnitSnapshot, BigDecimal balanceAfter, User recordedBy) {
        this.inventoryItem = inventoryItem;
        this.businessDate = businessDate;
        this.movementType = movementType;
        this.quantityDelta = quantityDelta;
        this.costPerUnitSnapshot = costPerUnitSnapshot;
        this.balanceAfter = balanceAfter;
        this.recordedBy = recordedBy;
    }

    @PrePersist
    protected void onCreate() {
        if (this.recordedAt == null) {
            this.recordedAt = LocalDateTime.now();
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

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public StockMovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(StockMovementType movementType) {
        this.movementType = movementType;
    }

    public BigDecimal getQuantityDelta() {
        return quantityDelta;
    }

    public void setQuantityDelta(BigDecimal quantityDelta) {
        this.quantityDelta = quantityDelta;
    }

    public BigDecimal getCostPerUnitSnapshot() {
        return costPerUnitSnapshot;
    }

    public void setCostPerUnitSnapshot(BigDecimal costPerUnitSnapshot) {
        this.costPerUnitSnapshot = costPerUnitSnapshot;
    }

    public BigDecimal getCostAmount() {
        if (costAmount == null && quantityDelta != null && costPerUnitSnapshot != null) {
            return quantityDelta.abs().multiply(costPerUnitSnapshot).setScale(2, RoundingMode.HALF_UP);
        }
        return costAmount;
    }

    public void setCostAmount(BigDecimal costAmount) {
        this.costAmount = costAmount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public StockPurchaseLine getPurchaseLine() {
        return purchaseLine;
    }

    public void setPurchaseLine(StockPurchaseLine purchaseLine) {
        this.purchaseLine = purchaseLine;
    }

    public StockDailyEntry getDailyEntry() {
        return dailyEntry;
    }

    public void setDailyEntry(StockDailyEntry dailyEntry) {
        this.dailyEntry = dailyEntry;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public User getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(User recordedBy) {
        this.recordedBy = recordedBy;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockMovement that = (StockMovement) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

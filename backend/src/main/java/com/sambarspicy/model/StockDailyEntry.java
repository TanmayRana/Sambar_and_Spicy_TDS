package com.sambarspicy.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Daily kitchen usage and spoilage/wastage form entry.
 * Atomically generates corresponding negative movements in the stock ledger.
 */
@Entity
@Table(
    name = "stock_daily_entries",
    indexes = {
        @Index(name = "idx_daily_entry_item_date", columnList = "inventory_item_id, business_date"),
        @Index(name = "idx_daily_entry_date", columnList = "business_date")
    }
)
public class StockDailyEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "request_key", length = 36, unique = true)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String requestKey;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(name = "used_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal usedQty = BigDecimal.ZERO;

    @Column(name = "wasted_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal wastedQty = BigDecimal.ZERO;

    @Column(name = "notes", length = 255)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    @JdbcTypeCode(SqlTypes.TINYINT)
    private User createdBy;

    @Column(name = "posted_at", nullable = false, updatable = false)
    private LocalDateTime postedAt;

    public StockDailyEntry() {
    }

    public StockDailyEntry(LocalDate businessDate, InventoryItem inventoryItem, BigDecimal usedQty, BigDecimal wastedQty, User createdBy) {
        this.businessDate = businessDate;
        this.inventoryItem = inventoryItem;
        this.usedQty = usedQty != null ? usedQty : BigDecimal.ZERO;
        this.wastedQty = wastedQty != null ? wastedQty : BigDecimal.ZERO;
        this.createdBy = createdBy;
    }

    @PrePersist
    protected void onCreate() {
        if (this.postedAt == null) {
            this.postedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public BigDecimal getUsedQty() {
        return usedQty;
    }

    public void setUsedQty(BigDecimal usedQty) {
        this.usedQty = usedQty;
    }

    public BigDecimal getWastedQty() {
        return wastedQty;
    }

    public void setWastedQty(BigDecimal wastedQty) {
        this.wastedQty = wastedQty;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(LocalDateTime postedAt) {
        this.postedAt = postedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockDailyEntry that = (StockDailyEntry) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

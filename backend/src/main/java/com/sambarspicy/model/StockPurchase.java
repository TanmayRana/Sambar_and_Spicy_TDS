package com.sambarspicy.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stock purchase header recording goods acquisition.
 * Purchasing inventory adds to assets and increases stock on hand; it is not an immediate P&L expense.
 */
@Entity
@Table(
    name = "stock_purchases",
    indexes = {
        @Index(name = "idx_purchases_business_date", columnList = "business_date")
    }
)
public class StockPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "purchase_reference", length = 100)
    private String purchaseReference;

    @Column(name = "supplier_name", length = 150)
    private String supplierName;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "notes", length = 255)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<StockPurchaseLine> purchaseLines = new ArrayList<>();

    public StockPurchase() {
    }

    public StockPurchase(LocalDate businessDate, BigDecimal totalAmount, User createdBy) {
        this.businessDate = businessDate;
        this.totalAmount = totalAmount;
        this.createdBy = createdBy;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public void addPurchaseLine(StockPurchaseLine line) {
        purchaseLines.add(line);
        line.setPurchase(this);
    }

    public void removePurchaseLine(StockPurchaseLine line) {
        purchaseLines.remove(line);
        line.setPurchase(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPurchaseReference() {
        return purchaseReference;
    }

    public void setPurchaseReference(String purchaseReference) {
        this.purchaseReference = purchaseReference;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<StockPurchaseLine> getPurchaseLines() {
        return purchaseLines;
    }

    public void setPurchaseLines(List<StockPurchaseLine> purchaseLines) {
        this.purchaseLines = purchaseLines;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockPurchase that = (StockPurchase) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

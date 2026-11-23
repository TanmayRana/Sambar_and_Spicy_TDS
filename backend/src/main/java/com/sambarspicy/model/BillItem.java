package com.sambarspicy.model;

import com.sambarspicy.enums.PortionSize;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Line item of a bill representing frozen dish price, portion variant (FULL/HALF),
 * cost snapshot, quantity, and line discount.
 * Gross and net line totals are database-generated stored columns.
 */
@Entity
@Table(
    name = "bill_items",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_bill_menu_item_portion", columnNames = {"bill_id", "menu_item_id", "portion"})
    },
    indexes = {
        @Index(name = "idx_billitem_menu", columnList = "menu_item_id")
    }
)
public class BillItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(name = "item_name_snapshot", nullable = false, length = 150)
    private String itemNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "portion", nullable = false, length = 10)
    private PortionSize portion = PortionSize.FULL;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "estimated_unit_cost_snapshot", precision = 12, scale = 2)
    private BigDecimal estimatedUnitCostSnapshot;

    @Column(name = "line_discount_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineDiscountAmount = BigDecimal.ZERO;

    @Column(name = "line_total", precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal lineTotal;

    @Column(name = "line_net_total", precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal lineNetTotal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BillItem() {
    }

    public BillItem(Bill bill, MenuItem menuItem, String itemNameSnapshot, Integer quantity, BigDecimal unitPrice) {
        this(bill, menuItem, itemNameSnapshot, quantity, unitPrice, PortionSize.FULL);
    }

    public BillItem(Bill bill, MenuItem menuItem, String itemNameSnapshot, Integer quantity, BigDecimal unitPrice, PortionSize portion) {
        this.bill = bill;
        this.menuItem = menuItem;
        this.itemNameSnapshot = itemNameSnapshot;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.portion = portion != null ? portion : PortionSize.FULL;
        this.lineDiscountAmount = BigDecimal.ZERO;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public void setMenuItem(MenuItem menuItem) {
        this.menuItem = menuItem;
    }

    public String getItemNameSnapshot() {
        return itemNameSnapshot;
    }

    public void setItemNameSnapshot(String itemNameSnapshot) {
        this.itemNameSnapshot = itemNameSnapshot;
    }

    public PortionSize getPortion() {
        return portion;
    }

    public void setPortion(PortionSize portion) {
        this.portion = portion;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getEstimatedUnitCostSnapshot() {
        return estimatedUnitCostSnapshot;
    }

    public void setEstimatedUnitCostSnapshot(BigDecimal estimatedUnitCostSnapshot) {
        this.estimatedUnitCostSnapshot = estimatedUnitCostSnapshot;
    }

    public BigDecimal getLineDiscountAmount() {
        return lineDiscountAmount;
    }

    public void setLineDiscountAmount(BigDecimal lineDiscountAmount) {
        this.lineDiscountAmount = lineDiscountAmount;
    }

    public BigDecimal getLineTotal() {
        if (lineTotal == null && quantity != null && unitPrice != null) {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public BigDecimal getLineNetTotal() {
        if (lineNetTotal == null) {
            BigDecimal gross = getLineTotal();
            if (gross != null) {
                BigDecimal discount = lineDiscountAmount != null ? lineDiscountAmount : BigDecimal.ZERO;
                return gross.subtract(discount);
            }
        }
        return lineNetTotal;
    }

    public void setLineNetTotal(BigDecimal lineNetTotal) {
        this.lineNetTotal = lineNetTotal;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BillItem billItem = (BillItem) o;
        return Objects.equals(id, billItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

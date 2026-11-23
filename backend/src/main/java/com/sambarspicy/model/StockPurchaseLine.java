package com.sambarspicy.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Itemized purchase line representing purchased raw material quantity and unit cost.
 * Line total is a database-generated stored column.
 */
@Entity
@Table(
    name = "stock_purchase_lines",
    indexes = {
        @Index(name = "idx_purchase_line_purchase", columnList = "purchase_id"),
        @Index(name = "idx_purchase_line_item", columnList = "inventory_item_id")
    }
)
public class StockPurchaseLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private StockPurchase purchase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_cost", nullable = false, precision = 14, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "line_total", precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal lineTotal;

    public StockPurchaseLine() {
    }

    public StockPurchaseLine(StockPurchase purchase, InventoryItem inventoryItem, BigDecimal quantity, BigDecimal unitCost) {
        this.purchase = purchase;
        this.inventoryItem = inventoryItem;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StockPurchase getPurchase() {
        return purchase;
    }

    public void setPurchase(StockPurchase purchase) {
        this.purchase = purchase;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getLineTotal() {
        if (lineTotal == null && quantity != null && unitCost != null) {
            return quantity.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
        }
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockPurchaseLine that = (StockPurchaseLine) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

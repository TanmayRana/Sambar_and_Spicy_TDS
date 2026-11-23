package com.sambarspicy.model.view;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Read-only JPA entity mapping the database reporting view `vw_daily_menu_item_sales`.
 * Aggregates daily sales quantity, revenue, and estimated profit per dish.
 */
@Entity
@Immutable
@Table(name = "vw_daily_menu_item_sales")
@IdClass(DailyMenuItemSalesId.class)
public class VwDailyMenuItemSales {

    @Id
    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Id
    @Column(name = "menu_item_id", nullable = false)
    private Long menuItemId;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "quantity_sold")
    private BigDecimal quantitySold;

    @Column(name = "sales_revenue")
    private BigDecimal salesRevenue;

    @Column(name = "quantity_missing_cost")
    private BigDecimal quantityMissingCost;

    @Column(name = "estimated_item_cost")
    private BigDecimal estimatedItemCost;

    @Column(name = "estimated_item_profit")
    private BigDecimal estimatedItemProfit;

    public VwDailyMenuItemSales() {
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public Long getMenuItemId() {
        return menuItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public BigDecimal getQuantitySold() {
        return quantitySold;
    }

    public void setQuantitySold(BigDecimal quantitySold) {
        this.quantitySold = quantitySold;
    }

    public BigDecimal getSalesRevenue() {
        return salesRevenue;
    }

    public void setSalesRevenue(BigDecimal salesRevenue) {
        this.salesRevenue = salesRevenue;
    }

    public BigDecimal getQuantityMissingCost() {
        return quantityMissingCost;
    }

    public void setQuantityMissingCost(BigDecimal quantityMissingCost) {
        this.quantityMissingCost = quantityMissingCost;
    }

    public BigDecimal getEstimatedItemCost() {
        return estimatedItemCost;
    }

    public BigDecimal getEstimatedItemProfit() {
        return estimatedItemProfit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VwDailyMenuItemSales that = (VwDailyMenuItemSales) o;
        return Objects.equals(businessDate, that.businessDate) &&
               Objects.equals(menuItemId, that.menuItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(businessDate, menuItemId);
    }
}

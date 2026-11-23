package com.sambarspicy.model.view;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Read-only JPA entity mapping the database reporting view `vw_daily_financial_summary`.
 * Aggregates net sales revenue, daily consumed stock, waste, adjustments, and operating expenses.
 */
@Entity
@Immutable
@Table(name = "vw_daily_financial_summary")
public class VwDailyFinancialSummary {

    @Id
    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "sales_revenue")
    private BigDecimal salesRevenue;

    @Column(name = "stock_used_cost")
    private BigDecimal stockUsedCost;

    @Column(name = "waste_cost")
    private BigDecimal wasteCost;

    @Column(name = "stock_adjustment_loss")
    private BigDecimal stockAdjustmentLoss;

    @Column(name = "operating_expense")
    private BigDecimal operatingExpense;

    @Column(name = "contribution_after_stock_cost")
    private BigDecimal contributionAfterStockCost;

    @Column(name = "estimated_operating_profit")
    private BigDecimal estimatedOperatingProfit;

    @Column(name = "waste_ratio_percent")
    private BigDecimal wasteRatioPercent;

    @Column(name = "estimated_operating_margin_percent")
    private BigDecimal estimatedOperatingMarginPercent;

    public VwDailyFinancialSummary() {
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public BigDecimal getSalesRevenue() {
        return salesRevenue;
    }

    public BigDecimal getStockUsedCost() {
        return stockUsedCost;
    }

    public BigDecimal getWasteCost() {
        return wasteCost;
    }

    public BigDecimal getStockAdjustmentLoss() {
        return stockAdjustmentLoss;
    }

    public BigDecimal getOperatingExpense() {
        return operatingExpense;
    }

    public BigDecimal getContributionAfterStockCost() {
        return contributionAfterStockCost;
    }

    public BigDecimal getEstimatedOperatingProfit() {
        return estimatedOperatingProfit;
    }

    public BigDecimal getWasteRatioPercent() {
        return wasteRatioPercent;
    }

    public BigDecimal getEstimatedOperatingMarginPercent() {
        return estimatedOperatingMarginPercent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VwDailyFinancialSummary that = (VwDailyFinancialSummary) o;
        return Objects.equals(businessDate, that.businessDate);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(businessDate);
    }
}

package com.sambarspicy.model.view;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Composite primary key for `vw_daily_menu_item_sales` view.
 */
public class DailyMenuItemSalesId implements Serializable {

    private LocalDate businessDate;
    private Long menuItemId;

    public DailyMenuItemSalesId() {
    }

    public DailyMenuItemSalesId(LocalDate businessDate, Long menuItemId) {
        this.businessDate = businessDate;
        this.menuItemId = menuItemId;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public Long getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(Long menuItemId) {
        this.menuItemId = menuItemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyMenuItemSalesId that = (DailyMenuItemSalesId) o;
        return Objects.equals(businessDate, that.businessDate) &&
               Objects.equals(menuItemId, that.menuItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(businessDate, menuItemId);
    }
}

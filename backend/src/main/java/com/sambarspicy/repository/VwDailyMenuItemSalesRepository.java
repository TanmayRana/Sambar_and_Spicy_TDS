package com.sambarspicy.repository;

import com.sambarspicy.model.view.DailyMenuItemSalesId;
import com.sambarspicy.model.view.VwDailyMenuItemSales;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface VwDailyMenuItemSalesRepository extends JpaRepository<VwDailyMenuItemSales, DailyMenuItemSalesId> {
    List<VwDailyMenuItemSales> findByBusinessDateOrderByQuantitySoldDesc(LocalDate businessDate);
    List<VwDailyMenuItemSales> findByBusinessDateBetweenOrderByBusinessDateDesc(LocalDate startDate, LocalDate endDate);
}

package com.sambarspicy.repository;

import com.sambarspicy.model.StockDailyEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockDailyEntryRepository extends JpaRepository<StockDailyEntry, Long> {
    Optional<StockDailyEntry> findByRequestKey(String requestKey);
    List<StockDailyEntry> findByBusinessDateOrderByPostedAtDesc(LocalDate businessDate);
    List<StockDailyEntry> findByInventoryItemIdAndBusinessDate(Long inventoryItemId, LocalDate businessDate);
}

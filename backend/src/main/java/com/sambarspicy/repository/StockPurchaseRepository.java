package com.sambarspicy.repository;

import com.sambarspicy.model.StockPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockPurchaseRepository extends JpaRepository<StockPurchase, Long> {
    List<StockPurchase> findByBusinessDateOrderByCreatedAtDesc(LocalDate businessDate);
    List<StockPurchase> findByBusinessDateBetweenOrderByBusinessDateDesc(LocalDate startDate, LocalDate endDate);
}

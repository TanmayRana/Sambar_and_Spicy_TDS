package com.sambarspicy.repository;

import com.sambarspicy.model.StockPurchaseLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StockPurchaseLineRepository extends JpaRepository<StockPurchaseLine, Long> {
    List<StockPurchaseLine> findByPurchaseId(Long purchaseId);
    List<StockPurchaseLine> findByInventoryItemId(Long inventoryItemId);
}

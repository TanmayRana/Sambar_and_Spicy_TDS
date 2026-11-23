package com.sambarspicy.repository;

import com.sambarspicy.model.StockMovement;
import com.sambarspicy.enums.StockMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByInventoryItemIdOrderByRecordedAtDesc(Long inventoryItemId);
    List<StockMovement> findByBusinessDateAndMovementType(LocalDate businessDate, StockMovementType movementType);
    List<StockMovement> findByBusinessDateBetweenOrderByRecordedAtDesc(LocalDate startDate, LocalDate endDate);
    Optional<StockMovement> findByPurchaseLineId(Long purchaseLineId);
    Optional<StockMovement> findByDailyEntryIdAndMovementType(Long dailyEntryId, StockMovementType movementType);
}

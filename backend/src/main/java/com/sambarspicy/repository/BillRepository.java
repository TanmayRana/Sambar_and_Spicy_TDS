package com.sambarspicy.repository;

import com.sambarspicy.model.Bill;
import com.sambarspicy.enums.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByBillNumber(String billNumber);

    Optional<Bill> findByCheckoutKey(String checkoutKey);

    List<Bill> findByStatusOrderByCreatedAtAsc(BillStatus status);

    List<Bill> findByStatusAndBusinessDate(BillStatus status, LocalDate businessDate);

    List<Bill> findByBusinessDateBetweenOrderByCreatedAtDesc(LocalDate startDate, LocalDate endDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bill b WHERE b.id = :id")
    Optional<Bill> findByIdForUpdate(@Param("id") Long id);
}

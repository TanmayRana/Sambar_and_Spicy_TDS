package com.sambarspicy.repository;

import com.sambarspicy.model.BillCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface BillCounterRepository extends JpaRepository<BillCounter, LocalDate> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bc FROM BillCounter bc WHERE bc.businessDate = :businessDate")
    Optional<BillCounter> findByBusinessDateForUpdate(@Param("businessDate") LocalDate businessDate);
}

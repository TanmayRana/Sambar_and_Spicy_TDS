package com.sambarspicy.repository;

import com.sambarspicy.model.view.VwDailyFinancialSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface VwDailyFinancialSummaryRepository extends JpaRepository<VwDailyFinancialSummary, LocalDate> {
    List<VwDailyFinancialSummary> findByBusinessDateBetweenOrderByBusinessDateDesc(LocalDate startDate, LocalDate endDate);
}

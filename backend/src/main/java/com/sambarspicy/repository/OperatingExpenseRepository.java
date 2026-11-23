package com.sambarspicy.repository;

import com.sambarspicy.model.OperatingExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OperatingExpenseRepository extends JpaRepository<OperatingExpense, Long> {
    List<OperatingExpense> findByBusinessDateOrderByCreatedAtDesc(LocalDate businessDate);
    List<OperatingExpense> findByBusinessDateBetweenOrderByBusinessDateDesc(LocalDate startDate, LocalDate endDate);
    List<OperatingExpense> findByExpenseCategory(String expenseCategory);
}

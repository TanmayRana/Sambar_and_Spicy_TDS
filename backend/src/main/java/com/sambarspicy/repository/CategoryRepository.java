package com.sambarspicy.repository;

import com.sambarspicy.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Category c where c.id = :id")
    java.util.Optional<Category> findByIdForBilling(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<Category> findByName(String name);
    Optional<Category> findByNameIgnoreCase(String name);
    List<Category> findByIsActiveTrueOrderBySortOrderAsc();
    List<Category> findAllByOrderBySortOrderAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    boolean existsByImagePath(String imagePath);
}

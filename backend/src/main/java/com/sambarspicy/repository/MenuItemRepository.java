package com.sambarspicy.repository;

import com.sambarspicy.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MenuItem m where m.id = :id")
    Optional<MenuItem> findByIdForBilling(@Param("id") Long id);
    Optional<MenuItem> findBySku(String sku);
    Optional<MenuItem> findBySkuIgnoreCase(String sku);
    Optional<MenuItem> findByCategoryIdAndName(Long categoryId, String name);
    Optional<MenuItem> findByCategoryIdAndNameIgnoreCase(Long categoryId, String name);

    List<MenuItem> findByCategoryIdAndIsAvailableTrueOrderBySortOrderAsc(Long categoryId);
    List<MenuItem> findByCategoryIdOrderBySortOrderAsc(Long categoryId);

    List<MenuItem> findByIsAvailableTrueOrderBySortOrderAsc();
    List<MenuItem> findAllByOrderBySortOrderAsc();

    List<MenuItem> findByNameContainingIgnoreCaseAndIsAvailableTrueOrderBySortOrderAsc(String name);
    List<MenuItem> findByNameContainingIgnoreCaseOrderBySortOrderAsc(String name);

    List<MenuItem> findByCategoryIdAndNameContainingIgnoreCaseAndIsAvailableTrueOrderBySortOrderAsc(Long categoryId, String name);
    List<MenuItem> findByCategoryIdAndNameContainingIgnoreCaseOrderBySortOrderAsc(Long categoryId, String name);

    @Query("SELECT m FROM MenuItem m WHERE (:categoryId IS NULL OR m.category.id = :categoryId) " +
           "AND (:activeOnly = false OR m.isAvailable = true) " +
           "AND (:query IS NULL OR TRIM(:query) = '' " +
           "     OR LOWER(m.name) LIKE LOWER(CONCAT('%', TRIM(:query), '%')) " +
           "     OR LOWER(m.sku) LIKE LOWER(CONCAT('%', TRIM(:query), '%')) " +
           "     OR LOWER(m.description) LIKE LOWER(CONCAT('%', TRIM(:query), '%'))) " +
           "ORDER BY m.sortOrder ASC, m.name ASC")
    List<MenuItem> searchMenuItems(@Param("categoryId") Long categoryId,
                                  @Param("query") String query,
                                  @Param("activeOnly") boolean activeOnly);

    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);
    boolean existsByCategoryIdAndNameIgnoreCaseAndIdNot(Long categoryId, String name, Long id);

    boolean existsByCategoryId(Long categoryId);
    boolean existsByImagePath(String imagePath);
    long countByCategoryId(Long categoryId);
}

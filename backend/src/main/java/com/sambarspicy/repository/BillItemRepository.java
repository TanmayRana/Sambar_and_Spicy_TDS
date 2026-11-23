package com.sambarspicy.repository;

import com.sambarspicy.model.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BillItemRepository extends JpaRepository<BillItem, Long> {
    List<BillItem> findByBillId(Long billId);
    boolean existsByMenuItemId(Long menuItemId);
    long countByMenuItemId(Long menuItemId);
}

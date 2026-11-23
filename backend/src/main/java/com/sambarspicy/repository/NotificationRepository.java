package com.sambarspicy.repository;

import com.sambarspicy.model.Notification;
import com.sambarspicy.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByStatusOrderByCreatedAtDesc(NotificationStatus status);
    Optional<Notification> findByInventoryItemIdAndStatus(Long inventoryItemId, NotificationStatus status);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select n from Notification n where n.id = :id")
    Optional<Notification> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}

package com.sambarspicy.repository;

import com.sambarspicy.model.StoreSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface StoreSettingRepository extends JpaRepository<StoreSetting, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StoreSetting s where s.id = 1")
    Optional<StoreSetting> lockSingleton();
}

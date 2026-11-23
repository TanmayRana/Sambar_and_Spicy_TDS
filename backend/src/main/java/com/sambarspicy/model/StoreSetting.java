package com.sambarspicy.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Singleton entity storing store contact identity and receipt footer.
 * Database check constraint enforces id = 1.
 */
@Entity
@Table(name = "store_settings")
public class StoreSetting {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer id = 1;

    @Column(name = "store_name", nullable = false, length = 120)
    private String storeName;

    @Column(name = "address", nullable = false, length = 500)
    private String address = "";

    @Column(name = "phone", nullable = false, length = 25)
    private String phone = "";

    @Column(name = "currency_code", nullable = false, length = 3, columnDefinition = "CHAR(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currencyCode = "INR";

    @Column(name = "business_timezone", nullable = false, length = 64)
    private String businessTimezone = "Asia/Kolkata";

    @Column(name = "receipt_footer", length = 255)
    private String receiptFooter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", columnDefinition = "TINYINT UNSIGNED")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private User updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StoreSetting() {
    }

    public StoreSetting(String storeName, String address, String phone) {
        this.id = 1;
        this.storeName = storeName;
        this.address = address;
        this.phone = phone;
        this.currencyCode = "INR";
        this.businessTimezone = "Asia/Kolkata";
    }

    @PrePersist
    @PreUpdate
    protected void onPersistOrUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getBusinessTimezone() {
        return businessTimezone;
    }

    public void setBusinessTimezone(String businessTimezone) {
        this.businessTimezone = businessTimezone;
    }

    public String getReceiptFooter() {
        return receiptFooter;
    }

    public void setReceiptFooter(String receiptFooter) {
        this.receiptFooter = receiptFooter;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoreSetting that = (StoreSetting) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

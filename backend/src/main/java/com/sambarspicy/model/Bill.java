package com.sambarspicy.model;

import com.sambarspicy.enums.BillStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bill header representing a sales transaction in HELD, PAID, or CANCELLED status.
 * Paid bills are strictly immutable and bear sequential receipt numbers and frozen store details.
 */
@Entity
@Table(
    name = "bills",
    indexes = {
        @Index(name = "idx_bills_reporting", columnList = "status, business_date, paid_at"),
        @Index(name = "idx_bills_held", columnList = "status, created_at"),
        @Index(name = "idx_bills_user_date", columnList = "created_by, business_date")
    }
)
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "bill_number", length = 40, unique = true)
    private String billNumber;

    @Column(name = "checkout_key", length = 36, unique = true, columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String checkoutKey;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private BillStatus status = BillStatus.HELD;

    @Column(name = "hold_label", length = 80)
    private String holdLabel;

    @Column(name = "item_count", nullable = false)
    private Integer itemCount = 0;

    @Column(name = "subtotal_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "round_off_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal roundOffAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "store_name_snapshot", length = 120)
    private String storeNameSnapshot;

    @Column(name = "store_address_snapshot", length = 500)
    private String storeAddressSnapshot;

    @Column(name = "store_phone_snapshot", length = 25)
    private String storePhoneSnapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by", columnDefinition = "TINYINT UNSIGNED")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private User paidBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by", columnDefinition = "TINYINT UNSIGNED")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private User cancelledBy;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "held_at")
    private LocalDateTime heldAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BillItem> billItems = new ArrayList<>();

    @OneToOne(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private BillPayment payment;

    public Bill() {
    }

    public Bill(LocalDate businessDate, User createdBy) {
        this.businessDate = businessDate;
        this.createdBy = createdBy;
        this.status = BillStatus.HELD;
        this.subtotalAmount = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
        this.itemCount = 0;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
    }

    public void addBillItem(BillItem item) {
        billItems.add(item);
        item.setBill(this);
    }

    public void removeBillItem(BillItem item) {
        billItems.remove(item);
        item.setBill(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public String getCheckoutKey() {
        return checkoutKey;
    }

    public void setCheckoutKey(String checkoutKey) {
        this.checkoutKey = checkoutKey;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public BillStatus getStatus() {
        return status;
    }

    public void setStatus(BillStatus status) {
        this.status = status;
    }

    public String getHoldLabel() {
        return holdLabel;
    }

    public void setHoldLabel(String holdLabel) {
        this.holdLabel = holdLabel;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getRoundOffAmount() { return roundOffAmount; }

    public void setRoundOffAmount(BigDecimal roundOffAmount) { this.roundOffAmount = roundOffAmount; }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getStoreNameSnapshot() {
        return storeNameSnapshot;
    }

    public void setStoreNameSnapshot(String storeNameSnapshot) {
        this.storeNameSnapshot = storeNameSnapshot;
    }

    public String getStoreAddressSnapshot() {
        return storeAddressSnapshot;
    }

    public void setStoreAddressSnapshot(String storeAddressSnapshot) {
        this.storeAddressSnapshot = storeAddressSnapshot;
    }

    public String getStorePhoneSnapshot() {
        return storePhoneSnapshot;
    }

    public void setStorePhoneSnapshot(String storePhoneSnapshot) {
        this.storePhoneSnapshot = storePhoneSnapshot;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public User getPaidBy() {
        return paidBy;
    }

    public void setPaidBy(User paidBy) {
        this.paidBy = paidBy;
    }

    public User getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(User cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getHeldAt() {
        return heldAt;
    }

    public void setHeldAt(LocalDateTime heldAt) {
        this.heldAt = heldAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public BillPayment getPayment() {
        return payment;
    }

    public void setPayment(BillPayment payment) {
        this.payment = payment;
        if (payment != null) {
            payment.setBill(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bill bill = (Bill) o;
        return Objects.equals(id, bill.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

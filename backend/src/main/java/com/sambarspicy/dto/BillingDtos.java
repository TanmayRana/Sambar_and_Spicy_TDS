package com.sambarspicy.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.sambarspicy.enums.PaymentMethod;
import com.sambarspicy.enums.PortionSize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class BillingDtos {
    private BillingDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record LineRequest(@NotNull @Positive Long menuItemId, PortionSize portion,
                              @NotNull @Positive @Max(100000) Integer quantity,
                              @DecimalMin("0.00") @Digits(integer=12, fraction=2) BigDecimal lineDiscountAmount,
                              @DecimalMin("0.00") @Digits(integer=10, fraction=2) BigDecimal expectedUnitPrice) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record BillRequest(@NotEmpty @Size(max=200) List<@NotNull @Valid LineRequest> items,
                              @Size(max=80) String holdLabel, @Size(max=255) String note,
                              @Positive Long billId) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CheckoutRequest(@NotEmpty @Size(max=200) List<@NotNull @Valid LineRequest> items,
                                  @Size(max=80) String holdLabel, @Size(max=255) String note,
                                  @Positive Long billId, @NotBlank @Size(max=36) String checkoutKey,
                                  @NotNull PaymentMethod paymentMethod,
                                  @NotNull @DecimalMin("0.00") @Digits(integer=12, fraction=2) BigDecimal tenderedAmount,
                                  @Size(max=100) String referenceNumber, Boolean roundOff) {}

    public record CancelRequest(@NotBlank @Size(max=255) String reason) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Line(Long id, Long billId, Long menuItemId, PortionSize portion, String itemNameSnapshot,
                       Integer quantity, BigDecimal unitPrice, BigDecimal estimatedUnitCostSnapshot,
                       BigDecimal lineDiscountAmount, BigDecimal lineTotal, BigDecimal lineNetTotal, LocalDateTime createdAt) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Payment(Long id, Long billId, PaymentMethod paymentMethod, BigDecimal amount,
                          BigDecimal tenderedAmount, BigDecimal changeAmount, String referenceNumber,
                          Integer recordedBy, LocalDateTime recordedAt) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Detail(Long id, String billNumber, String checkoutKey, LocalDate businessDate, String status,
                         String holdLabel, Integer itemCount, BigDecimal subtotalAmount, BigDecimal discountAmount, BigDecimal roundOffAmount,
                         BigDecimal totalAmount, String note, String storeNameSnapshot, String storeAddressSnapshot,
                         String storePhoneSnapshot, Integer createdBy, Integer paidBy, Integer cancelledBy,
                         String cancelReason, LocalDateTime createdAt, LocalDateTime heldAt, LocalDateTime paidAt,
                         LocalDateTime cancelledAt, LocalDateTime updatedAt, List<Line> items, Payment payment,
                         String cashierName) {}
}

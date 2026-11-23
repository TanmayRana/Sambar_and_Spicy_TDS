package com.sambarspicy.enums;

/**
 * Lifecycle status of a customer bill.
 * Only PAID bills represent finalized financial revenue and bear receipt numbers.
 */
public enum BillStatus {
    HELD,
    PAID,
    CANCELLED
}

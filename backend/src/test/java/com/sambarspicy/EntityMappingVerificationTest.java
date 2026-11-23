package com.sambarspicy;

import com.sambarspicy.model.*;
import com.sambarspicy.model.view.*;
import com.sambarspicy.enums.*;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rigorous architectural test verifying all 17 tables, 2 views, 7 enums,
 * precision/scale mappings, and generated column settings against the authoritative MySQL schema.
 */
class EntityMappingVerificationTest {

    @Test
    @DisplayName("Verify exactly 17 application entities map to authoritative MySQL table names")
    void test17TableMappings() {
        Map<Class<?>, String> expectedTables = new HashMap<>();
        expectedTables.put(User.class, "users");
        expectedTables.put(AuthRefreshToken.class, "auth_refresh_tokens");
        expectedTables.put(StoreSetting.class, "store_settings");
        expectedTables.put(Category.class, "categories");
        expectedTables.put(MenuItem.class, "menu_items");
        expectedTables.put(BillCounter.class, "bill_counters");
        expectedTables.put(Bill.class, "bills");
        expectedTables.put(BillItem.class, "bill_items");
        expectedTables.put(BillPayment.class, "bill_payments");
        expectedTables.put(InventoryItem.class, "inventory_items");
        expectedTables.put(StockPurchase.class, "stock_purchases");
        expectedTables.put(StockPurchaseLine.class, "stock_purchase_lines");
        expectedTables.put(StockDailyEntry.class, "stock_daily_entries");
        expectedTables.put(StockMovement.class, "stock_movements");
        expectedTables.put(OperatingExpense.class, "operating_expenses");
        expectedTables.put(Notification.class, "notifications");
        expectedTables.put(AuditLog.class, "audit_logs");

        assertEquals(17, expectedTables.size(), "Must have exactly 17 application tables");

        for (Map.Entry<Class<?>, String> entry : expectedTables.entrySet()) {
            Table tableAnn = entry.getKey().getAnnotation(Table.class);
            assertNotNull(tableAnn, "Entity " + entry.getKey().getSimpleName() + " must have @Table annotation");
            assertEquals(entry.getValue(), tableAnn.name(), "Entity " + entry.getKey().getSimpleName() + " table name mismatch");
        }
    }

    @Test
    @DisplayName("Verify reporting views map to authoritative view names")
    void testReportingViewMappings() {
        Table summaryView = VwDailyFinancialSummary.class.getAnnotation(Table.class);
        assertNotNull(summaryView);
        assertEquals("vw_daily_financial_summary", summaryView.name());

        Table itemSalesView = VwDailyMenuItemSales.class.getAnnotation(Table.class);
        assertNotNull(itemSalesView);
        assertEquals("vw_daily_menu_item_sales", itemSalesView.name());
    }

    @Test
    @DisplayName("Verify all 7 enum types have exact database check values")
    void testEnumValues() {
        assertArrayEquals(new String[]{"ADMIN", "BILLING", "READ_ONLY"},
                Arrays.stream(UserRole.values()).map(Enum::name).toArray());

        assertArrayEquals(new String[]{"HELD", "PAID", "CANCELLED"},
                Arrays.stream(BillStatus.values()).map(Enum::name).toArray());

        assertArrayEquals(new String[]{"CASH", "UPI", "CARD"},
                Arrays.stream(PaymentMethod.values()).map(Enum::name).toArray());

        assertArrayEquals(new String[]{"PACK", "KG", "LITRE", "PIECE"},
                Arrays.stream(InventoryUnit.values()).map(Enum::name).toArray());

        assertArrayEquals(new String[]{"PURCHASE", "DAILY_USE", "WASTE", "OPENING", "ADJUSTMENT_IN", "ADJUSTMENT_OUT"},
                Arrays.stream(StockMovementType.values()).map(Enum::name).toArray());

        assertArrayEquals(new String[]{"WARNING", "CRITICAL"},
                Arrays.stream(NotificationSeverity.values()).map(Enum::name).toArray());

        assertArrayEquals(new String[]{"OPEN", "RESOLVED"},
                Arrays.stream(NotificationStatus.values()).map(Enum::name).toArray());

        assertArrayEquals(new String[]{"FULL", "HALF"},
                Arrays.stream(PortionSize.values()).map(Enum::name).toArray());
    }

    @Test
    @DisplayName("Verify database generated columns are mapped with insertable=false and updatable=false")
    void testGeneratedColumnsReadOnly() throws NoSuchFieldException {
        // BillItem: line_total, line_net_total
        Field lineTotal = BillItem.class.getDeclaredField("lineTotal");
        Column colLineTotal = lineTotal.getAnnotation(Column.class);
        assertNotNull(colLineTotal);
        assertFalse(colLineTotal.insertable());
        assertFalse(colLineTotal.updatable());

        Field lineNetTotal = BillItem.class.getDeclaredField("lineNetTotal");
        Column colLineNetTotal = lineNetTotal.getAnnotation(Column.class);
        assertNotNull(colLineNetTotal);
        assertFalse(colLineNetTotal.insertable());
        assertFalse(colLineNetTotal.updatable());

        // BillPayment: change_amount
        Field changeAmount = BillPayment.class.getDeclaredField("changeAmount");
        Column colChange = changeAmount.getAnnotation(Column.class);
        assertNotNull(colChange);
        assertFalse(colChange.insertable());
        assertFalse(colChange.updatable());

        // StockPurchaseLine: line_total
        Field purchaseLineTotal = StockPurchaseLine.class.getDeclaredField("lineTotal");
        Column colPurchaseLineTotal = purchaseLineTotal.getAnnotation(Column.class);
        assertNotNull(colPurchaseLineTotal);
        assertFalse(colPurchaseLineTotal.insertable());
        assertFalse(colPurchaseLineTotal.updatable());

        // StockMovement: cost_amount
        Field costAmount = StockMovement.class.getDeclaredField("costAmount");
        Column colCostAmount = costAmount.getAnnotation(Column.class);
        assertNotNull(colCostAmount);
        assertFalse(colCostAmount.insertable());
        assertFalse(colCostAmount.updatable());

        // Notification: open_inventory_item_id
        Field openItem = Notification.class.getDeclaredField("openInventoryItemId");
        Column colOpenItem = openItem.getAnnotation(Column.class);
        assertNotNull(colOpenItem);
        assertFalse(colOpenItem.insertable());
        assertFalse(colOpenItem.updatable());
    }

    @Test
    @DisplayName("Verify decimal monetary and quantity precision/scale")
    void testDecimalPrecisionAndScale() throws NoSuchFieldException {
        // Bill total: DECIMAL(14,2)
        Column totalCol = Bill.class.getDeclaredField("totalAmount").getAnnotation(Column.class);
        assertEquals(14, totalCol.precision());
        assertEquals(2, totalCol.scale());

        // MenuItem selling_price: DECIMAL(12,2)
        Column priceCol = MenuItem.class.getDeclaredField("sellingPrice").getAnnotation(Column.class);
        assertEquals(12, priceCol.precision());
        assertEquals(2, priceCol.scale());

        Column halfPriceCol = MenuItem.class.getDeclaredField("halfSellingPrice").getAnnotation(Column.class);
        assertEquals(12, halfPriceCol.precision());
        assertEquals(2, halfPriceCol.scale());

        Column halfCostCol = MenuItem.class.getDeclaredField("halfEstimatedUnitCost").getAnnotation(Column.class);
        assertEquals(12, halfCostCol.precision());
        assertEquals(2, halfCostCol.scale());

        // BillItem portion: length 10
        Column portionCol = BillItem.class.getDeclaredField("portion").getAnnotation(Column.class);
        assertEquals(10, portionCol.length());
        assertFalse(portionCol.nullable());

        // InventoryItem current_qty: DECIMAL(14,3)
        Column qtyCol = InventoryItem.class.getDeclaredField("currentQty").getAnnotation(Column.class);
        assertEquals(14, qtyCol.precision());
        assertEquals(3, qtyCol.scale());

        // InventoryItem avg_unit_cost: DECIMAL(14,4)
        Column costCol = InventoryItem.class.getDeclaredField("avgUnitCost").getAnnotation(Column.class);
        assertEquals(14, costCol.precision());
        assertEquals(4, costCol.scale());
    }

    @Test
    @DisplayName("Verify temporal types use Java 8+ LocalDate and LocalDateTime")
    void testTemporalTypes() throws NoSuchFieldException {
        assertEquals(LocalDate.class, Bill.class.getDeclaredField("businessDate").getType());
        assertEquals(LocalDateTime.class, Bill.class.getDeclaredField("createdAt").getType());
        assertEquals(LocalDate.class, BillCounter.class.getDeclaredField("businessDate").getType());
        assertEquals(LocalDate.class, StockPurchase.class.getDeclaredField("businessDate").getType());
        assertEquals(LocalDateTime.class, StockMovement.class.getDeclaredField("recordedAt").getType());
    }

    @Test
    @DisplayName("Verify User slot constraints and primary key type")
    void testUserSlotTypes() throws NoSuchFieldException {
        assertEquals(Integer.class, User.class.getDeclaredField("id").getType());
        assertEquals(Integer.class, StoreSetting.class.getDeclaredField("id").getType());
    }

    @Test
    @DisplayName("Verify TINYINT and JSON column mappings for Hibernate schema-validation")
    void testTinyIntUnsignedAndJsonMappings() throws NoSuchFieldException {
        // User.id
        Field userId = User.class.getDeclaredField("id");
        assertEquals(SqlTypes.TINYINT, userId.getAnnotation(JdbcTypeCode.class).value());

        // StoreSetting.id & updatedBy
        Field storeId = StoreSetting.class.getDeclaredField("id");
        assertEquals(SqlTypes.TINYINT, storeId.getAnnotation(JdbcTypeCode.class).value());
        Field storeUpdatedBy = StoreSetting.class.getDeclaredField("updatedBy");
        assertEquals(SqlTypes.TINYINT, storeUpdatedBy.getAnnotation(JdbcTypeCode.class).value());

        // AuditLog actorUser, oldData, newData
        Field auditActor = AuditLog.class.getDeclaredField("actorUser");
        assertEquals(SqlTypes.TINYINT, auditActor.getAnnotation(JdbcTypeCode.class).value());
        Field oldData = AuditLog.class.getDeclaredField("oldData");
        assertEquals(SqlTypes.JSON, oldData.getAnnotation(JdbcTypeCode.class).value());
        Field newData = AuditLog.class.getDeclaredField("newData");
        assertEquals(SqlTypes.JSON, newData.getAnnotation(JdbcTypeCode.class).value());

        // AuthRefreshToken user
        Field authUser = AuthRefreshToken.class.getDeclaredField("user");
        assertEquals(SqlTypes.TINYINT, authUser.getAnnotation(JdbcTypeCode.class).value());

        // Bill createdBy, paidBy, cancelledBy
        Field billCreated = Bill.class.getDeclaredField("createdBy");
        assertEquals(SqlTypes.TINYINT, billCreated.getAnnotation(JdbcTypeCode.class).value());
        Field billPaid = Bill.class.getDeclaredField("paidBy");
        assertEquals(SqlTypes.TINYINT, billPaid.getAnnotation(JdbcTypeCode.class).value());
        Field billCancelled = Bill.class.getDeclaredField("cancelledBy");
        assertEquals(SqlTypes.TINYINT, billCancelled.getAnnotation(JdbcTypeCode.class).value());

        // BillPayment recordedBy
        Field paymentRecorded = BillPayment.class.getDeclaredField("recordedBy");
        assertEquals(SqlTypes.TINYINT, paymentRecorded.getAnnotation(JdbcTypeCode.class).value());

        // StockPurchase createdBy
        Field purchaseCreated = StockPurchase.class.getDeclaredField("createdBy");
        assertEquals(SqlTypes.TINYINT, purchaseCreated.getAnnotation(JdbcTypeCode.class).value());

        // StockDailyEntry createdBy
        Field dailyCreated = StockDailyEntry.class.getDeclaredField("createdBy");
        assertEquals(SqlTypes.TINYINT, dailyCreated.getAnnotation(JdbcTypeCode.class).value());

        // StockMovement recordedBy
        Field movementRecorded = StockMovement.class.getDeclaredField("recordedBy");
        assertEquals(SqlTypes.TINYINT, movementRecorded.getAnnotation(JdbcTypeCode.class).value());

        // OperatingExpense createdBy
        Field expenseCreated = OperatingExpense.class.getDeclaredField("createdBy");
        assertEquals(SqlTypes.TINYINT, expenseCreated.getAnnotation(JdbcTypeCode.class).value());

        // Notification readBy
        Field notificationRead = Notification.class.getDeclaredField("readBy");
        assertEquals(SqlTypes.TINYINT, notificationRead.getAnnotation(JdbcTypeCode.class).value());

        // CHAR columns
        Field tokenHash = AuthRefreshToken.class.getDeclaredField("tokenHash");
        assertEquals(SqlTypes.CHAR, tokenHash.getAnnotation(JdbcTypeCode.class).value());

        Field currencyCode = StoreSetting.class.getDeclaredField("currencyCode");
        assertEquals(SqlTypes.CHAR, currencyCode.getAnnotation(JdbcTypeCode.class).value());

        Field checkoutKey = Bill.class.getDeclaredField("checkoutKey");
        assertEquals(SqlTypes.CHAR, checkoutKey.getAnnotation(JdbcTypeCode.class).value());

        Field requestKey = StockDailyEntry.class.getDeclaredField("requestKey");
        assertEquals(SqlTypes.CHAR, requestKey.getAnnotation(JdbcTypeCode.class).value());
    }
}

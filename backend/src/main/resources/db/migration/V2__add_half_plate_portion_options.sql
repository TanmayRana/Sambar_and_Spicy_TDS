-- ============================================================================
-- SAMBAR & SPICY — FLYWAY V2 MIGRATION: PORTION VARIANTS (HALF & FULL PLATE)
-- PostgreSQL dialect
-- ============================================================================

-- 1. Extend menu_items with optional half plate pricing and cost
ALTER TABLE menu_items
  ADD COLUMN has_half_portion SMALLINT NOT NULL DEFAULT 0,
  ADD COLUMN half_selling_price NUMERIC(12,2) NULL,
  ADD COLUMN half_estimated_unit_cost NUMERIC(12,2) NULL,
  ADD CONSTRAINT chk_menu_half_price CHECK (
    has_half_portion = 0 OR (half_selling_price IS NOT NULL AND half_selling_price >= 0)
  );

-- 2. Extend bill_items with portion indicator and update uniqueness constraint
ALTER TABLE bill_items
  ADD COLUMN portion VARCHAR(10) NOT NULL DEFAULT 'FULL',
  ADD CONSTRAINT chk_billitem_portion CHECK (portion IN ('FULL', 'HALF')),
  DROP CONSTRAINT uq_bill_menu_item,
  ADD CONSTRAINT uq_bill_menu_item_portion UNIQUE (bill_id, menu_item_id, portion);

-- ============================================================================
-- SAMBAR & SPICY — FLYWAY V2 MIGRATION: PORTION VARIANTS (HALF & FULL PLATE)
-- Enables dish portioning options (Full Plate vs Half Plate) for billing.
-- ============================================================================

-- 1. Extend menu_items with optional half plate pricing and cost
ALTER TABLE `menu_items`
  ADD COLUMN `has_half_portion` TINYINT(1) NOT NULL DEFAULT 0 AFTER `selling_price`,
  ADD COLUMN `half_selling_price` DECIMAL(12,2) NULL AFTER `has_half_portion`,
  ADD COLUMN `half_estimated_unit_cost` DECIMAL(12,2) NULL AFTER `estimated_unit_cost`,
  ADD CONSTRAINT `chk_menu_half_price` CHECK (
    `has_half_portion` = 0 OR (`half_selling_price` IS NOT NULL AND `half_selling_price` >= 0)
  );

-- 2. Extend bill_items with portion indicator and update uniqueness constraint
ALTER TABLE `bill_items`
  ADD COLUMN `portion` VARCHAR(10) NOT NULL DEFAULT 'FULL' AFTER `item_name_snapshot`,
  ADD CONSTRAINT `chk_billitem_portion` CHECK (`portion` IN ('FULL', 'HALF')),
  DROP INDEX `uq_bill_menu_item`,
  ADD UNIQUE KEY `uq_bill_menu_item_portion` (`bill_id`, `menu_item_id`, `portion`);

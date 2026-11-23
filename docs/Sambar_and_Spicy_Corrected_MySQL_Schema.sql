-- ============================================================================
-- SAMBAR & SPICY | CORRECTED DATABASE SCHEMA
-- Target: MySQL 8.0.16+ (recommended MySQL 8.4 LTS), InnoDB / utf8mb4
-- New, single-outlet database. NO DROP, TRUNCATE, or destructive migration.
-- Frontend: Next.js    Backend: Spring Boot    Currency: INR    Store TZ: Asia/Kolkata
-- Design: 17 tables + 2 reporting views, with exactly THREE FIXED USER SLOTS.
-- IMPORTANT: Run against a NEW database or review differences before migrating
-- an existing database. Back up existing data before any later schema changes.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `sambar_spicy`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `sambar_spicy`;

-- ---------------------------------------------------------------------------
-- 01. USERS - no signup; only ID 1/2/3 with permanently assigned roles.
-- Passwords MUST be produced by Spring Security's BCrypt/Argon2 password encoder.
-- Create the 3 accounts in an authorized bootstrap process, NOT in this SQL.
-- ---------------------------------------------------------------------------
CREATE TABLE `users` (
  `id` TINYINT UNSIGNED NOT NULL,
  `role` VARCHAR(16) NOT NULL,
  `username` VARCHAR(80) NOT NULL,
  `full_name` VARCHAR(120) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `last_login_at` DATETIME(3) NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_users_role` (`role`),
  UNIQUE KEY `uq_users_username` (`username`),
  CONSTRAINT `chk_users_fixed_slots` CHECK (
    (`id` = 1 AND `role` = 'ADMIN') OR
    (`id` = 2 AND `role` = 'BILLING') OR
    (`id` = 3 AND `role` = 'READ_ONLY')
  ),
  CONSTRAINT `chk_users_active` CHECK (`is_active` IN (0,1))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 02. AUTH REFRESH TOKENS - hash only, no plaintext session/refresh tokens.
-- Access tokens should be short-lived and held in HttpOnly Secure cookies.
-- ---------------------------------------------------------------------------
CREATE TABLE `auth_refresh_tokens` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` TINYINT UNSIGNED NOT NULL,
  `token_hash` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `expires_at` DATETIME(3) NOT NULL,
  `revoked_at` DATETIME(3) NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_refresh_token_hash` (`token_hash`),
  KEY `idx_refresh_user_expiry` (`user_id`, `expires_at`),
  CONSTRAINT `fk_refresh_user` FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 03. STORE SETTINGS - single row (id=1).
-- ---------------------------------------------------------------------------
CREATE TABLE `store_settings` (
  `id` TINYINT UNSIGNED NOT NULL,
  `store_name` VARCHAR(120) NOT NULL,
  `address` VARCHAR(500) NOT NULL DEFAULT '',
  `phone` VARCHAR(25) NOT NULL DEFAULT '',
  `currency_code` CHAR(3) NOT NULL DEFAULT 'INR',
  `business_timezone` VARCHAR(64) NOT NULL DEFAULT 'Asia/Kolkata',
  `receipt_footer` VARCHAR(255) NULL,
  `updated_by` TINYINT UNSIGNED NULL,
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_settings_singleton` CHECK (`id` = 1),
  CONSTRAINT `fk_settings_updated_by` FOREIGN KEY (`updated_by`)
    REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 04. CATEGORIES - one optional cover image per category.
-- ---------------------------------------------------------------------------
CREATE TABLE `categories` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(120) NOT NULL,
  `image_path` VARCHAR(512) NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_category_name` (`name`),
  KEY `idx_category_active_sort` (`is_active`, `sort_order`),
  CONSTRAINT `chk_category_active` CHECK (`is_active` IN (0,1))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 05. MENU ITEMS - only one image_path, NOT a menu-image gallery.
-- Keep historical items by disabling them; never delete referenced items.
-- ---------------------------------------------------------------------------
CREATE TABLE `menu_items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `category_id` BIGINT UNSIGNED NOT NULL,
  `sku` VARCHAR(50) NULL,
  `name` VARCHAR(150) NOT NULL,
  `description` VARCHAR(300) NULL,
  `dietary_type` ENUM('UNSPECIFIED','VEG','NON_VEG','EGG') NOT NULL DEFAULT 'UNSPECIFIED',
  `selling_price` DECIMAL(12,2) NOT NULL,
  `estimated_unit_cost` DECIMAL(12,2) NULL,
  `image_path` VARCHAR(512) NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `is_available` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_menu_item_sku` (`sku`),
  UNIQUE KEY `uq_menu_category_name` (`category_id`, `name`),
  KEY `idx_menu_billing` (`category_id`, `is_available`, `sort_order`),
  CONSTRAINT `fk_menu_category` FOREIGN KEY (`category_id`)
    REFERENCES `categories` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_menu_price` CHECK (`selling_price` >= 0),
  CONSTRAINT `chk_menu_cost` CHECK
    (`estimated_unit_cost` IS NULL OR `estimated_unit_cost` >= 0),
  CONSTRAINT `chk_menu_availability` CHECK (`is_available` IN (0,1))
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 06. BILL COUNTERS - atomic daily receipt numbering; only for PAID bills.
-- In a transaction, INSERT ON DUPLICATE KEY then increment with row locking.
-- Never derive the next bill number using SELECT MAX(bill_number).
-- ---------------------------------------------------------------------------
CREATE TABLE `bill_counters` (
  `business_date` DATE NOT NULL,
  `last_sequence` INT UNSIGNED NOT NULL DEFAULT 0,
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`business_date`)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 07. BILLS - one record for HELD, PAID, or CANCELLED (unpaid only).
-- No draft row is needed: ordinary in-progress cart can remain client-side.
-- Bill/receipt numbers are NULL until payment is finalized.
-- ---------------------------------------------------------------------------
CREATE TABLE `bills` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `bill_number` VARCHAR(40) NULL,
  `checkout_key` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  `business_date` DATE NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'HELD',
  `hold_label` VARCHAR(80) NULL,
  `item_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `subtotal_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `discount_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `round_off_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `total_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `note` VARCHAR(255) NULL,
  `store_name_snapshot` VARCHAR(120) NULL,
  `store_address_snapshot` VARCHAR(500) NULL,
  `store_phone_snapshot` VARCHAR(25) NULL,
  `created_by` TINYINT UNSIGNED NOT NULL,
  `paid_by` TINYINT UNSIGNED NULL,
  `cancelled_by` TINYINT UNSIGNED NULL,
  `cancel_reason` VARCHAR(255) NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `held_at` DATETIME(3) NULL,
  `paid_at` DATETIME(3) NULL,
  `cancelled_at` DATETIME(3) NULL,
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_bill_number` (`bill_number`),
  UNIQUE KEY `uq_checkout_key` (`checkout_key`),
  KEY `idx_bills_reporting` (`status`, `business_date`, `paid_at`),
  KEY `idx_bills_held` (`status`, `created_at`),
  KEY `idx_bills_user_date` (`created_by`, `business_date`),
  CONSTRAINT `fk_bill_creator` FOREIGN KEY (`created_by`)
    REFERENCES `users` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_bill_payer` FOREIGN KEY (`paid_by`)
    REFERENCES `users` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_bill_canceller` FOREIGN KEY (`cancelled_by`)
    REFERENCES `users` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_bill_status` CHECK (`status` IN ('HELD','PAID','CANCELLED')),
  CONSTRAINT `chk_bill_amounts` CHECK (
    `subtotal_amount` >= 0 AND `discount_amount` >= 0 AND
    `discount_amount` <= `subtotal_amount` AND
    `round_off_amount` BETWEEN -0.49 AND 0.50 AND
    (`round_off_amount` = 0 OR `total_amount` = ROUND(`subtotal_amount` - `discount_amount`, 0)) AND
    `total_amount` >= 0 AND
    `total_amount` = (`subtotal_amount` - `discount_amount` + `round_off_amount`)
  ),
  CONSTRAINT `chk_bill_paid_fields` CHECK (
    (`status` = 'PAID' AND `bill_number` IS NOT NULL
      AND `paid_at` IS NOT NULL AND `paid_by` IS NOT NULL
      AND `item_count` > 0 AND `total_amount` > 0
      AND `store_name_snapshot` IS NOT NULL)
    OR
    (`status` IN ('HELD','CANCELLED')
      AND `bill_number` IS NULL AND `paid_at` IS NULL)
  ),
  CONSTRAINT `chk_bill_cancel_fields` CHECK (
    (`status` = 'CANCELLED' AND `cancelled_at` IS NOT NULL
      AND `cancelled_by` IS NOT NULL)
    OR (`status` <> 'CANCELLED' AND `cancelled_at` IS NULL)
  )
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 08. BILL ITEMS - values captured at time of sale (price/cost/name snapshots).
-- gross and net line totals computed by MySQL; discounts are stored per line.
-- On checkout, header subtotal/discount/total must equal the sums of lines.
-- ---------------------------------------------------------------------------
CREATE TABLE `bill_items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `bill_id` BIGINT UNSIGNED NOT NULL,
  `menu_item_id` BIGINT UNSIGNED NOT NULL,
  `item_name_snapshot` VARCHAR(150) NOT NULL,
  `quantity` INT UNSIGNED NOT NULL,
  `unit_price` DECIMAL(12,2) NOT NULL,
  `estimated_unit_cost_snapshot` DECIMAL(12,2) NULL,
  `line_discount_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  `line_total` DECIMAL(14,2) GENERATED ALWAYS AS
    (`quantity` * `unit_price`) STORED,
  `line_net_total` DECIMAL(14,2) GENERATED ALWAYS AS
    ((`quantity` * `unit_price`) - `line_discount_amount`) STORED,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_bill_menu_item` (`bill_id`, `menu_item_id`),
  KEY `idx_billitem_menu` (`menu_item_id`),
  CONSTRAINT `fk_billitem_bill` FOREIGN KEY (`bill_id`)
    REFERENCES `bills` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_billitem_menu` FOREIGN KEY (`menu_item_id`)
    REFERENCES `menu_items` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_billitem_qty` CHECK (`quantity` > 0),
  CONSTRAINT `chk_billitem_price` CHECK (`unit_price` >= 0),
  CONSTRAINT `chk_billitem_discount` CHECK
    (`line_discount_amount` >= 0 AND `line_discount_amount` <= (`quantity` * `unit_price`)),
  CONSTRAINT `chk_billitem_cost` CHECK
    (`estimated_unit_cost_snapshot` IS NULL OR `estimated_unit_cost_snapshot` >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 09. BILL PAYMENTS - ONE payment method per bill (Cash / UPI / Card).
-- tendered_amount represents cash received; change = tendered - amount.
-- Payment and PAID bill must be created/updated in ONE DB transaction.
-- ---------------------------------------------------------------------------
CREATE TABLE `bill_payments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `bill_id` BIGINT UNSIGNED NOT NULL,
  `payment_method` VARCHAR(10) NOT NULL,
  `amount` DECIMAL(14,2) NOT NULL,
  `tendered_amount` DECIMAL(14,2) NOT NULL,
  `change_amount` DECIMAL(14,2) GENERATED ALWAYS AS
    (`tendered_amount` - `amount`) STORED,
  `reference_number` VARCHAR(100) NULL,
  `recorded_by` TINYINT UNSIGNED NOT NULL,
  `recorded_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_payment_bill` (`bill_id`),
  KEY `idx_payment_method_date` (`payment_method`, `recorded_at`),
  CONSTRAINT `fk_payment_bill` FOREIGN KEY (`bill_id`)
    REFERENCES `bills` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_payment_user` FOREIGN KEY (`recorded_by`)
    REFERENCES `users` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_payment_method` CHECK (`payment_method` IN ('CASH','UPI','CARD')),
  CONSTRAINT `chk_payment_amount` CHECK (
    `amount` > 0 AND `tendered_amount` >= `amount` AND
    (`payment_method` = 'CASH' OR `tendered_amount` = `amount`)
  )
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 10. INVENTORY MASTER - quantity in selected unit; weighted avg unit cost.
-- Do not directly overwrite current_qty from a dashboard save request.
-- ---------------------------------------------------------------------------
CREATE TABLE `inventory_items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(150) NOT NULL,
  `sku` VARCHAR(60) NULL,
  `unit` VARCHAR(16) NOT NULL,
  `current_qty` DECIMAL(14,3) NOT NULL DEFAULT 0.000,
  `min_limit_qty` DECIMAL(14,3) NOT NULL DEFAULT 0.000,
  `critical_limit_qty` DECIMAL(14,3) NOT NULL DEFAULT 0.000,
  `avg_unit_cost` DECIMAL(14,4) NOT NULL DEFAULT 0.0000,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_inventory_name` (`name`),
  UNIQUE KEY `uq_inventory_sku` (`sku`),
  KEY `idx_inventory_active_qty` (`is_active`, `current_qty`),
  CONSTRAINT `chk_inventory_unit` CHECK
    (`unit` IN ('PACK','KG','LITRE','PIECE')),
  CONSTRAINT `chk_inventory_qty` CHECK (
    `current_qty` >= 0 AND `min_limit_qty` >= 0 AND
    `critical_limit_qty` >= 0 AND `critical_limit_qty` <= `min_limit_qty` AND
    `avg_unit_cost` >= 0
  )
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 11. STOCK PURCHASE HEADERS - buying inventory is NOT immediate P&L expense.
-- ---------------------------------------------------------------------------
CREATE TABLE `stock_purchases` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `purchase_reference` VARCHAR(100) NULL,
  `supplier_name` VARCHAR(150) NULL,
  `business_date` DATE NOT NULL,
  `total_amount` DECIMAL(14,2) NOT NULL,
  `notes` VARCHAR(255) NULL,
  `created_by` TINYINT UNSIGNED NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_purchases_business_date` (`business_date`),
  CONSTRAINT `fk_stockpurchase_user` FOREIGN KEY (`created_by`)
    REFERENCES `users` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_stockpurchase_amount` CHECK (`total_amount` >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 12. STOCK PURCHASE LINES - the purchase's itemized contents.
-- ---------------------------------------------------------------------------
CREATE TABLE `stock_purchase_lines` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `purchase_id` BIGINT UNSIGNED NOT NULL,
  `inventory_item_id` BIGINT UNSIGNED NOT NULL,
  `quantity` DECIMAL(14,3) NOT NULL,
  `unit_cost` DECIMAL(14,4) NOT NULL,
  `line_total` DECIMAL(14,2) GENERATED ALWAYS AS
    (ROUND(`quantity` * `unit_cost`,2)) STORED,
  PRIMARY KEY (`id`),
  KEY `idx_purchase_line_purchase` (`purchase_id`),
  KEY `idx_purchase_line_item` (`inventory_item_id`),
  CONSTRAINT `fk_purchase_line_header` FOREIGN KEY (`purchase_id`)
    REFERENCES `stock_purchases` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_purchase_line_inventory` FOREIGN KEY (`inventory_item_id`)
    REFERENCES `inventory_items` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_purchase_line_qty` CHECK (`quantity` > 0),
  CONSTRAINT `chk_purchase_line_cost` CHECK (`unit_cost` >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 13. DAILY STOCK USAGE/WASTAGE ENTRIES - exactly the client-facing form.
-- A single entry creates 1 or 2 corresponding stock_movements atomically.
-- ---------------------------------------------------------------------------
CREATE TABLE `stock_daily_entries` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `request_key` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  `business_date` DATE NOT NULL,
  `inventory_item_id` BIGINT UNSIGNED NOT NULL,
  `used_qty` DECIMAL(14,3) NOT NULL DEFAULT 0.000,
  `wasted_qty` DECIMAL(14,3) NOT NULL DEFAULT 0.000,
  `notes` VARCHAR(255) NULL,
  `created_by` TINYINT UNSIGNED NOT NULL,
  `posted_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_daily_request_key` (`request_key`),
  KEY `idx_daily_entry_item_date` (`inventory_item_id`, `business_date`),
  KEY `idx_daily_entry_date` (`business_date`),
  CONSTRAINT `fk_daily_item` FOREIGN KEY (`inventory_item_id`)
    REFERENCES `inventory_items` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_daily_user` FOREIGN KEY (`created_by`)
    REFERENCES `users` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_daily_qty` CHECK (
    `used_qty` >= 0 AND `wasted_qty` >= 0 AND
    (`used_qty` + `wasted_qty`) > 0
  )
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 14. STOCK MOVEMENTS - immutable movement ledger; generated monetary value.
-- PURCHASE: +qty, FK purchase_line_id (one movement per purchase line)
-- DAILY_USE/WASTE: -qty, FK daily_entry_id (one of each per daily entry)
-- OPENING/ADJUSTMENT_IN: +qty; ADJUSTMENT_OUT: -qty
-- ---------------------------------------------------------------------------
CREATE TABLE `stock_movements` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `inventory_item_id` BIGINT UNSIGNED NOT NULL,
  `business_date` DATE NOT NULL,
  `movement_type` VARCHAR(20) NOT NULL,
  `quantity_delta` DECIMAL(14,3) NOT NULL,
  `cost_per_unit_snapshot` DECIMAL(14,4) NOT NULL,
  `cost_amount` DECIMAL(14,2) GENERATED ALWAYS AS
    (ROUND(ABS(`quantity_delta`) * `cost_per_unit_snapshot`,2)) STORED,
  `balance_after` DECIMAL(14,3) NOT NULL,
  `purchase_line_id` BIGINT UNSIGNED NULL,
  `daily_entry_id` BIGINT UNSIGNED NULL,
  `notes` VARCHAR(255) NULL,
  `recorded_by` TINYINT UNSIGNED NOT NULL,
  `recorded_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_movement_purchase_line` (`purchase_line_id`),
  UNIQUE KEY `uq_movement_daily_type` (`daily_entry_id`,`movement_type`),
  KEY `idx_mov_item_time` (`inventory_item_id`, `recorded_at`),
  KEY `idx_mov_date_type` (`business_date`, `movement_type`),
  CONSTRAINT `fk_movement_item` FOREIGN KEY (`inventory_item_id`)
    REFERENCES `inventory_items` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_movement_purchase_line` FOREIGN KEY (`purchase_line_id`)
    REFERENCES `stock_purchase_lines` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_movement_daily_entry` FOREIGN KEY (`daily_entry_id`)
    REFERENCES `stock_daily_entries` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_movement_actor` FOREIGN KEY (`recorded_by`)
    REFERENCES `users` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_movement_positive_cost` CHECK (
    `cost_per_unit_snapshot` >= 0 AND `balance_after` >= 0
  ),
  CONSTRAINT `chk_movement_source_and_sign` CHECK (
    (`movement_type` = 'PURCHASE' AND `quantity_delta` > 0
      AND `purchase_line_id` IS NOT NULL AND `daily_entry_id` IS NULL)
    OR
    (`movement_type` IN ('DAILY_USE','WASTE') AND `quantity_delta` < 0
      AND `daily_entry_id` IS NOT NULL AND `purchase_line_id` IS NULL)
    OR
    (`movement_type` IN ('OPENING','ADJUSTMENT_IN') AND `quantity_delta` > 0
      AND `purchase_line_id` IS NULL AND `daily_entry_id` IS NULL)
    OR
    (`movement_type` = 'ADJUSTMENT_OUT' AND `quantity_delta` < 0
      AND `purchase_line_id` IS NULL AND `daily_entry_id` IS NULL)
  )
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 15. OPERATING EXPENSES - recommended extension for operating P&L.
-- E.g. RENT, WAGES, ELECTRICITY, GAS, OTHER.
-- ---------------------------------------------------------------------------
CREATE TABLE `operating_expenses` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `business_date` DATE NOT NULL,
  `expense_category` VARCHAR(60) NOT NULL,
  `description` VARCHAR(255) NOT NULL,
  `amount` DECIMAL(14,2) NOT NULL,
  `created_by` TINYINT UNSIGNED NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_expense_date_type` (`business_date`, `expense_category`),
  CONSTRAINT `fk_expense_user` FOREIGN KEY (`created_by`)
    REFERENCES `users` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_expense_amount` CHECK (`amount` > 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 16. NOTIFICATIONS - one OPEN notification per stock item. Reopen on a
-- new threshold breach after prior alert is RESOLVED. Only admin can mark read.
-- ---------------------------------------------------------------------------
CREATE TABLE `notifications` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `inventory_item_id` BIGINT UNSIGNED NOT NULL,
  `severity` VARCHAR(12) NOT NULL,
  `status` VARCHAR(12) NOT NULL DEFAULT 'OPEN',
  `message` VARCHAR(255) NOT NULL,
  `triggered_qty` DECIMAL(14,3) NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `last_notified_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `read_by` TINYINT UNSIGNED NULL,
  `read_at` DATETIME(3) NULL,
  `resolved_at` DATETIME(3) NULL,
  `open_inventory_item_id` BIGINT UNSIGNED GENERATED ALWAYS AS
    (CASE WHEN `status` = 'OPEN' THEN `inventory_item_id` ELSE NULL END) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_one_open_stock_alert` (`open_inventory_item_id`),
  KEY `idx_notifications_status` (`status`, `created_at`),
  KEY `idx_notifications_inventory` (`inventory_item_id`, `created_at`),
  CONSTRAINT `fk_notification_inventory` FOREIGN KEY (`inventory_item_id`)
    REFERENCES `inventory_items` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_notification_reader` FOREIGN KEY (`read_by`)
    REFERENCES `users` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_notification_severity` CHECK (`severity` IN ('WARNING','CRITICAL')),
  CONSTRAINT `chk_notification_status` CHECK (`status` IN ('OPEN','RESOLVED')),
  CONSTRAINT `chk_notification_qty` CHECK (`triggered_qty` >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- 17. AUDIT LOGS - keep append-only; never store passwords, tokens or card PAN.
-- ---------------------------------------------------------------------------
CREATE TABLE `audit_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `actor_user_id` TINYINT UNSIGNED NULL,
  `action` VARCHAR(80) NOT NULL,
  `entity_type` VARCHAR(60) NOT NULL,
  `entity_id` VARCHAR(64) NULL,
  `old_data` JSON NULL,
  `new_data` JSON NULL,
  `ip_address` VARCHAR(45) NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_audit_entity` (`entity_type`, `entity_id`, `created_at`),
  KEY `idx_audit_actor` (`actor_user_id`, `created_at`),
  CONSTRAINT `fk_audit_user` FOREIGN KEY (`actor_user_id`)
    REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================================
-- REPORTING VIEWS
-- The daily operating estimate uses ACTUAL stock removed from inventory,
-- NOT the purchase invoice value (unused purchased stock remains an asset).
-- No double counting of inventory purchases as consumed inventory cost.
-- ============================================================================

CREATE VIEW `vw_daily_financial_summary` AS
SELECT
  `a`.`business_date`,
  SUM(`a`.`sales_revenue`) AS `sales_revenue`,
  SUM(`a`.`stock_used_cost`) AS `stock_used_cost`,
  SUM(`a`.`waste_cost`) AS `waste_cost`,
  SUM(`a`.`stock_adjustment_loss`) AS `stock_adjustment_loss`,
  SUM(`a`.`operating_expense`) AS `operating_expense`,
  (SUM(`a`.`sales_revenue`) - SUM(`a`.`stock_used_cost`)
     - SUM(`a`.`waste_cost`) - SUM(`a`.`stock_adjustment_loss`))
      AS `contribution_after_stock_cost`,
  (SUM(`a`.`sales_revenue`) - SUM(`a`.`stock_used_cost`)
     - SUM(`a`.`waste_cost`) - SUM(`a`.`stock_adjustment_loss`)
     - SUM(`a`.`operating_expense`)) AS `estimated_operating_profit`,
  ROUND(100 * SUM(`a`.`waste_cost`) /
    NULLIF(SUM(`a`.`stock_used_cost`) + SUM(`a`.`waste_cost`), 0), 2)
    AS `waste_ratio_percent`,
  ROUND(100 * (SUM(`a`.`sales_revenue`) - SUM(`a`.`stock_used_cost`)
     - SUM(`a`.`waste_cost`) - SUM(`a`.`stock_adjustment_loss`)
     - SUM(`a`.`operating_expense`)) /
    NULLIF(SUM(`a`.`sales_revenue`), 0), 2)
    AS `estimated_operating_margin_percent`
FROM (
  SELECT `business_date`, SUM(`total_amount`) AS `sales_revenue`,
         0.00 AS `stock_used_cost`, 0.00 AS `waste_cost`,
         0.00 AS `stock_adjustment_loss`, 0.00 AS `operating_expense`
  FROM `bills`
  WHERE `status` = 'PAID'
  GROUP BY `business_date`
  UNION ALL
  SELECT `business_date`, 0.00,
         SUM(CASE WHEN `movement_type` = 'DAILY_USE' THEN `cost_amount` ELSE 0 END),
         SUM(CASE WHEN `movement_type` = 'WASTE' THEN `cost_amount` ELSE 0 END),
         SUM(CASE WHEN `movement_type` = 'ADJUSTMENT_OUT' THEN `cost_amount` ELSE 0 END),
         0.00
  FROM `stock_movements`
  GROUP BY `business_date`
  UNION ALL
  SELECT `business_date`, 0.00, 0.00, 0.00, 0.00, SUM(`amount`)
  FROM `operating_expenses`
  GROUP BY `business_date`
) AS `a`
GROUP BY `a`.`business_date`;

CREATE VIEW `vw_daily_menu_item_sales` AS
SELECT
  `b`.`business_date`,
  `bi`.`menu_item_id`,
  MAX(`bi`.`item_name_snapshot`) AS `item_name`,
  SUM(`bi`.`quantity`) AS `quantity_sold`,
  SUM(`bi`.`line_net_total`) AS `sales_revenue`,
  SUM(CASE WHEN `bi`.`estimated_unit_cost_snapshot` IS NULL
       THEN `bi`.`quantity` ELSE 0 END) AS `quantity_missing_cost`,
  CASE WHEN SUM(CASE WHEN `bi`.`estimated_unit_cost_snapshot` IS NULL
                     THEN 1 ELSE 0 END) = 0
       THEN SUM(`bi`.`quantity` * `bi`.`estimated_unit_cost_snapshot`)
       ELSE NULL END AS `estimated_item_cost`,
  CASE WHEN SUM(CASE WHEN `bi`.`estimated_unit_cost_snapshot` IS NULL
                     THEN 1 ELSE 0 END) = 0
       THEN SUM(`bi`.`line_net_total`) -
            SUM(`bi`.`quantity` * `bi`.`estimated_unit_cost_snapshot`)
       ELSE NULL END AS `estimated_item_profit`
FROM `bills` AS `b`
INNER JOIN `bill_items` AS `bi` ON `bi`.`bill_id` = `b`.`id`
WHERE `b`.`status` = 'PAID'
GROUP BY `b`.`business_date`, `bi`.`menu_item_id`;

-- Seed the ONE store settings row (no credentials or demo sales data).
INSERT INTO `store_settings` (`id`,`store_name`,`address`,`phone`,`currency_code`,`business_timezone`)
VALUES (1, 'Sambar & Spicy', '', '', 'INR', 'Asia/Kolkata');

-- ============================================================================
-- BACKEND TRANSACTION / AUTH RULES (implementation requirements)
-- 1. Bootstrap exactly 3 users: (1, ADMIN), (2, BILLING), (3, READ_ONLY).
--    CHECK + UNIQUE prevents any fourth account, but app must forbid deleting
--    roles/users or opening signup; password hashes must be genuine BCrypt.
-- 2. ADMIN: all modules. BILLING: bills/menu read + hold/pay/print only.
--    READ_ONLY: GET/list only, no POST/PUT/PATCH/DELETE, export per policy.
--    Enforce in Spring Security AND service methods; never trust UI alone.
-- 3. A fresh in-progress cart stays on the client; persist HELD orders.
--    On payment: transaction + FOR UPDATE lock held bill (if any), validate
--    dish availability, re-price on server, insert bill_items / payment,
--    atomically assign a bill number, update bill -> PAID, COMMIT.
--    Use checkout_key to return existing bill on retries (idempotency).
-- 4. Validate SUM(line_total)=bill.subtotal_amount and SUM(line_discount_amount)
--    =bill.discount_amount; net lines plus round_off_amount equal bill.total_amount.
--    PAID bills/payments/items are immutable. Refund/reversal workflow is
--    NOT included in this v1 schema; do not silently edit/delete paid bills.
-- 5. For stock entry: lock inventory_items row SELECT ... FOR UPDATE,
--    reject overdraft, create source entry + movement(s), update cached
--    current_qty and weighted average cost, process alert, COMMIT.
-- 6. App validates stock movement source item/date/qty match its source
--    purchase/daily entry. Foreign keys alone cannot validate cross-row sums.
--    Enforce complete header/detail totals in the same transaction.
-- 7. Critical stock: qty <= critical_limit_qty; low: qty <= min_limit_qty;
--    healthy otherwise. If min=5 and critical=2, amber 3..5, red 0..2.
-- 8. Reports use business_date in Asia/Kolkata, with UTC timestamps.
--    Group daily/weekly/monthly/quarterly/yearly/lifetime via date ranges.
-- 9. Stock purchasing is inventory acquisition, not P&L stock consumption.
--    Daily usage and waste are valued at avg unit cost at deduction time.
--    Item-level profit is ESTIMATED from captured per-menu cost, and is not
--    allocated exactly from raw-ingredient stock (recipe/BOM is out of scope).
-- 10. Operating expenses are a recommended extension, not original MVP spec.
-- 11. Use proper VARCHAR images paths (one per dish), file-size/type checks,
--     server-side file validation, auth rate limit, backups and least privilege.
-- 12. Confirm local tax and receipt compliance with the store accountant.
-- ============================================================================

-- Post-install verification (run manually after creating the schema):
-- SELECT TABLE_NAME, TABLE_TYPE FROM information_schema.TABLES
-- WHERE TABLE_SCHEMA = 'sambar_spicy' ORDER BY TABLE_TYPE,TABLE_NAME;
-- SHOW TABLES FROM `sambar_spicy`;
-- SELECT * FROM `store_settings`;

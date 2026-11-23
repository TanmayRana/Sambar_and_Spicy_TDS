-- ============================================================================
-- SAMBAR & SPICY | FLYWAY V1 INITIAL SCHEMA MIGRATION (POSTGRESQL DIALECT)
-- Target: PostgreSQL 15+ / Neon Serverless Postgres
-- Schema: 17 application tables + 2 reporting views + store_settings singleton
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 01. USERS - no signup; only ID 1/2/3 with permanently assigned roles.
-- ---------------------------------------------------------------------------
CREATE TABLE users (
  id SMALLINT NOT NULL,
  role VARCHAR(16) NOT NULL,
  username VARCHAR(80) NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  is_active SMALLINT NOT NULL DEFAULT 1,
  last_login_at TIMESTAMPTZ NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_users_role UNIQUE (role),
  CONSTRAINT uq_users_username UNIQUE (username),
  CONSTRAINT chk_users_fixed_slots CHECK (
    (id = 1 AND role = 'ADMIN') OR
    (id = 2 AND role = 'BILLING') OR
    (id = 3 AND role = 'READ_ONLY')
  ),
  CONSTRAINT chk_users_active CHECK (is_active IN (0,1))
);

-- ---------------------------------------------------------------------------
-- 02. AUTH REFRESH TOKENS - hash only, no plaintext session/refresh tokens.
-- ---------------------------------------------------------------------------
CREATE TABLE auth_refresh_tokens (
  id BIGSERIAL NOT NULL,
  user_id SMALLINT NOT NULL,
  token_hash CHAR(64) NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash),
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE RESTRICT
);
CREATE INDEX idx_refresh_user_expiry ON auth_refresh_tokens (user_id, expires_at);

-- ---------------------------------------------------------------------------
-- 03. STORE SETTINGS - single row (id=1).
-- ---------------------------------------------------------------------------
CREATE TABLE store_settings (
  id SMALLINT NOT NULL,
  store_name VARCHAR(120) NOT NULL,
  address VARCHAR(500) NOT NULL DEFAULT '',
  phone VARCHAR(25) NOT NULL DEFAULT '',
  currency_code CHAR(3) NOT NULL DEFAULT 'INR',
  business_timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Kolkata',
  receipt_footer VARCHAR(255) NULL,
  updated_by SMALLINT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT chk_settings_singleton CHECK (id = 1),
  CONSTRAINT fk_settings_updated_by FOREIGN KEY (updated_by)
    REFERENCES users (id) ON DELETE SET NULL
);

-- ---------------------------------------------------------------------------
-- 04. CATEGORIES - one optional cover image per category.
-- ---------------------------------------------------------------------------
CREATE TABLE categories (
  id BIGSERIAL NOT NULL,
  name VARCHAR(120) NOT NULL,
  image_path VARCHAR(512) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  is_active SMALLINT NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_category_name UNIQUE (name),
  CONSTRAINT chk_category_active CHECK (is_active IN (0,1))
);
CREATE INDEX idx_category_active_sort ON categories (is_active, sort_order);

-- ---------------------------------------------------------------------------
-- 05. MENU ITEMS
-- ---------------------------------------------------------------------------
CREATE TABLE menu_items (
  id BIGSERIAL NOT NULL,
  category_id BIGINT NOT NULL,
  sku VARCHAR(50) NULL,
  name VARCHAR(150) NOT NULL,
  description VARCHAR(300) NULL,
  selling_price NUMERIC(12,2) NOT NULL,
  estimated_unit_cost NUMERIC(12,2) NULL,
  image_path VARCHAR(512) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  is_available SMALLINT NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_menu_item_sku UNIQUE (sku),
  CONSTRAINT uq_menu_category_name UNIQUE (category_id, name),
  CONSTRAINT fk_menu_category FOREIGN KEY (category_id)
    REFERENCES categories (id) ON DELETE RESTRICT,
  CONSTRAINT chk_menu_price CHECK (selling_price >= 0),
  CONSTRAINT chk_menu_cost CHECK (estimated_unit_cost IS NULL OR estimated_unit_cost >= 0),
  CONSTRAINT chk_menu_availability CHECK (is_available IN (0,1))
);
CREATE INDEX idx_menu_billing ON menu_items (category_id, is_available, sort_order);

-- ---------------------------------------------------------------------------
-- 06. BILL COUNTERS - atomic daily receipt numbering; only for PAID bills.
-- ---------------------------------------------------------------------------
CREATE TABLE bill_counters (
  business_date DATE NOT NULL,
  last_sequence INT NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (business_date)
);

-- ---------------------------------------------------------------------------
-- 07. BILLS - one record for HELD, PAID, or CANCELLED (unpaid only).
-- ---------------------------------------------------------------------------
CREATE TABLE bills (
  id BIGSERIAL NOT NULL,
  bill_number VARCHAR(40) NULL,
  checkout_key CHAR(36) NULL,
  business_date DATE NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'HELD',
  hold_label VARCHAR(80) NULL,
  item_count INT NOT NULL DEFAULT 0,
  subtotal_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
  discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
  total_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
  note VARCHAR(255) NULL,
  store_name_snapshot VARCHAR(120) NULL,
  store_address_snapshot VARCHAR(500) NULL,
  store_phone_snapshot VARCHAR(25) NULL,
  created_by SMALLINT NOT NULL,
  paid_by SMALLINT NULL,
  cancelled_by SMALLINT NULL,
  cancel_reason VARCHAR(255) NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  held_at TIMESTAMPTZ NULL,
  paid_at TIMESTAMPTZ NULL,
  cancelled_at TIMESTAMPTZ NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_bill_number UNIQUE (bill_number),
  CONSTRAINT uq_checkout_key UNIQUE (checkout_key),
  CONSTRAINT fk_bill_creator FOREIGN KEY (created_by)
    REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT fk_bill_payer FOREIGN KEY (paid_by)
    REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT fk_bill_canceller FOREIGN KEY (cancelled_by)
    REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT chk_bill_status CHECK (status IN ('HELD','PAID','CANCELLED')),
  CONSTRAINT chk_bill_amounts CHECK (
    subtotal_amount >= 0 AND discount_amount >= 0 AND
    discount_amount <= subtotal_amount AND
    total_amount = (subtotal_amount - discount_amount)
  ),
  CONSTRAINT chk_bill_paid_fields CHECK (
    (status = 'PAID' AND bill_number IS NOT NULL
      AND paid_at IS NOT NULL AND paid_by IS NOT NULL
      AND item_count > 0 AND total_amount > 0
      AND store_name_snapshot IS NOT NULL)
    OR
    (status IN ('HELD','CANCELLED')
      AND bill_number IS NULL AND paid_at IS NULL)
  ),
  CONSTRAINT chk_bill_cancel_fields CHECK (
    (status = 'CANCELLED' AND cancelled_at IS NOT NULL
      AND cancelled_by IS NOT NULL)
    OR (status <> 'CANCELLED' AND cancelled_at IS NULL)
  )
);
CREATE INDEX idx_bills_reporting ON bills (status, business_date, paid_at);
CREATE INDEX idx_bills_held ON bills (status, created_at);
CREATE INDEX idx_bills_user_date ON bills (created_by, business_date);

-- ---------------------------------------------------------------------------
-- 08. BILL ITEMS - values captured at time of sale.
-- ---------------------------------------------------------------------------
CREATE TABLE bill_items (
  id BIGSERIAL NOT NULL,
  bill_id BIGINT NOT NULL,
  menu_item_id BIGINT NOT NULL,
  item_name_snapshot VARCHAR(150) NOT NULL,
  quantity INT NOT NULL,
  unit_price NUMERIC(12,2) NOT NULL,
  estimated_unit_cost_snapshot NUMERIC(12,2) NULL,
  line_discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
  line_total NUMERIC(14,2) GENERATED ALWAYS AS (quantity * unit_price) STORED,
  line_net_total NUMERIC(14,2) GENERATED ALWAYS AS ((quantity * unit_price) - line_discount_amount) STORED,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_bill_menu_item UNIQUE (bill_id, menu_item_id),
  CONSTRAINT fk_billitem_bill FOREIGN KEY (bill_id)
    REFERENCES bills (id) ON DELETE RESTRICT,
  CONSTRAINT fk_billitem_menu FOREIGN KEY (menu_item_id)
    REFERENCES menu_items (id) ON DELETE RESTRICT,
  CONSTRAINT chk_billitem_qty CHECK (quantity > 0),
  CONSTRAINT chk_billitem_price CHECK (unit_price >= 0),
  CONSTRAINT chk_billitem_discount CHECK (line_discount_amount >= 0 AND line_discount_amount <= (quantity * unit_price)),
  CONSTRAINT chk_billitem_cost CHECK (estimated_unit_cost_snapshot IS NULL OR estimated_unit_cost_snapshot >= 0)
);
CREATE INDEX idx_billitem_menu ON bill_items (menu_item_id);

-- ---------------------------------------------------------------------------
-- 09. BILL PAYMENTS - ONE payment method per bill (Cash / UPI / Card).
-- ---------------------------------------------------------------------------
CREATE TABLE bill_payments (
  id BIGSERIAL NOT NULL,
  bill_id BIGINT NOT NULL,
  payment_method VARCHAR(10) NOT NULL,
  amount NUMERIC(14,2) NOT NULL,
  tendered_amount NUMERIC(14,2) NOT NULL,
  change_amount NUMERIC(14,2) GENERATED ALWAYS AS (tendered_amount - amount) STORED,
  reference_number VARCHAR(100) NULL,
  recorded_by SMALLINT NOT NULL,
  recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_payment_bill UNIQUE (bill_id),
  CONSTRAINT fk_payment_bill FOREIGN KEY (bill_id)
    REFERENCES bills (id) ON DELETE RESTRICT,
  CONSTRAINT fk_payment_user FOREIGN KEY (recorded_by)
    REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT chk_payment_method CHECK (payment_method IN ('CASH','UPI','CARD')),
  CONSTRAINT chk_payment_amount CHECK (
    amount > 0 AND tendered_amount >= amount AND
    (payment_method = 'CASH' OR tendered_amount = amount)
  )
);
CREATE INDEX idx_payment_method_date ON bill_payments (payment_method, recorded_at);

-- ---------------------------------------------------------------------------
-- 10. INVENTORY MASTER
-- ---------------------------------------------------------------------------
CREATE TABLE inventory_items (
  id BIGSERIAL NOT NULL,
  name VARCHAR(150) NOT NULL,
  sku VARCHAR(60) NULL,
  unit VARCHAR(16) NOT NULL,
  current_qty NUMERIC(14,3) NOT NULL DEFAULT 0.000,
  min_limit_qty NUMERIC(14,3) NOT NULL DEFAULT 0.000,
  critical_limit_qty NUMERIC(14,3) NOT NULL DEFAULT 0.000,
  avg_unit_cost NUMERIC(14,4) NOT NULL DEFAULT 0.0000,
  is_active SMALLINT NOT NULL DEFAULT 1,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_inventory_name UNIQUE (name),
  CONSTRAINT uq_inventory_sku UNIQUE (sku),
  CONSTRAINT chk_inventory_unit CHECK (unit IN ('PACK','KG','LITRE','PIECE')),
  CONSTRAINT chk_inventory_qty CHECK (
    current_qty >= 0 AND min_limit_qty >= 0 AND
    critical_limit_qty >= 0 AND critical_limit_qty <= min_limit_qty AND
    avg_unit_cost >= 0
  )
);
CREATE INDEX idx_inventory_active_qty ON inventory_items (is_active, current_qty);

-- ---------------------------------------------------------------------------
-- 11. STOCK PURCHASE HEADERS
-- ---------------------------------------------------------------------------
CREATE TABLE stock_purchases (
  id BIGSERIAL NOT NULL,
  purchase_reference VARCHAR(100) NULL,
  supplier_name VARCHAR(150) NULL,
  business_date DATE NOT NULL,
  total_amount NUMERIC(14,2) NOT NULL,
  notes VARCHAR(255) NULL,
  created_by SMALLINT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_stockpurchase_user FOREIGN KEY (created_by)
    REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT chk_stockpurchase_amount CHECK (total_amount >= 0)
);
CREATE INDEX idx_purchases_business_date ON stock_purchases (business_date);

-- ---------------------------------------------------------------------------
-- 12. STOCK PURCHASE LINES
-- ---------------------------------------------------------------------------
CREATE TABLE stock_purchase_lines (
  id BIGSERIAL NOT NULL,
  purchase_id BIGINT NOT NULL,
  inventory_item_id BIGINT NOT NULL,
  quantity NUMERIC(14,3) NOT NULL,
  unit_cost NUMERIC(14,4) NOT NULL,
  line_total NUMERIC(14,2) GENERATED ALWAYS AS (ROUND(quantity * unit_cost, 2)) STORED,
  PRIMARY KEY (id),
  CONSTRAINT fk_purchase_line_header FOREIGN KEY (purchase_id)
    REFERENCES stock_purchases (id) ON DELETE RESTRICT,
  CONSTRAINT fk_purchase_line_inventory FOREIGN KEY (inventory_item_id)
    REFERENCES inventory_items (id) ON DELETE RESTRICT,
  CONSTRAINT chk_purchase_line_qty CHECK (quantity > 0),
  CONSTRAINT chk_purchase_line_cost CHECK (unit_cost >= 0)
);
CREATE INDEX idx_purchase_line_purchase ON stock_purchase_lines (purchase_id);
CREATE INDEX idx_purchase_line_item ON stock_purchase_lines (inventory_item_id);

-- ---------------------------------------------------------------------------
-- 13. DAILY STOCK USAGE/WASTAGE ENTRIES
-- ---------------------------------------------------------------------------
CREATE TABLE stock_daily_entries (
  id BIGSERIAL NOT NULL,
  request_key CHAR(36) NULL,
  business_date DATE NOT NULL,
  inventory_item_id BIGINT NOT NULL,
  used_qty NUMERIC(14,3) NOT NULL DEFAULT 0.000,
  wasted_qty NUMERIC(14,3) NOT NULL DEFAULT 0.000,
  notes VARCHAR(255) NULL,
  created_by SMALLINT NOT NULL,
  posted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_daily_request_key UNIQUE (request_key),
  CONSTRAINT fk_daily_item FOREIGN KEY (inventory_item_id)
    REFERENCES inventory_items (id) ON DELETE RESTRICT,
  CONSTRAINT fk_daily_user FOREIGN KEY (created_by)
    REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT chk_daily_qty CHECK (
    used_qty >= 0 AND wasted_qty >= 0 AND (used_qty + wasted_qty) > 0
  )
);
CREATE INDEX idx_daily_entry_item_date ON stock_daily_entries (inventory_item_id, business_date);
CREATE INDEX idx_daily_entry_date ON stock_daily_entries (business_date);

-- ---------------------------------------------------------------------------
-- 14. STOCK MOVEMENTS - immutable movement ledger
-- ---------------------------------------------------------------------------
CREATE TABLE stock_movements (
  id BIGSERIAL NOT NULL,
  inventory_item_id BIGINT NOT NULL,
  business_date DATE NOT NULL,
  movement_type VARCHAR(20) NOT NULL,
  quantity_delta NUMERIC(14,3) NOT NULL,
  cost_per_unit_snapshot NUMERIC(14,4) NOT NULL,
  cost_amount NUMERIC(14,2) GENERATED ALWAYS AS (ROUND(ABS(quantity_delta) * cost_per_unit_snapshot, 2)) STORED,
  balance_after NUMERIC(14,3) NOT NULL,
  purchase_line_id BIGINT NULL,
  daily_entry_id BIGINT NULL,
  notes VARCHAR(255) NULL,
  recorded_by SMALLINT NOT NULL,
  recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_movement_purchase_line UNIQUE (purchase_line_id),
  CONSTRAINT uq_movement_daily_type UNIQUE (daily_entry_id, movement_type),
  CONSTRAINT fk_movement_item FOREIGN KEY (inventory_item_id)
    REFERENCES inventory_items (id) ON DELETE RESTRICT,
  CONSTRAINT fk_movement_purchase_line FOREIGN KEY (purchase_line_id)
    REFERENCES stock_purchase_lines (id) ON DELETE RESTRICT,
  CONSTRAINT fk_movement_daily_entry FOREIGN KEY (daily_entry_id)
    REFERENCES stock_daily_entries (id) ON DELETE RESTRICT,
  CONSTRAINT fk_movement_actor FOREIGN KEY (recorded_by)
    REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT chk_movement_positive_cost CHECK (cost_per_unit_snapshot >= 0 AND balance_after >= 0),
  CONSTRAINT chk_movement_source_and_sign CHECK (
    (movement_type = 'PURCHASE' AND quantity_delta > 0
      AND purchase_line_id IS NOT NULL AND daily_entry_id IS NULL)
    OR
    (movement_type IN ('DAILY_USE','WASTE') AND quantity_delta < 0
      AND daily_entry_id IS NOT NULL AND purchase_line_id IS NULL)
    OR
    (movement_type IN ('OPENING','ADJUSTMENT_IN') AND quantity_delta > 0
      AND purchase_line_id IS NULL AND daily_entry_id IS NULL)
    OR
    (movement_type = 'ADJUSTMENT_OUT' AND quantity_delta < 0
      AND purchase_line_id IS NULL AND daily_entry_id IS NULL)
  )
);
CREATE INDEX idx_mov_item_time ON stock_movements (inventory_item_id, recorded_at);
CREATE INDEX idx_mov_date_type ON stock_movements (business_date, movement_type);

-- ---------------------------------------------------------------------------
-- 15. OPERATING EXPENSES
-- ---------------------------------------------------------------------------
CREATE TABLE operating_expenses (
  id BIGSERIAL NOT NULL,
  business_date DATE NOT NULL,
  expense_category VARCHAR(60) NOT NULL,
  description VARCHAR(255) NOT NULL,
  amount NUMERIC(14,2) NOT NULL,
  created_by SMALLINT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_expense_user FOREIGN KEY (created_by)
    REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT chk_expense_amount CHECK (amount > 0)
);
CREATE INDEX idx_expense_date_type ON operating_expenses (business_date, expense_category);

-- ---------------------------------------------------------------------------
-- 16. NOTIFICATIONS
-- ---------------------------------------------------------------------------
CREATE TABLE notifications (
  id BIGSERIAL NOT NULL,
  inventory_item_id BIGINT NOT NULL,
  severity VARCHAR(12) NOT NULL,
  status VARCHAR(12) NOT NULL DEFAULT 'OPEN',
  message VARCHAR(255) NOT NULL,
  triggered_qty NUMERIC(14,3) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_notified_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  read_by SMALLINT NULL,
  read_at TIMESTAMPTZ NULL,
  resolved_at TIMESTAMPTZ NULL,
  open_inventory_item_id BIGINT GENERATED ALWAYS AS (
    CASE WHEN status = 'OPEN' THEN inventory_item_id ELSE NULL END
  ) STORED,
  PRIMARY KEY (id),
  CONSTRAINT uq_one_open_stock_alert UNIQUE (open_inventory_item_id),
  CONSTRAINT fk_notification_inventory FOREIGN KEY (inventory_item_id)
    REFERENCES inventory_items (id) ON DELETE RESTRICT,
  CONSTRAINT fk_notification_reader FOREIGN KEY (read_by)
    REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT chk_notification_severity CHECK (severity IN ('WARNING','CRITICAL')),
  CONSTRAINT chk_notification_status CHECK (status IN ('OPEN','RESOLVED')),
  CONSTRAINT chk_notification_qty CHECK (triggered_qty >= 0)
);
CREATE INDEX idx_notifications_status ON notifications (status, created_at);
CREATE INDEX idx_notifications_inventory ON notifications (inventory_item_id, created_at);

-- ---------------------------------------------------------------------------
-- 17. AUDIT LOGS
-- ---------------------------------------------------------------------------
CREATE TABLE audit_logs (
  id BIGSERIAL NOT NULL,
  actor_user_id SMALLINT NULL,
  action VARCHAR(80) NOT NULL,
  entity_type VARCHAR(60) NOT NULL,
  entity_id VARCHAR(64) NULL,
  old_data JSONB NULL,
  new_data JSONB NULL,
  ip_address VARCHAR(45) NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_audit_user FOREIGN KEY (actor_user_id)
    REFERENCES users (id) ON DELETE SET NULL
);
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id, created_at);
CREATE INDEX idx_audit_actor ON audit_logs (actor_user_id, created_at);

-- ============================================================================
-- REPORTING VIEWS
-- ============================================================================

CREATE VIEW vw_daily_financial_summary AS
SELECT
  a.business_date,
  SUM(a.sales_revenue) AS sales_revenue,
  SUM(a.stock_used_cost) AS stock_used_cost,
  SUM(a.waste_cost) AS waste_cost,
  SUM(a.stock_adjustment_loss) AS stock_adjustment_loss,
  SUM(a.operating_expense) AS operating_expense,
  (SUM(a.sales_revenue) - SUM(a.stock_used_cost)
     - SUM(a.waste_cost) - SUM(a.stock_adjustment_loss))
      AS contribution_after_stock_cost,
  (SUM(a.sales_revenue) - SUM(a.stock_used_cost)
     - SUM(a.waste_cost) - SUM(a.stock_adjustment_loss)
     - SUM(a.operating_expense)) AS estimated_operating_profit,
  ROUND(100.0 * SUM(a.waste_cost) /
    NULLIF(SUM(a.stock_used_cost) + SUM(a.waste_cost), 0), 2)
    AS waste_ratio_percent,
  ROUND(100.0 * (SUM(a.sales_revenue) - SUM(a.stock_used_cost)
     - SUM(a.waste_cost) - SUM(a.stock_adjustment_loss)
     - SUM(a.operating_expense)) /
    NULLIF(SUM(a.sales_revenue), 0), 2)
    AS estimated_operating_margin_percent
FROM (
  SELECT business_date, SUM(total_amount) AS sales_revenue,
         0.00 AS stock_used_cost, 0.00 AS waste_cost,
         0.00 AS stock_adjustment_loss, 0.00 AS operating_expense
  FROM bills
  WHERE status = 'PAID'
  GROUP BY business_date
  UNION ALL
  SELECT business_date, 0.00,
         SUM(CASE WHEN movement_type = 'DAILY_USE' THEN cost_amount ELSE 0 END),
         SUM(CASE WHEN movement_type = 'WASTE' THEN cost_amount ELSE 0 END),
         SUM(CASE WHEN movement_type = 'ADJUSTMENT_OUT' THEN cost_amount ELSE 0 END),
         0.00
  FROM stock_movements
  GROUP BY business_date
  UNION ALL
  SELECT business_date, 0.00, 0.00, 0.00, 0.00, SUM(amount)
  FROM operating_expenses
  GROUP BY business_date
) AS a
GROUP BY a.business_date;

CREATE VIEW vw_daily_menu_item_sales AS
SELECT
  b.business_date,
  bi.menu_item_id,
  MAX(bi.item_name_snapshot) AS item_name,
  SUM(bi.quantity) AS quantity_sold,
  SUM(bi.line_net_total) AS sales_revenue,
  SUM(CASE WHEN bi.estimated_unit_cost_snapshot IS NULL
       THEN bi.quantity ELSE 0 END) AS quantity_missing_cost,
  CASE WHEN SUM(CASE WHEN bi.estimated_unit_cost_snapshot IS NULL
                     THEN 1 ELSE 0 END) = 0
       THEN SUM(bi.quantity * bi.estimated_unit_cost_snapshot)
       ELSE NULL END AS estimated_item_cost,
  CASE WHEN SUM(CASE WHEN bi.estimated_unit_cost_snapshot IS NULL
                     THEN 1 ELSE 0 END) = 0
       THEN SUM(bi.line_net_total) -
            SUM(bi.quantity * bi.estimated_unit_cost_snapshot)
       ELSE NULL END AS estimated_item_profit
FROM bills AS b
INNER JOIN bill_items AS bi ON bi.bill_id = b.id
WHERE b.status = 'PAID'
GROUP BY b.business_date, bi.menu_item_id;

-- Seed the ONE store settings row.
INSERT INTO store_settings (id, store_name, address, phone, currency_code, business_timezone)
VALUES (1, 'Sambar & Spicy', '', '', 'INR', 'Asia/Kolkata')
ON CONFLICT (id) DO NOTHING;

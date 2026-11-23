-- ============================================================================
-- SAMBAR & SPICY — FLYWAY V5 MIGRATION: BOOLEAN COLUMN TYPES FOR POSTGRESQL
-- Aligns PostgreSQL native boolean columns with Java Boolean entity mappings
-- ============================================================================

-- users.is_active
ALTER TABLE users 
  DROP CONSTRAINT IF EXISTS chk_users_active;

ALTER TABLE users 
  ALTER COLUMN is_active DROP DEFAULT,
  ALTER COLUMN is_active TYPE BOOLEAN USING (is_active = 1),
  ALTER COLUMN is_active SET DEFAULT true;

-- categories.is_active
ALTER TABLE categories 
  DROP CONSTRAINT IF EXISTS chk_category_active;

ALTER TABLE categories 
  ALTER COLUMN is_active DROP DEFAULT,
  ALTER COLUMN is_active TYPE BOOLEAN USING (is_active = 1),
  ALTER COLUMN is_active SET DEFAULT true;

-- menu_items.is_available
ALTER TABLE menu_items 
  DROP CONSTRAINT IF EXISTS chk_menu_availability;

ALTER TABLE menu_items 
  ALTER COLUMN is_available DROP DEFAULT,
  ALTER COLUMN is_available TYPE BOOLEAN USING (is_available = 1),
  ALTER COLUMN is_available SET DEFAULT true;

-- menu_items.has_half_portion
ALTER TABLE menu_items 
  DROP CONSTRAINT IF EXISTS chk_menu_half_price;

ALTER TABLE menu_items 
  ALTER COLUMN has_half_portion DROP DEFAULT,
  ALTER COLUMN has_half_portion TYPE BOOLEAN USING (has_half_portion = 1),
  ALTER COLUMN has_half_portion SET DEFAULT false,
  ADD CONSTRAINT chk_menu_half_price CHECK (
    has_half_portion = false OR (half_selling_price IS NOT NULL AND half_selling_price >= 0)
  );

-- inventory_items.is_active
ALTER TABLE inventory_items 
  ALTER COLUMN is_active DROP DEFAULT,
  ALTER COLUMN is_active TYPE BOOLEAN USING (is_active = 1),
  ALTER COLUMN is_active SET DEFAULT true;

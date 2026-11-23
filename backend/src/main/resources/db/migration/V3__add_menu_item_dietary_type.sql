-- ============================================================================
-- SAMBAR & SPICY — FLYWAY V3 MIGRATION: DIETARY TYPE (POSTGRESQL DIALECT)
-- ============================================================================

ALTER TABLE menu_items
  ADD COLUMN dietary_type VARCHAR(20) NOT NULL DEFAULT 'UNSPECIFIED',
  ADD CONSTRAINT chk_menu_dietary_type CHECK (dietary_type IN ('UNSPECIFIED','VEG','NON_VEG','EGG'));

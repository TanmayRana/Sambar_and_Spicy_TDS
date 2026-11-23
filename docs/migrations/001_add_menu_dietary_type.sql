-- Run once on an existing database after backing it up. Existing dishes remain unclassified.
ALTER TABLE `menu_items`
  ADD COLUMN `dietary_type` ENUM('UNSPECIFIED','VEG','NON_VEG','EGG') NOT NULL DEFAULT 'UNSPECIFIED' AFTER `description`;

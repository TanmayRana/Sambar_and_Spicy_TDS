-- Existing dishes have no trustworthy classification and must be reviewed.
ALTER TABLE `menu_items`
  ADD COLUMN `dietary_type` ENUM('UNSPECIFIED','VEG','NON_VEG','EGG')
    NOT NULL DEFAULT 'UNSPECIFIED' AFTER `description`;

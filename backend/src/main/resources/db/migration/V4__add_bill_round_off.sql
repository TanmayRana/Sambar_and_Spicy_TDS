-- Existing bills retain their original totals; only future checkout can set an adjustment.
ALTER TABLE `bills`
  DROP CHECK `chk_bill_amounts`,
  ADD COLUMN `round_off_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00 AFTER `discount_amount`,
  ADD CONSTRAINT `chk_bill_amounts` CHECK (
    `subtotal_amount` >= 0 AND `discount_amount` >= 0 AND
    `discount_amount` <= `subtotal_amount` AND
    `round_off_amount` BETWEEN -0.49 AND 0.50 AND
    (`round_off_amount` = 0 OR `total_amount` = ROUND(`subtotal_amount` - `discount_amount`, 0)) AND
    `total_amount` >= 0 AND
    `total_amount` = (`subtotal_amount` - `discount_amount` + `round_off_amount`)
  );

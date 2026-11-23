-- ============================================================================
-- SAMBAR & SPICY — FLYWAY V6 MIGRATION: VIEW TYPES ALIGNMENT FOR POSTGRESQL
-- Casts view calculation columns to NUMERIC to align with Java BigDecimal
-- ============================================================================

DROP VIEW IF EXISTS vw_daily_menu_item_sales;
DROP VIEW IF EXISTS vw_daily_financial_summary;

CREATE VIEW vw_daily_financial_summary AS
SELECT
  a.business_date,
  CAST(SUM(a.sales_revenue) AS NUMERIC(14,2)) AS sales_revenue,
  CAST(SUM(a.stock_used_cost) AS NUMERIC(14,2)) AS stock_used_cost,
  CAST(SUM(a.waste_cost) AS NUMERIC(14,2)) AS waste_cost,
  CAST(SUM(a.stock_adjustment_loss) AS NUMERIC(14,2)) AS stock_adjustment_loss,
  CAST(SUM(a.operating_expense) AS NUMERIC(14,2)) AS operating_expense,
  CAST((SUM(a.sales_revenue) - SUM(a.stock_used_cost)
     - SUM(a.waste_cost) - SUM(a.stock_adjustment_loss)) AS NUMERIC(14,2))
      AS contribution_after_stock_cost,
  CAST((SUM(a.sales_revenue) - SUM(a.stock_used_cost)
     - SUM(a.waste_cost) - SUM(a.stock_adjustment_loss)
     - SUM(a.operating_expense)) AS NUMERIC(14,2)) AS estimated_operating_profit,
  CAST(ROUND(100.0 * SUM(a.waste_cost) /
    NULLIF(SUM(a.stock_used_cost) + SUM(a.waste_cost), 0), 2) AS NUMERIC(14,2))
    AS waste_ratio_percent,
  CAST(ROUND(100.0 * (SUM(a.sales_revenue) - SUM(a.stock_used_cost)
     - SUM(a.waste_cost) - SUM(a.stock_adjustment_loss)
     - SUM(a.operating_expense)) /
    NULLIF(SUM(a.sales_revenue), 0), 2) AS NUMERIC(14,2))
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
  CAST(SUM(bi.quantity) AS NUMERIC(14,2)) AS quantity_sold,
  CAST(SUM(bi.line_net_total) AS NUMERIC(14,2)) AS sales_revenue,
  CAST(SUM(CASE WHEN bi.estimated_unit_cost_snapshot IS NULL
       THEN bi.quantity ELSE 0 END) AS NUMERIC(14,2)) AS quantity_missing_cost,
  CAST(CASE WHEN SUM(CASE WHEN bi.estimated_unit_cost_snapshot IS NULL
                     THEN 1 ELSE 0 END) = 0
       THEN SUM(bi.quantity * bi.estimated_unit_cost_snapshot)
       ELSE NULL END AS NUMERIC(14,2)) AS estimated_item_cost,
  CAST(CASE WHEN SUM(CASE WHEN bi.estimated_unit_cost_snapshot IS NULL
                     THEN 1 ELSE 0 END) = 0
       THEN SUM(bi.line_net_total) -
            SUM(bi.quantity * bi.estimated_unit_cost_snapshot)
       ELSE NULL END AS NUMERIC(14,2)) AS estimated_item_profit
FROM bills AS b
INNER JOIN bill_items AS bi ON bi.bill_id = b.id
WHERE b.status = 'PAID'
GROUP BY b.business_date, bi.menu_item_id;

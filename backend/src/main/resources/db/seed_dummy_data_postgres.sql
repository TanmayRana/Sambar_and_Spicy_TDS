-- ==============================================================================
-- SAMBAR & SPICY — POSTGRESQL (NEON) FULL SEED SCRIPT (50+ DATASETS)
-- Covers ALL 17 Tables + Daily Sales Orders, Stock Movements, Expenses & Alerts
-- Compatible with PostgreSQL 15+ / Neon Serverless Postgres
-- ==============================================================================

-- 1. Ensure Store Settings singleton row (id=1)
INSERT INTO store_settings (id, store_name, address, phone, currency_code, business_timezone, receipt_footer, updated_by, updated_at)
VALUES (
  1,
  'Sambar & Spicy - South Indian Delights',
  'Shop 4 & 5, Annapoorna Arcade, MG Road, Indiranagar, Bengaluru, Karnataka 560038',
  '+91 80 4123 4567',
  'INR',
  'Asia/Kolkata',
  'Thank you for dining with Sambar & Spicy! Visit again.',
  NULL,
  CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
  store_name = EXCLUDED.store_name,
  address = EXCLUDED.address,
  phone = EXCLUDED.phone,
  receipt_footer = EXCLUDED.receipt_footer;

-- 2. Clean previous dummy seed data (preserving user table fixed accounts id 1, 2, 3)
DELETE FROM audit_logs;
DELETE FROM notifications;
DELETE FROM operating_expenses;
DELETE FROM stock_movements;
DELETE FROM stock_daily_entries;
DELETE FROM stock_purchase_lines;
DELETE FROM stock_purchases;
DELETE FROM inventory_items;
DELETE FROM bill_payments;
DELETE FROM bill_items;
DELETE FROM bills;
DELETE FROM bill_counters;
DELETE FROM menu_items;
DELETE FROM categories;

ALTER SEQUENCE categories_id_seq RESTART WITH 1;
ALTER SEQUENCE menu_items_id_seq RESTART WITH 1;
ALTER SEQUENCE inventory_items_id_seq RESTART WITH 1;
ALTER SEQUENCE stock_purchases_id_seq RESTART WITH 1;
ALTER SEQUENCE stock_purchase_lines_id_seq RESTART WITH 1;
ALTER SEQUENCE stock_daily_entries_id_seq RESTART WITH 1;
ALTER SEQUENCE stock_movements_id_seq RESTART WITH 1;
ALTER SEQUENCE bills_id_seq RESTART WITH 1;
ALTER SEQUENCE bill_items_id_seq RESTART WITH 1;
ALTER SEQUENCE bill_payments_id_seq RESTART WITH 1;
ALTER SEQUENCE operating_expenses_id_seq RESTART WITH 1;
ALTER SEQUENCE notifications_id_seq RESTART WITH 1;
ALTER SEQUENCE audit_logs_id_seq RESTART WITH 1;

-- ------------------------------------------------------------------------------
-- 3. CATEGORIES (10 Categories)
-- ------------------------------------------------------------------------------
INSERT INTO categories (id, name, image_path, sort_order, is_active, created_at, updated_at) VALUES
(1, 'Crispy Dosas & Crepes', 'uploads/categories/crispy-dosas.jpg', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Steamed Idlis & Vadas', 'uploads/categories/idli-vada.jpg', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'South Indian Meals & Thalis', 'uploads/categories/meals-thali.jpg', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'Biryani & Pulao Specialties', 'uploads/categories/biryani-rice.jpg', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'Spicy Curries & Gravies', 'uploads/categories/spicy-curries.jpg', 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'Starters & Crispy Bites', 'uploads/categories/starters.jpg', 6, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'Beverages & Filter Coffee', 'uploads/categories/beverages.jpg', 7, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'Traditional Sweets & Desserts', 'uploads/categories/sweets.jpg', 8, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 'Tiffin Combos & Snacks', 'uploads/categories/tiffin.jpg', 9, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'Breads & Accompaniments', 'uploads/categories/breads.jpg', 10, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
SELECT setval('categories_id_seq', 10, true);

-- ------------------------------------------------------------------------------
-- 4. MENU ITEMS (56 Items with Portions, Dietary Types, and Costs)
-- ------------------------------------------------------------------------------
INSERT INTO menu_items
(id, category_id, sku, name, description, dietary_type, selling_price, has_half_portion, half_selling_price, estimated_unit_cost, half_estimated_unit_cost, image_path, sort_order, is_available, created_at, updated_at)
VALUES
-- Crispy Dosas (Category 1)
(1, 1, 'DOS-001', 'Plain Roast Dosa', 'Golden thin crispy crepe served with 3 coconut chutneys & piping hot sambar', 'VEG', 60.00, FALSE, NULL, 20.00, NULL, 'uploads/dish-1.jpg', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, 'DOS-002', 'Special Masala Dosa', 'Crispy fermented crepe filled with spiced potato onion palya', 'VEG', 85.00, FALSE, NULL, 28.00, NULL, 'uploads/dish-2.jpg', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, 'DOS-003', 'Mysore Masala Dosa', 'Spicy red garlic chutney smeared inside and filled with seasoned aloo masala', 'VEG', 95.00, FALSE, NULL, 32.00, NULL, 'uploads/dish-3.jpg', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 1, 'DOS-004', 'Ghee Roast Dosa', 'Crispy crepe roasted generously with fragrant desi cow ghee', 'VEG', 110.00, FALSE, NULL, 40.00, NULL, 'uploads/dish-4.jpg', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 1, 'DOS-005', 'Onion Rava Masala Dosa', 'Crispy semolina crepe studded with green chilies, onions and cumin seeds', 'VEG', 95.00, FALSE, NULL, 30.00, NULL, 'uploads/dish-5.jpg', 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 1, 'DOS-006', 'Paneer Cheese Burst Dosa', 'Fusion dosa overloaded with spiced grated paneer and melted mozzarella cheese', 'VEG', 130.00, FALSE, NULL, 48.00, NULL, 'uploads/dish-6.jpg', 6, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 1, 'DOS-007', 'Egg Podi Roast Dosa', 'Crispy roast coated with spicy gunpowder podi and a farm egg layer', 'EGG', 105.00, FALSE, NULL, 38.00, NULL, 'uploads/dish-7.jpg', 7, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 1, 'DOS-008', 'Butter Paper Roast Dosa', 'Ultra-thin, large rolled crispy dosa smothered with churned butter', 'VEG', 115.00, FALSE, NULL, 36.00, NULL, 'uploads/dish-8.jpg', 8, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 1, 'DOS-009', 'Cheese Corn Dosa', 'Golden crepe stuffed with sweet american corn and grated cheese', 'VEG', 125.00, FALSE, NULL, 44.00, NULL, 'uploads/dish-9.jpg', 9, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 1, 'DOS-010', 'Chicken Keema Roast Dosa', 'Crisp crepe filled with minced spicy chicken keema and curry leaf garnish', 'NON_VEG', 150.00, FALSE, NULL, 62.00, NULL, 'uploads/dish-10.jpg', 10, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Steamed Idlis & Vadas (Category 2)
(11, 2, 'IDL-001', 'Steamed Rice Idli (2 Pcs)', 'Pillow soft fermented steamed rice and urad dal cakes', 'VEG', 40.00, TRUE, 25.00, 12.00, 7.00, 'uploads/dish-11.jpg', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 2, 'IDL-002', 'Crispy Medu Vada (2 Pcs)', 'Deep fried crunchy urad dal doughnut shaped fritters with fresh coconut pieces', 'VEG', 50.00, TRUE, 30.00, 16.00, 9.00, 'uploads/dish-12.jpg', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, 2, 'IDL-003', 'Idli Vada Combo', 'Two hot idlis and one crispy medu vada dipped in hot aromatic sambar bowl', 'VEG', 65.00, FALSE, NULL, 20.00, NULL, 'uploads/dish-13.jpg', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(14, 2, 'IDL-004', 'Ghee Podi Button Idlis (12 Pcs)', 'Mini button idlis tossed in hot ghee and aromatic spicy gunpowder', 'VEG', 75.00, FALSE, NULL, 26.00, NULL, 'uploads/dish-14.jpg', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(15, 2, 'IDL-005', 'Dahi Vada (2 Pcs)', 'Crispy vadas soaked in chilled seasoned curd topped with cumin powder & boondi', 'VEG', 70.00, FALSE, NULL, 24.00, NULL, 'uploads/dish-15.jpg', 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(16, 2, 'IDL-006', 'Rava Idli with Potato Sagu', 'Steamed semolina idli enriched with cashews, mustard seeds, and ghee', 'VEG', 55.00, FALSE, NULL, 18.00, NULL, 'uploads/dish-16.jpg', 6, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(17, 2, 'IDL-007', 'Sambar Dipped Mini Idli Bowl', '14 mini idlis immersed fully in homestyle vegetable sambar', 'VEG', 70.00, FALSE, NULL, 22.00, NULL, 'uploads/dish-17.jpg', 7, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- South Indian Meals & Thalis (Category 3)
(18, 3, 'THL-001', 'South Indian Quick Mini Meal', 'Steamed rice, sambar, rasam, kootu, papad, curd and sweet of the day', 'VEG', 120.00, FALSE, NULL, 42.00, NULL, 'uploads/dish-18.jpg', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(19, 3, 'THL-002', 'Grand Royal Karnataka Thali', 'Full course feast on banana leaf with 2 rotis, poori, rice, 4 curries, payasam', 'VEG', 180.00, FALSE, NULL, 65.00, NULL, 'uploads/dish-19.jpg', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(20, 3, 'THL-003', 'Sambar Rice Bowl with Ghee', 'Traditional aromatic bisibelebath style rice tempered with cashew and ghee', 'VEG', 80.00, TRUE, 50.00, 26.00, 16.00, 'uploads/dish-20.jpg', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(21, 3, 'THL-004', 'Curd Rice with Tadka', 'Comforting cooled rice tempered with mustard, pomegranate, and ginger', 'VEG', 60.00, TRUE, 35.00, 18.00, 11.00, 'uploads/dish-21.jpg', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(22, 3, 'THL-005', 'Lemon Rice with Papad', 'Tangy lemon infused tempered rice with roasted peanuts and curry leaves', 'VEG', 70.00, TRUE, 40.00, 22.00, 13.00, 'uploads/dish-22.jpg', 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(23, 3, 'THL-006', 'Tamarind Rice (Puliyogare)', 'Traditional temple style tangy spiced tamarind rice with groundnut crunch', 'VEG', 75.00, TRUE, 45.00, 24.00, 15.00, 'uploads/dish-23.jpg', 6, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Biryani & Pulao Specialties (Category 4)
(24, 4, 'BRY-001', 'Ambur Chicken Dum Biryani', 'Short grain seeraga samba rice cooked with tender marinated chicken pieces', 'NON_VEG', 190.00, TRUE, 110.00, 72.00, 42.00, 'uploads/dish-24.jpg', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(25, 4, 'BRY-002', 'Chettinad Mutton Biryani', 'Fragrant slow cooked mutton spiced with freshly roasted Chettinad spices', 'NON_VEG', 260.00, TRUE, 150.00, 110.00, 65.00, 'uploads/dish-25.jpg', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(26, 4, 'BRY-003', 'Hyderabadi Veg Dum Biryani', 'Layers of basmati rice, vegetables, paneer cubes and saffron essence', 'VEG', 140.00, TRUE, 80.00, 48.00, 28.00, 'uploads/dish-26.jpg', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(27, 4, 'BRY-004', 'Egg Biryani (2 Eggs)', 'Fragrant biryani rice served with 2 hard boiled fried spiced eggs and raita', 'EGG', 130.00, TRUE, 75.00, 44.00, 26.00, 'uploads/dish-27.jpg', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(28, 4, 'BRY-005', 'Donne Chicken Biryani', 'Bangalore military hotel style green coriander-mint spiced fragrant rice', 'NON_VEG', 195.00, TRUE, 115.00, 76.00, 45.00, 'uploads/dish-28.jpg', 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(29, 4, 'BRY-006', 'Paneer Tikka Biryani', 'Clay oven smoked paneer cubes layered with aromatic basmati grains', 'VEG', 165.00, TRUE, 95.00, 58.00, 34.00, 'uploads/dish-29.jpg', 6, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Spicy Curries & Gravies (Category 5)
(30, 5, 'CUR-001', 'Chettinad Chicken Masala', 'Fiery chicken curry infused with star anise, fennel, and coconut paste', 'NON_VEG', 170.00, TRUE, 95.00, 65.00, 38.00, 'uploads/dish-30.jpg', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(31, 5, 'CUR-002', 'Mangalore Ghee Roast Chicken', 'Succulent boneless chicken dry roasted in spicy red byadgi chili paste and ghee', 'NON_VEG', 210.00, TRUE, 120.00, 80.00, 48.00, 'uploads/dish-31.jpg', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(32, 5, 'CUR-003', 'Paneer Butter Masala', 'Cottage cheese cubes simmered in velvety butter tomato cashew gravy', 'VEG', 150.00, TRUE, 85.00, 52.00, 30.00, 'uploads/dish-32.jpg', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(33, 5, 'CUR-004', 'South Indian Veg Kurma', 'Mixed seasonal vegetables cooked in coconut, poppy seed and cashew gravy', 'VEG', 90.00, TRUE, 55.00, 30.00, 18.00, 'uploads/dish-33.jpg', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(34, 5, 'CUR-005', 'Mutton Sukka Masala', 'Dry pan roasted spiced goat meat with caramelized onions and curry leaves', 'NON_VEG', 240.00, TRUE, 135.00, 105.00, 60.00, 'uploads/dish-34.jpg', 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(35, 5, 'CUR-006', 'Egg Curry (2 Eggs)', 'Boiled farm eggs cooked in spicy onion tomato onion gravy', 'EGG', 95.00, TRUE, 60.00, 32.00, 20.00, 'uploads/dish-35.jpg', 6, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Starters & Crispy Bites (Category 6)
(36, 6, 'STR-001', 'Chicken 65', 'Deep fried crispy boneless chicken bites marinated in curd, chili and curry leaves', 'NON_VEG', 150.00, TRUE, 85.00, 55.00, 32.00, 'uploads/dish-36.jpg', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(37, 6, 'STR-002', 'Crispy Paneer Pepper Fry', 'Fresh paneer strips tossed with cracked black pepper and green bell peppers', 'VEG', 135.00, TRUE, 75.00, 48.00, 26.00, 'uploads/dish-37.jpg', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(38, 6, 'STR-003', 'Gobi Manchurian Dry', 'Cauliflower florets crisp-fried and tossed in tangy soy-garlic glaze', 'VEG', 95.00, TRUE, 55.00, 30.00, 18.00, 'uploads/dish-38.jpg', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(39, 6, 'STR-004', 'Egg Bonda (3 Pcs)', 'Boiled egg halves dipped in spiced gram flour batter and deep fried', 'EGG', 65.00, FALSE, NULL, 22.00, NULL, 'uploads/dish-39.jpg', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(40, 6, 'STR-005', 'Mangalore Fish Fry', 'Seer fish fillet coated in spicy red masala and shallow fried on tava', 'NON_VEG', 220.00, FALSE, NULL, 95.00, NULL, 'uploads/dish-40.jpg', 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Beverages & Filter Coffee (Category 7)
(41, 7, 'BEV-001', 'Degree Filter Coffee', 'Freshly brewed aromatic chicory-coffee decoction with foaming hot milk', 'VEG', 30.00, FALSE, NULL, 10.00, NULL, 'uploads/dish-41.jpg', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(42, 7, 'BEV-002', 'Special Masala Chai', 'Hot tea infused with fresh ginger, crushed cardamom and cloves', 'VEG', 25.00, FALSE, NULL, 8.00, NULL, 'uploads/dish-42.jpg', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(43, 7, 'BEV-003', 'Masala Chaas (Spiced Buttermilk)', 'Chilled churned buttermilk with ginger, green chili, coriander and asafoetida', 'VEG', 25.00, FALSE, NULL, 7.00, NULL, 'uploads/dish-43.jpg', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(44, 7, 'BEV-004', 'Badam Milk (Hot / Cold)', 'Rich milk flavored with almond paste, saffron strands and green cardamom', 'VEG', 50.00, FALSE, NULL, 18.00, NULL, 'uploads/dish-44.jpg', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(45, 7, 'BEV-005', 'Fresh Lime Soda (Sweet & Salt)', 'Refreshing sparkling soda with fresh squeezed lime juice', 'VEG', 40.00, FALSE, NULL, 12.00, NULL, 'uploads/dish-45.jpg', 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Traditional Sweets & Desserts (Category 8)
(46, 8, 'SWT-001', 'Pineapple Rava Kesari Bath', 'Mouth melting semolina sweet loaded with pure cow ghee and pineapple bits', 'VEG', 50.00, FALSE, NULL, 16.00, NULL, 'uploads/dish-46.jpg', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(47, 8, 'SWT-002', 'Mysore Pak', 'Rich melt-in-mouth traditional sweet made of gram flour, sugar and pure desi ghee', 'VEG', 45.00, FALSE, NULL, 15.00, NULL, 'uploads/dish-47.jpg', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(48, 8, 'SWT-003', 'Hot Gulab Jamun (2 Pcs)', 'Soft fried milk dough dumplings soaked in rose flavored warm sugar syrup', 'VEG', 40.00, FALSE, NULL, 14.00, NULL, 'uploads/dish-48.jpg', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(49, 8, 'SWT-004', 'Elaneer Payasam (Tender Coconut Kheer)', 'Kerala delicacy made with fresh tender coconut pulp, coconut milk and cardamom', 'VEG', 65.00, FALSE, NULL, 24.00, NULL, 'uploads/dish-49.jpg', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Tiffin Combos & Snacks (Category 9)
(50, 9, 'TIF-001', 'Poori Sagu (3 Pcs)', 'Fluffy deep-fried golden wheat pooris served with spiced vegetable sagu', 'VEG', 70.00, FALSE, NULL, 24.00, NULL, 'uploads/dish-50.jpg', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(51, 9, 'TIF-002', 'Upma Chow Chow Bath', 'Half plate spicy vegetable khara bath and half sweet pineapple kesari', 'VEG', 75.00, FALSE, NULL, 25.00, NULL, 'uploads/dish-51.jpg', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(52, 9, 'TIF-003', 'Onion Uttapam', 'Thick soft spongy savory pancake topped with caramelized chopped onions', 'VEG', 80.00, FALSE, NULL, 26.00, NULL, 'uploads/dish-52.jpg', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(53, 9, 'TIF-004', 'Tomato Chutney Uttapam', 'Thick pancake generously smeared with tangy spiced tomato-onion chutney', 'VEG', 85.00, FALSE, NULL, 28.00, NULL, 'uploads/dish-53.jpg', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Breads & Accompaniments (Category 10)
(54, 10, 'BRD-001', 'Malabar Parotta (2 Pcs)', 'Flaky, layered soft multi-fold refined flour flatbread', 'VEG', 45.00, FALSE, NULL, 14.00, NULL, 'uploads/dish-54.jpg', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(55, 10, 'BRD-002', 'Wheat Chapati (2 Pcs)', 'Soft hand-rolled whole wheat tawa chapatis served with pickle', 'VEG', 35.00, FALSE, NULL, 10.00, NULL, 'uploads/dish-55.jpg', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(56, 10, 'BRD-003', 'Appam with Coconut Milk (2 Pcs)', 'Fermented rice bowl pancake with soft spongy center and crispy lace edges', 'VEG', 60.00, FALSE, NULL, 18.00, NULL, 'uploads/dish-56.jpg', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
SELECT setval('menu_items_id_seq', 56, true);

-- ------------------------------------------------------------------------------
-- 5. INVENTORY ITEMS (20 Raw Material Items)
-- ------------------------------------------------------------------------------
INSERT INTO inventory_items
(id, name, sku, unit, current_qty, min_limit_qty, critical_limit_qty, avg_unit_cost, is_active, created_at, updated_at)
VALUES
(1, 'Sona Masoori Raw Rice', 'INV-RICE-01', 'KG', 180.000, 40.000, 15.000, 52.5000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Urad Dal (Black Gram Skinless)', 'INV-DAL-01', 'KG', 75.000, 25.000, 10.000, 128.0000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Toor Dal (Pigeon Pea)', 'INV-DAL-02', 'KG', 85.000, 20.000, 8.000, 145.0000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'Fresh Farm Chicken', 'INV-CHK-01', 'KG', 42.000, 15.000, 5.000, 195.0000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'Fresh Mutton (Bone-in)', 'INV-MUT-01', 'KG', 18.000, 8.000, 3.000, 720.0000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'Farm Fresh Eggs', 'INV-EGG-01', 'PIECE', 240.000, 60.000, 20.000, 5.8000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'Malai Paneer Block', 'INV-PAN-01', 'KG', 22.000, 10.000, 4.000, 320.0000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'Pure Cow Ghee', 'INV-GHEE-01', 'KG', 28.000, 10.000, 3.000, 620.0000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 'Refined Sunflower Cooking Oil', 'INV-OIL-01', 'LITRE', 90.000, 30.000, 10.000, 115.0000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'Full Cream Dairy Milk', 'INV-MLK-01', 'LITRE', 65.000, 25.000, 10.000, 54.0000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 'Onions (Sambar Small & Big)', 'INV-VEG-01', 'KG', 65.000, 20.000, 8.000, 35.0000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 'Country Tomatoes', 'INV-VEG-02', 'KG', 48.000, 15.000, 5.000, 28.0000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, 'Potatoes (Aloo)', 'INV-VEG-03', 'KG', 55.000, 20.000, 6.000, 26.0000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(14, 'South Indian Filter Coffee Powder', 'INV-COF-01', 'KG', 12.000, 5.000, 2.000, 480.0000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(15, 'Paper Packaging Boxes & Bowls', 'INV-PKG-01', 'PACK', 450.000, 100.000, 30.000, 4.5000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(16, 'Seeraga Samba Biryani Rice', 'INV-RICE-02', 'KG', 60.000, 20.000, 8.000, 110.0000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(17, 'Fresh Grated Coconut', 'INV-COC-01', 'KG', 35.000, 10.000, 4.000, 65.0000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(18, 'Refined Wheat Flour (Maida)', 'INV-FLR-01', 'KG', 50.000, 15.000, 5.000, 42.0000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(19, 'Atta (Whole Wheat Flour)', 'INV-FLR-02', 'KG', 45.000, 15.000, 5.000, 45.0000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(20, 'Semolina (Bombay Rava)', 'INV-RAV-01', 'KG', 40.000, 12.000, 4.000, 48.0000, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
SELECT setval('inventory_items_id_seq', 20, true);

-- ------------------------------------------------------------------------------
-- 6. STOCK PURCHASES & MOVEMENTS
-- ------------------------------------------------------------------------------
INSERT INTO stock_movements
(inventory_item_id, business_date, movement_type, quantity_delta, cost_per_unit_snapshot, balance_after, purchase_line_id, daily_entry_id, notes, recorded_by, recorded_at)
VALUES
(1, '2026-11-20', 'OPENING', 150.000, 52.0000, 150.000, NULL, NULL, 'Initial baseline opening stock', 1, '2026-11-20 06:00:00+05:30'),
(2, '2026-11-20', 'OPENING', 60.000, 126.0000, 60.000, NULL, NULL, 'Initial baseline opening stock', 1, '2026-11-20 06:00:00+05:30'),
(3, '2026-11-20', 'OPENING', 70.000, 142.0000, 70.000, NULL, NULL, 'Initial baseline opening stock', 1, '2026-11-20 06:00:00+05:30'),
(4, '2026-11-20', 'OPENING', 30.000, 190.0000, 30.000, NULL, NULL, 'Fresh poultry opening stock', 1, '2026-11-20 06:00:00+05:30'),
(7, '2026-11-20', 'OPENING', 15.000, 315.0000, 15.000, NULL, NULL, 'Dairy opening stock', 1, '2026-11-20 06:00:00+05:30'),
(8, '2026-11-20', 'OPENING', 20.000, 610.0000, 20.000, NULL, NULL, 'Desi Ghee opening stock', 1, '2026-11-20 06:00:00+05:30'),
(16, '2026-11-20', 'OPENING', 40.000, 108.0000, 40.000, NULL, NULL, 'Seeraga samba rice opening', 1, '2026-11-20 06:00:00+05:30');

-- Stock Purchase Headers
INSERT INTO stock_purchases (id, purchase_reference, supplier_name, business_date, total_amount, notes, created_by, created_at)
VALUES
(1, 'PO-2026-1121-01', 'Sri Lakshmi Provision Stores', '2026-11-21', 11475.00, 'Weekly grain, oil, and dal bulk supplies', 1, '2026-11-21 08:30:00+05:30'),
(2, 'PO-2026-1122-01', 'Kaveri Fresh Meats & Poultry', '2026-11-22', 12360.00, 'Daily fresh chicken and mutton shipment', 1, '2026-11-22 07:45:00+05:30'),
(3, 'PO-2026-1123-01', 'Nandi Hills Dairy Cooperative', '2026-11-23', 5940.00, 'Daily morning milk, curd, and paneer delivery', 1, '2026-11-23 06:30:00+05:30');
SELECT setval('stock_purchases_id_seq', 3, true);

-- Stock Purchase Lines
INSERT INTO stock_purchase_lines (id, purchase_id, inventory_item_id, quantity, unit_cost) VALUES
(1, 1, 1, 50.000, 53.0000),   -- Rice
(2, 1, 2, 25.000, 130.0000),  -- Urad Dal
(3, 1, 3, 25.000, 147.0000),  -- Toor Dal
(4, 1, 8, 10.000, 630.0000),  -- Ghee
(5, 2, 4, 30.000, 196.0000),  -- Chicken
(6, 2, 5, 9.000, 720.0000),   -- Mutton
(7, 3, 7, 10.000, 324.0000),  -- Paneer
(8, 3, 10, 50.000, 54.0000);  -- Milk
SELECT setval('stock_purchase_lines_id_seq', 8, true);

-- Inward movements
INSERT INTO stock_movements
(inventory_item_id, business_date, movement_type, quantity_delta, cost_per_unit_snapshot, balance_after, purchase_line_id, daily_entry_id, notes, recorded_by, recorded_at)
VALUES
(1, '2026-11-21', 'PURCHASE', 50.000, 53.0000, 200.000, 1, NULL, 'Supplier delivery PO-2026-1121-01', 1, '2026-11-21 09:00:00+05:30'),
(2, '2026-11-21', 'PURCHASE', 25.000, 130.0000, 85.000, 2, NULL, 'Supplier delivery PO-2026-1121-01', 1, '2026-11-21 09:00:00+05:30'),
(3, '2026-11-21', 'PURCHASE', 25.000, 147.0000, 95.000, 3, NULL, 'Supplier delivery PO-2026-1121-01', 1, '2026-11-21 09:00:00+05:30'),
(8, '2026-11-21', 'PURCHASE', 10.000, 630.0000, 30.000, 4, NULL, 'Supplier delivery PO-2026-1121-01', 1, '2026-11-21 09:00:00+05:30'),
(4, '2026-11-22', 'PURCHASE', 30.000, 196.0000, 60.000, 5, NULL, 'Fresh meat delivery PO-2026-1122-01', 1, '2026-11-22 08:00:00+05:30'),
(5, '2026-11-22', 'PURCHASE', 9.000, 720.0000, 27.000, 6, NULL, 'Fresh meat delivery PO-2026-1122-01', 1, '2026-11-22 08:00:00+05:30'),
(7, '2026-11-23', 'PURCHASE', 10.000, 324.0000, 25.000, 7, NULL, 'Dairy delivery PO-2026-1123-01', 1, '2026-11-23 07:00:00+05:30'),
(10, '2026-11-23', 'PURCHASE', 50.000, 54.0000, 95.000, 8, NULL, 'Dairy delivery PO-2026-1123-01', 1, '2026-11-23 07:00:00+05:30');

-- ------------------------------------------------------------------------------
-- 7. DAILY USAGE & WASTAGE ENTRIES
-- ------------------------------------------------------------------------------
INSERT INTO stock_daily_entries
(id, request_key, business_date, inventory_item_id, used_qty, wasted_qty, notes, created_by, posted_at)
VALUES
(1, 'REQ-DLY-20261121-01', '2026-11-21', 1, 15.000, 0.500, 'Batch 1 idli & dosa batter grind', 2, '2026-11-21 21:30:00+05:30'),
(2, 'REQ-DLY-20261121-02', '2026-11-21', 2, 8.000, 0.200, 'Medu vada and batter consumption', 2, '2026-11-21 21:30:00+05:30'),
(3, 'REQ-DLY-20261122-01', '2026-11-22', 4, 18.000, 1.000, 'Lunch & dinner chicken biryani prep', 2, '2026-11-22 22:00:00+05:30'),
(4, 'REQ-DLY-20261122-02', '2026-11-22', 5, 8.000, 0.500, 'Mutton biryani preparation', 2, '2026-11-22 22:00:00+05:30'),
(5, 'REQ-DLY-20261123-01', '2026-11-23', 10, 20.000, 0.800, 'Morning tea, filter coffee and kesari milk', 2, '2026-11-23 14:00:00+05:30');
SELECT setval('stock_daily_entries_id_seq', 5, true);

INSERT INTO stock_movements
(inventory_item_id, business_date, movement_type, quantity_delta, cost_per_unit_snapshot, balance_after, purchase_line_id, daily_entry_id, notes, recorded_by, recorded_at)
VALUES
(1, '2026-11-21', 'DAILY_USE', -15.000, 52.5000, 185.000, NULL, 1, 'Daily consumption', 2, '2026-11-21 21:30:00+05:30'),
(1, '2026-11-21', 'WASTE', -0.500, 52.5000, 184.500, NULL, 1, 'Spillage during grinding', 2, '2026-11-21 21:30:00+05:30'),
(2, '2026-11-21', 'DAILY_USE', -8.000, 128.0000, 77.000, NULL, 2, 'Daily consumption', 2, '2026-11-21 21:30:00+05:30'),
(2, '2026-11-21', 'WASTE', -0.200, 128.0000, 76.800, NULL, 2, 'Trimming waste', 2, '2026-11-21 21:30:00+05:30'),
(4, '2026-11-22', 'DAILY_USE', -18.000, 195.0000, 42.000, NULL, 3, 'Daily consumption', 2, '2026-11-22 22:00:00+05:30'),
(4, '2026-11-22', 'WASTE', -1.000, 195.0000, 41.000, NULL, 3, 'Trimming/fat removal', 2, '2026-11-22 22:00:00+05:30'),
(10, '2026-11-23', 'DAILY_USE', -20.000, 54.0000, 75.000, NULL, 5, 'Beverage preparations', 2, '2026-11-23 14:00:00+05:30'),
(10, '2026-11-23', 'WASTE', -0.800, 54.0000, 74.200, NULL, 5, 'Boiling spillage', 2, '2026-11-23 14:00:00+05:30');
SELECT setval('stock_movements_id_seq', 23, true);

-- ------------------------------------------------------------------------------
-- 8. OPERATING EXPENSES
-- ------------------------------------------------------------------------------
INSERT INTO operating_expenses (id, business_date, expense_category, description, amount, created_by, created_at)
VALUES
(1, '2026-11-21', 'UTILITY', 'Commercial LPG cylinder replacement', 1850.00, 1, '2026-11-21 11:00:00+05:30'),
(2, '2026-11-21', 'CLEANING', 'Cleaning supplies, dishwashing soap, tissues', 450.00, 2, '2026-11-21 14:00:00+05:30'),
(3, '2026-11-22', 'UTILITY', 'Filtered water can supply (10 cans)', 600.00, 1, '2026-11-22 10:30:00+05:30'),
(4, '2026-11-22', 'MAINTENANCE', 'Exhaust chimney servicing and hood grease clearing', 1200.00, 1, '2026-11-22 16:00:00+05:30'),
(5, '2026-11-23', 'UTILITY', 'Commercial ice bags and dairy supply transport', 350.00, 2, '2026-11-23 08:00:00+05:30'),
(6, '2026-11-23', 'PACKAGING', 'Banana leaf bundles from KR Market wholesale', 400.00, 2, '2026-11-23 09:30:00+05:30'),
(7, '2026-11-23', 'SALARY', 'Daily kitchen helper daily wages payout', 1400.00, 1, '2026-11-23 13:00:00+05:30'),
(8, '2026-11-23', 'REPAIR', 'Grinder belt replacement & lubrication service', 550.00, 1, '2026-11-23 14:30:00+05:30');
SELECT setval('operating_expenses_id_seq', 8, true);

-- ------------------------------------------------------------------------------
-- 9. BILL COUNTERS
-- ------------------------------------------------------------------------------
INSERT INTO bill_counters (business_date, last_sequence, updated_at) VALUES
('2026-11-21', 8, '2026-11-21 22:30:00+05:30'),
('2026-11-22', 8, '2026-11-22 22:45:00+05:30'),
('2026-11-23', 6, '2026-11-23 15:30:00+05:30');

-- ------------------------------------------------------------------------------
-- 10. BILLS, BILL ITEMS, AND BILL PAYMENTS
-- ------------------------------------------------------------------------------
-- Bill 1: 2026-11-21 PAID CASH
INSERT INTO bills
(id, bill_number, checkout_key, business_date, status, item_count, subtotal_amount, discount_amount, round_off_amount, total_amount, store_name_snapshot, store_address_snapshot, store_phone_snapshot, created_by, paid_by, created_at, paid_at)
VALUES
(1, 'BILL-20261121-0001', 'CHK-20261121-0001', '2026-11-21', 'PAID', 3, 225.00, 0.00, 0.00, 225.00, 'Sambar & Spicy', 'Shop 4 & 5, MG Road, Bengaluru', '+91 80 4123 4567', 2, 2, '2026-11-21 08:15:00+05:30', '2026-11-21 08:16:10+05:30');
INSERT INTO bill_items (bill_id, menu_item_id, item_name_snapshot, portion, quantity, unit_price, estimated_unit_cost_snapshot, line_discount_amount, created_at) VALUES
(1, 2, 'Special Masala Dosa', 'FULL', 2, 85.00, 28.00, 0.00, '2026-11-21 08:15:00+05:30'),
(1, 11, 'Steamed Rice Idli (2 Pcs)', 'HALF', 1, 25.00, 7.00, 0.00, '2026-11-21 08:15:00+05:30'),
(1, 41, 'Degree Filter Coffee', 'FULL', 1, 30.00, 10.00, 0.00, '2026-11-21 08:15:00+05:30');
INSERT INTO bill_payments (bill_id, payment_method, amount, tendered_amount, recorded_by, recorded_at) VALUES
(1, 'CASH', 225.00, 300.00, 2, '2026-11-21 08:16:10+05:30');

-- Bill 2: 2026-11-21 PAID UPI
INSERT INTO bills
(id, bill_number, checkout_key, business_date, status, item_count, subtotal_amount, discount_amount, round_off_amount, total_amount, store_name_snapshot, store_address_snapshot, store_phone_snapshot, created_by, paid_by, created_at, paid_at)
VALUES
(2, 'BILL-20261121-0002', 'CHK-20261121-0002', '2026-11-21', 'PAID', 2, 380.00, 0.00, 0.00, 380.00, 'Sambar & Spicy', 'Shop 4 & 5, MG Road, Bengaluru', '+91 80 4123 4567', 2, 2, '2026-11-21 13:10:00+05:30', '2026-11-21 13:11:00+05:30');
INSERT INTO bill_items (bill_id, menu_item_id, item_name_snapshot, portion, quantity, unit_price, estimated_unit_cost_snapshot, line_discount_amount, created_at) VALUES
(2, 24, 'Ambur Chicken Dum Biryani', 'FULL', 2, 190.00, 72.00, 0.00, '2026-11-21 13:10:00+05:30');
INSERT INTO bill_payments (bill_id, payment_method, amount, tendered_amount, reference_number, recorded_by, recorded_at) VALUES
(2, 'UPI', 380.00, 380.00, 'UPI-REF-9988112233', 2, '2026-11-21 13:11:00+05:30');

-- Bill 3: 2026-11-21 PAID CARD
INSERT INTO bills
(id, bill_number, checkout_key, business_date, status, item_count, subtotal_amount, discount_amount, round_off_amount, total_amount, store_name_snapshot, store_address_snapshot, store_phone_snapshot, created_by, paid_by, created_at, paid_at)
VALUES
(3, 'BILL-20261121-0003', 'CHK-20261121-0003', '2026-11-21', 'PAID', 3, 440.00, 20.00, 0.00, 420.00, 'Sambar & Spicy', 'Shop 4 & 5, MG Road, Bengaluru', '+91 80 4123 4567', 2, 2, '2026-11-21 14:20:00+05:30', '2026-11-21 14:21:30+05:30');
INSERT INTO bill_items (bill_id, menu_item_id, item_name_snapshot, portion, quantity, unit_price, estimated_unit_cost_snapshot, line_discount_amount, created_at) VALUES
(3, 19, 'Grand Royal Karnataka Thali', 'FULL', 2, 180.00, 65.00, 20.00, '2026-11-21 14:20:00+05:30'),
(3, 21, 'Curd Rice with Tadka', 'HALF', 1, 35.00, 11.00, 0.00, '2026-11-21 14:20:00+05:30'),
(3, 46, 'Pineapple Rava Kesari Bath', 'FULL', 1, 45.00, 16.00, 0.00, '2026-11-21 14:20:00+05:30');
INSERT INTO bill_payments (bill_id, payment_method, amount, tendered_amount, reference_number, recorded_by, recorded_at) VALUES
(3, 'CARD', 420.00, 420.00, 'CARD-AUTH-771122', 2, '2026-11-21 14:21:30+05:30');

-- Bill 4: 2026-11-21 PAID CASH
INSERT INTO bills
(id, bill_number, checkout_key, business_date, status, item_count, subtotal_amount, discount_amount, round_off_amount, total_amount, store_name_snapshot, store_address_snapshot, store_phone_snapshot, created_by, paid_by, created_at, paid_at)
VALUES
(4, 'BILL-20261121-0004', 'CHK-20261121-0004', '2026-11-21', 'PAID', 2, 175.00, 0.00, 0.00, 175.00, 'Sambar & Spicy', 'Shop 4 & 5, MG Road, Bengaluru', '+91 80 4123 4567', 2, 2, '2026-11-21 17:30:00+05:30', '2026-11-21 17:31:15+05:30');
INSERT INTO bill_items (bill_id, menu_item_id, item_name_snapshot, portion, quantity, unit_price, estimated_unit_cost_snapshot, line_discount_amount, created_at) VALUES
(4, 5, 'Onion Rava Masala Dosa', 'FULL', 1, 95.00, 30.00, 0.00, '2026-11-21 17:30:00+05:30'),
(4, 52, 'Onion Uttapam', 'FULL', 1, 80.00, 26.00, 0.00, '2026-11-21 17:30:00+05:30');
INSERT INTO bill_payments (bill_id, payment_method, amount, tendered_amount, recorded_by, recorded_at) VALUES
(4, 'CASH', 175.00, 200.00, 2, '2026-11-21 17:31:15+05:30');

-- Bill 5: 2026-11-21 PAID UPI
INSERT INTO bills
(id, bill_number, checkout_key, business_date, status, item_count, subtotal_amount, discount_amount, round_off_amount, total_amount, store_name_snapshot, store_address_snapshot, store_phone_snapshot, created_by, paid_by, created_at, paid_at)
VALUES
(5, 'BILL-20261121-0005', 'CHK-20261121-0005', '2026-11-21', 'PAID', 3, 560.00, 0.00, 0.00, 560.00, 'Sambar & Spicy', 'Shop 4 & 5, MG Road, Bengaluru', '+91 80 4123 4567', 2, 2, '2026-11-21 20:00:00+05:30', '2026-11-21 20:01:20+05:30');
INSERT INTO bill_items (bill_id, menu_item_id, item_name_snapshot, portion, quantity, unit_price, estimated_unit_cost_snapshot, line_discount_amount, created_at) VALUES
(5, 25, 'Chettinad Mutton Biryani', 'FULL', 1, 260.00, 110.00, 0.00, '2026-11-21 20:00:00+05:30'),
(5, 30, 'Chettinad Chicken Masala', 'HALF', 1, 95.00, 38.00, 0.00, '2026-11-21 20:00:00+05:30'),
(5, 31, 'Mangalore Ghee Roast Chicken', 'FULL', 1, 205.00, 80.00, 0.00, '2026-11-21 20:00:00+05:30');
INSERT INTO bill_payments (bill_id, payment_method, amount, tendered_amount, reference_number, recorded_by, recorded_at) VALUES
(5, 'UPI', 560.00, 560.00, 'UPI-REF-3344556677', 2, '2026-11-21 20:01:20+05:30');

-- Bill 6: 2026-11-22 PAID CASH
INSERT INTO bills
(id, bill_number, checkout_key, business_date, status, item_count, subtotal_amount, discount_amount, round_off_amount, total_amount, store_name_snapshot, store_address_snapshot, store_phone_snapshot, created_by, paid_by, created_at, paid_at)
VALUES
(6, 'BILL-20261122-0001', 'CHK-20261122-0001', '2026-11-22', 'PAID', 2, 115.00, 0.00, 0.00, 115.00, 'Sambar & Spicy', 'Shop 4 & 5, MG Road, Bengaluru', '+91 80 4123 4567', 2, 2, '2026-11-22 08:30:00+05:30', '2026-11-22 08:31:00+05:30');
INSERT INTO bill_items (bill_id, menu_item_id, item_name_snapshot, portion, quantity, unit_price, estimated_unit_cost_snapshot, line_discount_amount, created_at) VALUES
(6, 13, 'Idli Vada Combo', 'FULL', 1, 65.00, 20.00, 0.00, '2026-11-22 08:30:00+05:30'),
(6, 46, 'Pineapple Rava Kesari Bath', 'FULL', 1, 50.00, 16.00, 0.00, '2026-11-22 08:30:00+05:30');
INSERT INTO bill_payments (bill_id, payment_method, amount, tendered_amount, recorded_by, recorded_at) VALUES
(6, 'CASH', 115.00, 120.00, 2, '2026-11-22 08:31:00+05:30');

-- Bill 7: 2026-11-22 PAID UPI
INSERT INTO bills
(id, bill_number, checkout_key, business_date, status, item_count, subtotal_amount, discount_amount, round_off_amount, total_amount, store_name_snapshot, store_address_snapshot, store_phone_snapshot, created_by, paid_by, created_at, paid_at)
VALUES
(7, 'BILL-20261122-0002', 'CHK-20261122-0002', '2026-11-22', 'PAID', 3, 395.00, 0.00, 0.00, 395.00, 'Sambar & Spicy', 'Shop 4 & 5, MG Road, Bengaluru', '+91 80 4123 4567', 2, 2, '2026-11-22 13:40:00+05:30', '2026-11-22 13:41:20+05:30');
INSERT INTO bill_items (bill_id, menu_item_id, item_name_snapshot, portion, quantity, unit_price, estimated_unit_cost_snapshot, line_discount_amount, created_at) VALUES
(7, 28, 'Donne Chicken Biryani', 'FULL', 1, 195.00, 76.00, 0.00, '2026-11-22 13:40:00+05:30'),
(7, 36, 'Chicken 65', 'FULL', 1, 150.00, 55.00, 0.00, '2026-11-22 13:40:00+05:30'),
(7, 43, 'Masala Chaas (Spiced Buttermilk)', 'FULL', 2, 25.00, 7.00, 0.00, '2026-11-22 13:40:00+05:30');
INSERT INTO bill_payments (bill_id, payment_method, amount, tendered_amount, reference_number, recorded_by, recorded_at) VALUES
(7, 'UPI', 395.00, 395.00, 'UPI-REF-8877665544', 2, '2026-11-22 13:41:20+05:30');

-- Bill 8: 2026-11-22 PAID CARD
INSERT INTO bills
(id, bill_number, checkout_key, business_date, status, item_count, subtotal_amount, discount_amount, round_off_amount, total_amount, store_name_snapshot, store_address_snapshot, store_phone_snapshot, created_by, paid_by, created_at, paid_at)
VALUES
(8, 'BILL-20261122-0003', 'CHK-20261122-0003', '2026-11-22', 'PAID', 3, 440.00, 0.00, 0.00, 440.00, 'Sambar & Spicy', 'Shop 4 & 5, MG Road, Bengaluru', '+91 80 4123 4567', 2, 2, '2026-11-22 19:15:00+05:30', '2026-11-22 19:16:40+05:30');
INSERT INTO bill_items (bill_id, menu_item_id, item_name_snapshot, portion, quantity, unit_price, estimated_unit_cost_snapshot, line_discount_amount, created_at) VALUES
(8, 6, 'Paneer Cheese Burst Dosa', 'FULL', 2, 130.00, 48.00, 0.00, '2026-11-22 19:15:00+05:30'),
(8, 19, 'Grand Royal Karnataka Thali', 'FULL', 1, 180.00, 65.00, 0.00, '2026-11-22 19:15:00+05:30');
INSERT INTO bill_payments (bill_id, payment_method, amount, tendered_amount, reference_number, recorded_by, recorded_at) VALUES
(8, 'CARD', 440.00, 440.00, 'POS-TXN-991188', 2, '2026-11-22 19:16:40+05:30');

-- Bill 9: 2026-11-23 PAID CASH
INSERT INTO bills
(id, bill_number, checkout_key, business_date, status, item_count, subtotal_amount, discount_amount, round_off_amount, total_amount, store_name_snapshot, store_address_snapshot, store_phone_snapshot, created_by, paid_by, created_at, paid_at)
VALUES
(9, 'BILL-20261123-0001', 'CHK-20261123-0001', '2026-11-23', 'PAID', 3, 215.00, 0.00, 0.00, 215.00, 'Sambar & Spicy', 'Shop 4 & 5, MG Road, Bengaluru', '+91 80 4123 4567', 2, 2, '2026-11-23 08:10:00+05:30', '2026-11-23 08:11:00+05:30');
INSERT INTO bill_items (bill_id, menu_item_id, item_name_snapshot, portion, quantity, unit_price, estimated_unit_cost_snapshot, line_discount_amount, created_at) VALUES
(9, 4, 'Ghee Roast Dosa', 'FULL', 1, 110.00, 40.00, 0.00, '2026-11-23 08:10:00+05:30'),
(9, 14, 'Ghee Podi Button Idlis (12 Pcs)', 'FULL', 1, 75.00, 26.00, 0.00, '2026-11-23 08:10:00+05:30'),
(9, 41, 'Degree Filter Coffee', 'FULL', 1, 30.00, 10.00, 0.00, '2026-11-23 08:10:00+05:30');
INSERT INTO bill_payments (bill_id, payment_method, amount, tendered_amount, recorded_by, recorded_at) VALUES
(9, 'CASH', 215.00, 250.00, 2, '2026-11-23 08:11:00+05:30');

-- Bill 10: 2026-11-23 PAID UPI
INSERT INTO bills
(id, bill_number, checkout_key, business_date, status, item_count, subtotal_amount, discount_amount, round_off_amount, total_amount, store_name_snapshot, store_address_snapshot, store_phone_snapshot, created_by, paid_by, created_at, paid_at)
VALUES
(10, 'BILL-20261123-0002', 'CHK-20261123-0002', '2026-11-23', 'PAID', 2, 280.00, 0.00, 0.00, 280.00, 'Sambar & Spicy', 'Shop 4 & 5, MG Road, Bengaluru', '+91 80 4123 4567', 2, 2, '2026-11-23 12:45:00+05:30', '2026-11-23 12:46:15+05:30');
INSERT INTO bill_items (bill_id, menu_item_id, item_name_snapshot, portion, quantity, unit_price, estimated_unit_cost_snapshot, line_discount_amount, created_at) VALUES
(10, 26, 'Hyderabadi Veg Dum Biryani', 'FULL', 2, 140.00, 48.00, 0.00, '2026-11-23 12:45:00+05:30');
INSERT INTO bill_payments (bill_id, payment_method, amount, tendered_amount, reference_number, recorded_by, recorded_at) VALUES
(10, 'UPI', 280.00, 280.00, 'UPI-REF-1122998877', 2, '2026-11-23 12:46:15+05:30');

-- Bill 11: 2026-11-23 HELD Counter Order
INSERT INTO bills
(id, bill_number, checkout_key, business_date, status, hold_label, item_count, subtotal_amount, discount_amount, round_off_amount, total_amount, created_by, created_at, held_at)
VALUES
(11, NULL, NULL, '2026-11-23', 'HELD', 'Table 4 / Waiting for cash', 2, 190.00, 0.00, 0.00, 190.00, 2, '2026-11-23 15:00:00+05:30', '2026-11-23 15:05:00+05:30');
INSERT INTO bill_items (bill_id, menu_item_id, item_name_snapshot, portion, quantity, unit_price, estimated_unit_cost_snapshot, line_discount_amount, created_at) VALUES
(11, 3, 'Mysore Masala Dosa', 'FULL', 1, 95.00, 32.00, 0.00, '2026-11-23 15:00:00+05:30'),
(11, 5, 'Onion Rava Masala Dosa', 'FULL', 1, 95.00, 30.00, 0.00, '2026-11-23 15:00:00+05:30');

-- Bill 12: 2026-11-23 CANCELLED Order
INSERT INTO bills
(id, bill_number, checkout_key, business_date, status, hold_label, item_count, subtotal_amount, discount_amount, round_off_amount, total_amount, created_by, cancelled_by, cancel_reason, created_at, cancelled_at)
VALUES
(12, NULL, NULL, '2026-11-23', 'CANCELLED', 'Takeaway Queue', 1, 150.00, 0.00, 0.00, 150.00, 2, 2, 'Customer had insufficient change and walked away', '2026-11-23 13:15:00+05:30', '2026-11-23 13:17:00+05:30');
INSERT INTO bill_items (bill_id, menu_item_id, item_name_snapshot, portion, quantity, unit_price, estimated_unit_cost_snapshot, line_discount_amount, created_at) VALUES
(12, 10, 'Chicken Keema Roast Dosa', 'FULL', 1, 150.00, 62.00, 0.00, '2026-11-23 13:15:00+05:30');

SELECT setval('bills_id_seq', 12, true);
SELECT setval('bill_items_id_seq', 26, true);
SELECT setval('bill_payments_id_seq', 10, true);

-- ------------------------------------------------------------------------------
-- 11. NOTIFICATIONS (Low Stock Alerts)
-- ------------------------------------------------------------------------------
INSERT INTO notifications
(id, inventory_item_id, severity, status, message, triggered_qty, created_at, last_notified_at, read_by, read_at, resolved_at)
VALUES
(1, 5, 'WARNING', 'OPEN', 'Mutton (Bone-in) inventory is low (18.000 KG remaining).', 18.000, '2026-11-23 14:00:00+05:30', '2026-11-23 14:00:00+05:30', NULL, NULL, NULL),
(2, 14, 'WARNING', 'RESOLVED', 'Filter Coffee Powder reached minimum threshold (4.500 KG).', 4.500, '2026-11-21 16:00:00+05:30', '2026-11-21 16:00:00+05:30', 1, '2026-11-21 17:00:00+05:30', '2026-11-22 09:00:00+05:30');
SELECT setval('notifications_id_seq', 2, true);

-- ------------------------------------------------------------------------------
-- 12. AUDIT LOGS (Operational Traceability)
-- ------------------------------------------------------------------------------
INSERT INTO audit_logs
(id, actor_user_id, action, entity_type, entity_id, old_data, new_data, ip_address, created_at)
VALUES
(1, 1, 'UPDATE_STORE_SETTINGS', 'StoreSetting', '1', '{"store_name":"Sambar & Spicy"}'::jsonb, '{"store_name":"Sambar & Spicy - South Indian Delights"}'::jsonb, '127.0.0.1', '2026-11-21 08:00:00+05:30'),
(2, 1, 'CREATE_MENU_ITEM', 'MenuItem', '1', NULL, '{"name":"Plain Roast Dosa","price":60.00}'::jsonb, '127.0.0.1', '2026-11-21 08:05:00+05:30'),
(3, 1, 'CREATE_MENU_ITEM', 'MenuItem', '24', NULL, '{"name":"Ambur Chicken Dum Biryani","price":190.00}'::jsonb, '127.0.0.1', '2026-11-21 08:10:00+05:30'),
(4, 2, 'CHECKOUT_BILL', 'Bill', '1', NULL, '{"bill_number":"BILL-20261121-0001","amount":225.00,"payment":"CASH"}'::jsonb, '127.0.0.1', '2026-11-21 08:16:10+05:30'),
(5, 2, 'POST_STOCK_ENTRY', 'StockDailyEntry', '1', NULL, '{"item_id":1,"used":15.000,"waste":0.500}'::jsonb, '127.0.0.1', '2026-11-21 21:30:00+05:30');
SELECT setval('audit_logs_id_seq', 5, true);

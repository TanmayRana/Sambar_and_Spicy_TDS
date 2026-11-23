# Sambar & Spicy — Backend Model Layer & Database Architecture

Production-grade database design and Spring Boot Data JPA model layer for **Sambar & Spicy**, a high-efficiency restaurant counter billing, inventory management, and reporting system.

---

## 1. Technology Stack

- **Platform:** Java 17 / 21 LTS
- **Framework:** Spring Boot 3.3.5 / Spring Data JPA (Hibernate 6.5)
- **Database Engine:** MySQL 8.0.16+ (Recommended MySQL 8.4 LTS), InnoDB engine
- **Character Encoding:** `utf8mb4` with collation `utf8mb4_unicode_ci`
- **Currency:** Indian Rupee (`INR` / `₹`)
- **Business Timezone:** `Asia/Kolkata` (with UTC temporal persistence)
- **Database Migration:** Flyway (`flyway-mysql`)

---

## 2. Architecture & Modules

### A. Authentication & User Management
- Exactly **three fixed user slots**:
  - `ID = 1` -> `ADMIN`
  - `ID = 2` -> `BILLING`
  - `ID = 3` -> `READ_ONLY`
- Check constraints and unique indexes forbid any fourth account or public signup.
- Refresh tokens are stored strictly as SHA-256 digests in `auth_refresh_tokens`.

### B. Store Settings
- Singleton table `store_settings` (`CHECK id = 1`) storing store identity, contact info, and receipt footer.

### C. Menu Catalog
- `categories` (one cover image path, sort sequence, active flag)
- `menu_items` (single dish photo, selling price, optional estimated cost, availability)

### D. Billing & Checkout
- `bill_counters`: Atomic per-date sequence counter (`INSERT ... ON DUPLICATE KEY UPDATE` / `PESSIMISTIC_WRITE`) vending daily invoice numbers (e.g. `SNS-20261120-0001`).
- `bills`: Supports `HELD`, `PAID`, `CANCELLED` states. Invoice numbers and store snapshots freeze upon payment settlement.
- `bill_items`: Frozen dish price and cost snapshots; generated columns `line_total` and `line_net_total`.
- `bill_payments`: 1-to-1 relationship with `bills`; generated column `change_amount = tendered_amount - amount`.

### E. Inventory & Ledger
- `inventory_items`: Tracks stock on hand with non-negative constraints (`current_qty >= 0`) and weighted moving-average unit cost.
- `stock_purchases` & `stock_purchase_lines`: Goods acquisition (increases assets without immediate P&L expense deduction).
- `stock_daily_entries`: Daily kitchen consumption (`used_qty`) and spoilage (`wasted_qty`) with UUID idempotency key.
- `stock_movements`: Immutable, signed ledger (`+` purchases, `-` daily usage/waste) with generated `cost_amount`.

### F. Overhead & Notifications
- `operating_expenses`: Records operational expenses (rent, utilities, wages).
- `notifications`: Alerts on low stock (`<= min_limit_qty`) and critical stock (`<= critical_limit_qty`). Generated column `open_inventory_item_id` enforces at most one open alert per item.
- `audit_logs`: Append-only audit trail with JSON diff snapshots.

### G. Reporting Views
- `vw_daily_financial_summary`: Computes daily net sales revenue, consumed stock cost, waste cost, stock adjustment losses, operating overhead, and estimated operating margin.
- `vw_daily_menu_item_sales`: Computes per-dish sales quantities, revenue, and contribution margins.

---

## 3. Directory Layout

```
backend/
├── pom.xml                                    # Maven configuration (Java 17, Spring Boot 3.3.5)
├── mvnw / mvnw.cmd                            # Maven wrapper executables
├── .mvn/wrapper/                              # Maven wrapper configuration
├── README.md                                  # Backend documentation
└── src/
    ├── main/
    │   ├── java/com/sambarspicy/
    │   │   ├── SambarSpicyApplication.java   # Spring Boot entry point
    │   │   ├── config/                        # JpaConfig, SecurityConfig
    │   │   ├── controller/                    # AuthController, AuthorizationProbeController
    │   │   ├── dto/                           # LoginRequest, UserDto, AuthResponse, etc.
    │   │   ├── model/                         # 17 JPA Entities / Models
    │   │   │   ├── User.java
    │   │   │   ├── AuthRefreshToken.java
    │   │   │   ├── StoreSetting.java
    │   │   │   ├── Category.java
    │   │   │   ├── MenuItem.java
    │   │   │   ├── BillCounter.java
    │   │   │   ├── Bill.java
    │   │   │   ├── BillItem.java
    │   │   │   ├── BillPayment.java
    │   │   │   ├── InventoryItem.java
    │   │   │   ├── StockPurchase.java
    │   │   │   ├── StockPurchaseLine.java
    │   │   │   ├── StockDailyEntry.java
    │   │   │   ├── StockMovement.java
    │   │   │   ├── OperatingExpense.java
    │   │   │   ├── Notification.java
    │   │   │   └── AuditLog.java
    │   │   ├── model/view/                    # Read-only View Entities
    │   │   │   ├── VwDailyFinancialSummary.java
    │   │   │   ├── VwDailyMenuItemSales.java
    │   │   │   └── DailyMenuItemSalesId.java
    │   │   ├── enums/                         # 7 Core Enums
    │   │   │   ├── UserRole.java
    │   │   │   ├── BillStatus.java
    │   │   │   ├── PaymentMethod.java
    │   │   │   ├── InventoryUnit.java
    │   │   │   ├── StockMovementType.java
    │   │   │   ├── NotificationSeverity.java
    │   │   │   └── NotificationStatus.java
    │   │   ├── exception/                     # GlobalExceptionHandler, TokenRefreshException
    │   │   ├── repository/                    # 19 Spring Data Repositories
    │   │   ├── security/                      # JWT Provider, CustomUserDetails, Filters
    │   │   └── service/                       # AuthService
    │   └── resources/
    │       ├── application.properties         # Production configuration with env fallbacks
    │       ├── application-dev.properties     # Development profile
    │       └── db/migration/
    │           └── V1__initial_schema.sql     # Flyway versioned migration script
    └── test/
        ├── java/com/sambarspicy/
        │   ├── SambarSpicyApplicationTests.java
        │   └── EntityMappingVerificationTest.java # Comprehensive mapping test suite
        └── resources/
            └── application-test.properties    # H2 in-memory test profile
```

---

## 4. Database Setup & Running the Application

The backend implements authentication, store settings, admin read endpoints, categories, menu items and billing endpoints. Admin user mutations remain unimplemented. Before processing live sales, validate checkout, receipt numbering, concurrency, authorization and recovery against a disposable MySQL database using the same schema and deployment configuration; the H2 test profile does not verify MySQL locking or Flyway migrations.

The default credentials and JWT secret are for local development only. For the `prod` profile, configure a unique random `JWT_SECRET`, `JWT_COOKIE_SECURE=true`, HTTPS and non-default bootstrap passwords; startup rejects the development JWT secret and insecure cookies in that profile. Set `APP_CORS_ALLOWED_ORIGINS` to the exact frontend origins, separated by commas. Local defaults allow only `http://localhost:3000` and `http://127.0.0.1:3000`. Keep the JWT secret stable across restarts and out of source control. Existing accounts retain their passwords and active state when the server restarts.

### Optional Redis cache for the customer menu

The customer menu can use Redis (including Render Key Value) as a shared cache.
Set these **backend-only** environment variables:

```properties
PUBLIC_MENU_CACHE_ENABLED=true
REDIS_URL=redis://localhost:6379
```

On Render, replace the example with the Key Value instance's internal URL when
the backend and Key Value are in the same region and workspace; keep any
connection credentials private. Use `rediss://` for providers that require TLS.
Redis is optional and is disabled by default. If it is unreachable, the public
menu falls back to MySQL; billing and checkout always use MySQL directly.
Use a separate Redis database for each environment, or configure a distinct
`PUBLIC_MENU_CACHE_NAMESPACE` (letters, digits, hyphens and underscores) for
each installation that shares a Redis instance.

`REDIS_DATABASE` selects the Redis logical database (default `0`), and
`REDIS_CONNECT_TIMEOUT` and `REDIS_TIMEOUT` control the connection and command
timeouts (default `500ms` each). Spring Boot uses the configured URL for host,
port, username and password; supply an authenticated provider URL via a secret
environment variable, not source control. URL-encode credentials containing
reserved characters. Set the database using `REDIS_DATABASE` rather than a URL
path. Use separate namespaces and credentials appropriate to each environment.
The optional [local Redis Compose file](../infra/docker-compose.redis.yml) binds
to `127.0.0.1:6380` by default and does not alter an existing Redis container.

The public menu, authenticated category lists and unfiltered authenticated
menu-item lists are cached in Redis, for up to 30 seconds. Menu-item cache
entries contain staff-visible estimated costs; restrict Redis access accordingly.
Filtered searches and detail reads continue to use MySQL.
Successful category, menu item and store updates invalidate the shared cache
after the database transaction commits. Redis connectivity failures can leave
an older shared value available until its short TTL expires. Customer browsers
may keep the public menu for five minutes, so prices and availability in an
already-open or recently reopened menu can lag behind admin changes by up to
approximately five minutes plus the Redis TTL. Checkout always verifies the
current database values.

### Option 1: Automatic Flyway Migration (Recommended)

For local development, copy `.env.example` to `.env` in this backend folder
and set `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` to the
existing MySQL account credentials. Spring Boot loads this file when launched
with the backend folder as the working directory. It uses Java properties
syntax: do not wrap values in quotes, and write a literal backslash as `\\`.
The file is excluded from Git; keep the password private. Existing environment
variables with the same names take precedence over the file.
1. Ensure MySQL 8.0+ is running.
2. Create the target database:
   ```sql
   CREATE DATABASE IF NOT EXISTS sambar_spicy CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. Set your environment variables (or rely on default localhost configuration):
   ```powershell
   $env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/sambar_spicy?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata&characterEncoding=UTF-8"
   $env:SPRING_DATASOURCE_USERNAME = "root"
   $env:SPRING_DATASOURCE_PASSWORD = "<your-mysql-password>"
   ```
4. Run the Spring Boot application:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
   Flyway applies pending migrations in order. On a fresh database V1 creates the initial schema, V2 adds half portions, and V3 adds menu dietary classification. Hibernate validates the schema on startup (`ddl-auto=validate`).

### Optional bill round-off

V4 adds a signed `round_off_amount` to bills and updates the total constraint. Existing bills keep a zero adjustment and retain their original totals. Before updating an existing installation, back up the database and test the migration on a disposable copy. Stop the old backend, deploy the updated backend and frontend together, then restart the backend with the existing database credentials so Flyway applies V4. Do not run the SQL manually alongside Flyway.

After startup, the billing screen enables round-off only when the backend advertises support. It is off by default and must be selected for each bill. When selected, the final amount rounds to the nearest rupee (50 paise rounds up); the signed adjustment is stored with the bill, shown at checkout and on receipts, and included in paid sales. Held bills remain unrounded. Item sales remain based on line amounts before the bill-level adjustment.

### Dietary classification on an existing database

Back up the database before upgrading. From PowerShell, this command prompts for the MySQL password and writes a backup file without placing the password in the command:

```powershell
mysqldump -u root -p --single-transaction --routines --triggers --events --result-file=sambar_spicy_before_dietary.sql sambar_spicy
```

Start the backend with the correct database credentials. Flyway should apply `V3__add_menu_item_dietary_type.sql` once. Check the startup log and verify the column and migration in MySQL:

```sql
SHOW COLUMNS FROM sambar_spicy.menu_items LIKE 'dietary_type';
SELECT version, description, success FROM sambar_spicy.flyway_schema_history ORDER BY installed_rank;
```

The column should be an enum with default `UNSPECIFIED`, and migration version `3` should have `success = 1`. Do not run the ALTER manually alongside Flyway: if the column already exists but V3 is not recorded, stop and reconcile the schema and migration history before restarting. Existing dishes remain unclassified; edit each dish in Admin and select its correct dietary type so it appears under the corresponding billing filter.

If startup reports `Access denied for user 'root'@'localhost' (using password: NO)`, MySQL is rejecting the default empty password. In the same PowerShell window, set the credentials for a MySQL account with access to `sambar_spicy`, then start the application again:

```powershell
$env:SPRING_DATASOURCE_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = "<your-mysql-password>"
.\mvnw.cmd spring-boot:run
```

Environment variables set this way apply only to the current terminal session. Never commit a real password to the project.

### Option 2: Manual SQL Import
Import the authoritative script directly via MySQL client:
```powershell
mysql -u root -p < Sambar_and_Spicy_Corrected_MySQL_Schema.sql
```

---

## 5. Verification & Tests

To execute the verification test suite:
```powershell
mvn clean test
```
The suite verifies:
- Exact mapping of all 17 tables and 2 views.
- Enum constants matching database check constraints.
- Generated stored columns configured as read-only (`insertable = false, updatable = false`).
- Monetary and quantity precision and scale (`DECIMAL(14,2)`, `DECIMAL(14,3)`, `DECIMAL(14,4)`).
- Temporal mapping with `LocalDate` and `LocalDateTime`.
- Spring Data repository discovery for all 19 interfaces.

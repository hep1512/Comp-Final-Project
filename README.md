# Supermarket Management System

A web-based supermarket management system built with Kotlin and Ktor. Customers can register, browse products, manage a cart, check out, view orders, and receive product recommendations. Employees and admins can manage stock, handle warehouse picklists, review analytics, and manage users/products depending on their role.

## Features

### User Features

- Registration and login/logout with hashed passwords and session cookies.
- Product browsing with product list and product detail pages.
- Shopping cart actions: add items, update quantities, and remove items.
- Checkout flow that creates orders and clears the user's cart.
- Order history page showing previous orders, dates, statuses, and totals.
- Customer dashboard recommendations based on order history, product popularity, category trends, price fit, and stock availability.

### Employee Features

- Inventory management for updating stock status: In stock, Low stock, or Out of stock.
- Warehouse picklists showing active checkout orders, line items, customer names, stock availability, and order totals.
- Picklist status updates for order progress, including placed, picking, packed, and delivered.
- Staff dashboard analytics summary for sales, revenue, units sold, average basket value, and category performance.

### Admin Features

- Admin dashboard with total orders, total sales, product count, low stock count, and inventory summary.
- User management page for viewing registered users and their roles.
- Add product page for creating new catalogue items with name, description, category, price, SKU, and stock quantity.
- Seed sample products action for adding extra supermarket items and categories.
- Management analytics page with searchable/filterable product sales, category sales, low-stock alerts, and date filters.
- CSV download for filtered analytics reports.
- Role-based route protection for admin-only and staff-only pages.

## Tech Stack

- Kotlin
- Ktor with Netty
- Gradle 9.3 wrapper
- PostgreSQL
- Exposed SQL
- HikariCP
- Kotlinx serialization
- Ktor sessions, status pages, JWT authentication support, and YAML configuration

## Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/hep1512/Comp-Final-Project
   cd Comp-Final-Project
   ```

2. Install JDK 21 and make sure `JAVA_HOME` points to it. The Gradle build is configured with `jvmToolchain(21)`.

3. Check the database settings in `src/main/resources/application.yaml`. The application expects a PostgreSQL database connection.

4. Build and run the project:

   ```bash
   ./gradlew run
   ```

   On Windows:

   ```powershell
   .\gradlew.bat run
   ```

5. Open the app in a browser:

   ```text
   http://localhost:8080
   ```

## Usage

- Login: `/login`
- Register: `/register`
- Dashboard: `/dashboard`
- Product list: `/products`
- Product details: `/products/{id}`
- Cart: `/cart`
- Checkout: `/checkout`
- Order history: `/orders`
- Inventory management, Employee/Admin only: `/inventory`
- Warehouse picklists, Employee/Admin only: `/picklists`
- Admin dashboard, Admin only: `/admin`
- User management, Admin only: `/admin/users`
- Add product, Admin only: `/admin/products/add`
- Analytics, Admin only: `/analytics`
- Analytics CSV download, Admin only: `/analytics/download`

Routes are protected using session and role checks. Users without the required role are shown a forbidden page.

## Project Structure

- `com.supermarket.Application`: Starts the Ktor server, configures database access, sessions, JWT auth, JSON handling, status pages, and routes.
- `com.supermarket.auth`: JWT configuration and verifier setup.
- `com.supermarket.data`: Simple application-facing helpers for carts, products, orders, stock, and user lookup.
- `com.supermarket.database`: PostgreSQL connection setup and Exposed table definitions.
- `com.supermarket.models`: Shared data models such as users, sessions, products, cart lines, and order summaries.
- `com.supermarket.repositories`: Database-backed business logic for users, products, stock, orders, analytics, recommendations, and picklists.
- `com.supermarket.routes`: HTTP route definitions and generated HTML page templates.
- `src/main/resources`: Ktor application configuration and logging configuration.
- `src/main/test`: Basic Kotlin test cases.

## File System Map

```text
Comp-Final-Project/
|-- .gitignore
|-- README.md
|-- build.gradle.kts
|-- gradle.properties
|-- gradlew
|-- gradlew.bat
|-- jobstories.txt
|-- ktor-sample.zip
|-- settings.gradle.kts
|-- gradle/
|   |-- libs.versions.toml
|   `-- wrapper/
|       |-- gradle-wrapper.jar
|       `-- gradle-wrapper.properties
`-- src/
    `-- main/
        |-- kotlin/
        |   `-- com/
        |       `-- supermarket/
        |           |-- Application.kt
        |           |-- auth/
        |           |   `-- JwtConfig.kt
        |           |-- data/
        |           |   |-- ShopStore.kt
        |           |   `-- UserStore.kt
        |           |-- database/
        |           |   |-- DatabaseConfig.kt
        |           |   `-- Tables.kt
        |           |-- models/
        |           |   |-- CartLine.kt
        |           |   |-- OrderSummary.kt
        |           |   |-- Product.kt
        |           |   |-- Session.kt
        |           |   `-- User.kt
        |           |-- repositories/
        |           |   |-- AnalyticsRepository.kt
        |           |   |-- OrderRepository.kt
        |           |   |-- PickListRepository.kt
        |           |   |-- ProductRepository.kt
        |           |   |-- RecommendationRepository.kt
        |           |   |-- StockRepository.kt
        |           |   `-- UserRepository.kt
        |           `-- routes/
        |               |-- Pages.kt
        |               `-- Routes.kt
        |-- resources/
        |   |-- application.yaml
        |   `-- logback.xml
        `-- test/
            `-- ApplicationTest.kt
```

Generated build/cache folders such as `build/`, `.gradle/`, and `.gradle-user-home/` are not part of the source map.

## Developers

- Henry Payne - Backend, routing, and session management
- Junhao Shen - Frontend, Testing, HTML
- Charlelie Chagas Poinsot - Database design and data management

## Security

- Passwords are hashed before storage using `UserRepository.hashPassword`.
- Ktor sessions are stored in HTTP-only cookies.
- JWT configuration is loaded from `application.yaml`.
- Role-based access control protects employee and admin routes.

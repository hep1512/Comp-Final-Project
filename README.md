# Supermarket Management System

A web-based supermarket management system built using **Ktor** and **Kotlin**. This system allows users to browse products, manage a shopping cart, checkout, view orders, and for employees/admins to manage inventory and users.  

---

## Features

### User Features
- User authentication: Login/logout functionality with secure password hashing and secure session cookies using JWT.
- Product browsing: View all products and detailed information about them.
- Shopping cart: Add, update, and remove items from the cart.
- Checkout: Complete purchases and view order history.

### Employee Features
- Inventory management: Update product stock status (In stock, Low stock, Out of stock).

### Admin Features
- User management: View all registered users and their roles.
- Access control: Restrict specific routes based on user roles (EMPLOYEE, ADMIN).

---

## Installation

1. Clone the repository:
   git clone https://github.com/hep1512/Comp-Final-Project
   cd Comp-Final-Project

2. Ensure you have JDK 17+ installed.

3. Build and run the project:
   ./gradlew run

4. Access the app: Open your browser at http://localhost:8080

---

## Project Structure

- com.supermarket.routes: All HTTP route handlers and session logic.
- com.supermarket.data: Handles data operations such as user management, cart actions, and product stock updates.
- com.supermarket.models: Contains data models like UserSession and Role.

---

## Usage

- Login: /login
- Dashboard: /dashboard
- Product list: /products
- Product details: /products/{id}
- Cart: /cart
- Checkout: /checkout
- Order history: /orders
- Inventory management (Employee/Admin only): /inventory
- Admin dashboard: /admin
- User management (Admin only): /admin/users

> Routes are protected based on session and role. Users without the required role will see a forbidden page.

---

## Developers

- Henry Payne – Backend, Routing & Session Management
- Junhao Shen – Frontend, Templates & HTML
- Charlelie Chagas Poinsot – Database Design & Data Management
- Ella Ford - Testing, User feedback **UPDATE**

---

## Security

- Passwords are stored as hashes using a custom hashPassword function.
- Session-based authentication with role-based access control.
- Secure JWT token based session management

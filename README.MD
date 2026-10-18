# Web Based Banking System

An academic full-stack banking simulation built with **Spring Boot 3 (Java 21)** and **React 18 (Vite, Bootstrap 5)**.

> **Demonstration System Notice:** All customer data, account numbers, balances, transfers, and billers are purely fictional demonstration records.

---

## Architecture Overview

- **Backend:** Spring Boot 3.3.4, Java 21, Spring Data JPA (Hibernate ORM), Spring Security, JJWT, Bucket4j rate limiting, OpenPDF, Apache Commons CSV, MapStruct, SpringDoc OpenAPI.
- **ORM & Database:** Hibernate ORM with `spring.jpa.hibernate.ddl-auto: update`. No manual migration scripts or raw queries needed — database tables and relationships are created and maintained automatically by Spring Boot's JPA entities on both **MSSQL** and **MySQL**.
- **Frontend:** React 18, Vite, Bootstrap 5 + React-Bootstrap + Bootstrap Icons, React Router v6, Axios, react-hook-form. (Strict zero-custom-CSS policy — native Bootstrap components and utilities only).
- **Configuration:** Strictly isolated configuration via `backend/.env` and `frontend/.env`.

---

## Getting Started

### Prerequisites

- **Java Development Kit (JDK 21+)**
- **Node.js (v18+ or v20+ LTS)** & **npm**
- **Maven** (or an IDE like IntelliJ IDEA / Eclipse / VS Code with Maven extension)
- Local Database: **Microsoft SQL Server (e.g. SQLEXPRESS)** or **MySQL 8+**

---

### Step 1: Database Setup

Using SQL Server Management Studio (SSMS) or `sqlcmd`, create the database once:

```sql
CREATE DATABASE banking_db;
```

Hibernate ORM (`ddl-auto: update`) will automatically generate all necessary tables and indexes upon application startup.

---

### Step 2: Backend Configuration & Startup

1. Copy the example environment file:
   ```bash
   cd backend
   cp .env.example .env
   ```

2. Review [backend/.env](file:///h:/Software%20Projects/Online%20Banking%20System/backend/.env) to ensure database credentials match your local instance:
   ```env
   # SQL Server (SQLEXPRESS) example:
   DB_URL=jdbc:sqlserver://localhost:1433;databaseName=banking_db;encrypt=true;trustServerCertificate=true;instanceName=SQLEXPRESS
   DB_USERNAME=sa
   DB_PASSWORD=YourPassword123!
   ```

3. Run the backend:
   ```bash
   .\mvnw.cmd spring-boot:run
   ```
   *Or run `BankingSystemApplication.java` directly inside your IDE.*

Swagger API documentation is available at: `http://localhost:8080/swagger-ui`

---

### Step 3: Frontend Setup & Startup

1. Copy the frontend environment file:
   ```bash
   cd frontend
   cp .env.example .env
   ```

2. Install dependencies and start the Vite development server:
   ```bash
   npm install
   npm run dev
   ```

3. Access the application in your browser at `http://localhost:5173`.

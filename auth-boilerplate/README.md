# auth-boilerplate

A production-ready Spring Boot REST API that provides user registration and HTTP Basic Authentication backed by PostgreSQL. Passwords are never stored in plain text — they are hashed with BCrypt before being persisted.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Architecture Overview](#architecture-overview)
- [Database Schema](#database-schema)
- [API Reference](#api-reference)
- [Validation Rules](#validation-rules)
- [Error Handling](#error-handling)
- [Logging](#logging)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration Reference](#configuration-reference)
- [How HTTP Basic Auth Works](#how-http-basic-auth-works)
- [Security Notes](#security-notes)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.3 |
| Security | Spring Security (HTTP Basic Auth) |
| ORM | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Password Hashing | BCrypt (via `BCryptPasswordEncoder`) |
| Validation | Jakarta Bean Validation |
| Boilerplate reduction | Lombok |
| Build tool | Maven |

---

## Project Structure

```
auth-boilerplate/
├── .env.example                          # Environment variable template
├── pom.xml                               # Maven dependencies and build config
└── src/
    └── main/
        ├── java/com/authboilerplate/
        │   │
        │   ├── AuthBoilerplateApplication.java   # Application entry point
        │   │
        │   ├── config/
        │   │   └── SecurityConfig.java           # Spring Security configuration
        │   │
        │   ├── controller/
        │   │   └── AuthController.java           # REST endpoints (register, me)
        │   │
        │   ├── dto/                              # Data Transfer Objects
        │   │   ├── RegisterRequest.java          # Registration request body
        │   │   ├── AuthResponse.java             # Auth success response payload
        │   │   └── ApiResponse.java              # Generic API response wrapper
        │   │
        │   ├── entity/
        │   │   └── User.java                     # JPA entity mapped to "users" table
        │   │
        │   ├── exception/
        │   │   ├── UserAlreadyExistsException.java   # Thrown on duplicate username/email
        │   │   ├── UserNotFoundException.java         # Thrown when user lookup fails
        │   │   └── GlobalExceptionHandler.java        # @RestControllerAdvice — maps exceptions to HTTP responses
        │   │
        │   ├── repository/
        │   │   └── UserRepository.java           # JPA repository (CRUD + custom finders)
        │   │
        │   └── service/
        │       ├── AuthService.java              # Registration and profile retrieval logic
        │       └── UserDetailsServiceImpl.java   # Spring Security user loading bridge
        │
        └── resources/
            └── application.properties            # App configuration (uses env vars)
```

### Layer Responsibilities

```
Request
  │
  ▼
AuthController          — Accepts HTTP requests, validates input, delegates to service
  │
  ▼
AuthService             — Business logic: duplicate checks, password hashing, user save
  │
  ▼
UserRepository          — Spring Data JPA interface; talks to PostgreSQL
  │
  ▼
PostgreSQL (users table)
```

Spring Security sits **across** all requests:
- `SecurityConfig` declares which routes are public and which require authentication
- `UserDetailsServiceImpl` bridges the framework's auth mechanism to our `UserRepository`
- On every protected request, Spring's `BasicAuthenticationFilter` decodes the `Authorization` header and calls `UserDetailsServiceImpl.loadUserByUsername()` automatically

---

## Architecture Overview

### Registration Flow

```
POST /api/v1/auth/register
        │
        ▼
  AuthController.register()
        │  @Valid — triggers Jakarta Bean Validation
        ▼
  AuthService.register()
        │
        ├─ userRepository.existsByUsername()  →  409 Conflict if taken
        ├─ userRepository.existsByEmail()     →  409 Conflict if taken
        │
        ├─ BCryptPasswordEncoder.encode(rawPassword)
        │       ↳ Hashed password stored in DB — raw password is never persisted
        │
        └─ userRepository.save(user)
                │
                ▼
          201 Created  +  { username, email, message }
```

### Authentication Flow (every protected request)

```
GET /api/v1/auth/me
  Header: Authorization: Basic base64(username:password)
        │
        ▼
  BasicAuthenticationFilter  (Spring Security — automatic)
        │
        ├─ Decodes Base64 → extracts username:password
        ├─ Calls UserDetailsServiceImpl.loadUserByUsername(username)
        │       ↳ Fetches User from PostgreSQL
        ├─ BCryptPasswordEncoder.matches(rawPassword, storedHash)
        │
        ├─ 401 Unauthorized  →  if credentials are wrong
        │
        └─ Sets SecurityContext → request proceeds
                │
                ▼
          AuthController.getCurrentUser()
                │
                ▼
          200 OK  +  { username, email, message }
```

---

## Database Schema

The schema is auto-managed by Hibernate (`ddl-auto=update`). The table created is:

```sql
CREATE TABLE users (
    id         BIGSERIAL     PRIMARY KEY,
    username   VARCHAR(50)   NOT NULL UNIQUE,
    email      VARCHAR(100)  NOT NULL UNIQUE,
    password   VARCHAR(255)  NOT NULL,          -- BCrypt hash, never plain text
    enabled    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP     NOT NULL,
    updated_at TIMESTAMP
);
```

| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL | Auto-incremented primary key |
| `username` | VARCHAR(50) | Unique, 3–50 chars, alphanumeric + underscore only |
| `email` | VARCHAR(100) | Unique, must be a valid email format |
| `password` | VARCHAR(255) | BCrypt hash (never stored as plain text) |
| `enabled` | BOOLEAN | `true` by default; can be used to soft-disable accounts |
| `created_at` | TIMESTAMP | Set automatically on insert |
| `updated_at` | TIMESTAMP | Updated automatically on every save |

---

## API Reference

### Base URL

```
http://localhost:8080/api/v1/auth
```

---

### POST `/register` — Register a new user

**Auth required:** No (public)

**Request Body** (`application/json`)

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "mypassword123"
}
```

**Success Response — `201 Created`**

```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "username": "alice",
    "email": "alice@example.com",
    "message": "User registered successfully"
  }
}
```

**Error — `400 Bad Request`** (validation failure)

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "password": "Password must be at least 8 characters",
    "email": "Invalid email format"
  }
}
```

**Error — `409 Conflict`** (duplicate username or email)

```json
{
  "success": false,
  "message": "Username 'alice' is already taken",
  "data": null
}
```

---

### GET `/me` — Get authenticated user's profile

**Auth required:** Yes — HTTP Basic Auth (`Authorization: Basic base64(username:password)`)

**Success Response — `200 OK`**

```json
{
  "success": true,
  "message": "Authenticated successfully",
  "data": {
    "username": "alice",
    "email": "alice@example.com",
    "message": "Authenticated successfully"
  }
}
```

**Error — `401 Unauthorized`** (wrong credentials or missing header)

Spring Security returns a standard `401` with a `WWW-Authenticate: Basic` header.

---

## Validation Rules

| Field | Rules |
|---|---|
| `username` | Required · 3–50 characters · only letters, digits, underscores (`^[a-zA-Z0-9_]+$`) |
| `email` | Required · must be a valid email address |
| `password` | Required · minimum 8 characters |

Violations return `400 Bad Request` with a field-by-field error map (see [API Reference](#api-reference)).

---

## Error Handling

All errors are returned in the same `ApiResponse` envelope so clients have a consistent structure to parse.

| Exception | HTTP Status | Trigger |
|---|---|---|
| `UserAlreadyExistsException` | `409 Conflict` | Duplicate username or email on register |
| `UserNotFoundException` | `404 Not Found` | User record missing after authentication |
| `MethodArgumentNotValidException` | `400 Bad Request` | Bean Validation failure on request body |
| Any other `Exception` | `500 Internal Server Error` | Unexpected server-side error |

The `GlobalExceptionHandler` (`@RestControllerAdvice`) centralises all of this — no try/catch blocks in controllers or services.

---

## Logging

SLF4J with Logback is used throughout (via Lombok's `@Slf4j`). Log levels by package:

| Package / Category | Level | What is logged |
|---|---|---|
| `com.authboilerplate` | `DEBUG` | All app-level events (registrations, logins, profile fetches) |
| `org.springframework.security` | `INFO` | Security filter decisions |
| `org.hibernate.SQL` | `WARN` | SQL queries suppressed in normal operation |

**Log format (console):**
```
2026-02-26 10:42:01 [main] INFO  c.a.service.AuthService - User registered successfully: alice
```

To change the log level, edit `application.properties`:
```properties
logging.level.com.authboilerplate=INFO
```

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java JDK | 17 or later |
| Maven | 3.6 or later |
| PostgreSQL | 13 or later |

---

## Getting Started

### 1. Clone the repository

```bash
git clone <repository-url>
cd auth-boilerplate
```

### 2. Create the PostgreSQL database

```bash
psql -U postgres -c "CREATE DATABASE auth_db;"
```

### 3. Set up environment variables

```bash
cp .env.example .env
```

Edit `.env` and fill in your PostgreSQL credentials:

```env
DB_USERNAME=postgres
DB_PASSWORD=your_actual_password
```

Export them before running (or use a tool like `direnv`):

```bash
export $(cat .env | xargs)
```

### 4. Run the application

```bash
mvn spring-boot:run
```

The server starts at `http://localhost:8080`. Hibernate automatically creates the `users` table on first run.

### 5. Try it out

**Register a user:**
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"mypassword123"}' \
  | jq .
```

**Access the protected profile endpoint:**
```bash
curl -s http://localhost:8080/api/v1/auth/me \
  -u alice:mypassword123 \
  | jq .
```

**Wrong password (expect 401):**
```bash
curl -s http://localhost:8080/api/v1/auth/me \
  -u alice:wrongpassword \
  | jq .
```

---

## Configuration Reference

All settings live in `src/main/resources/application.properties`. Sensitive values are read from environment variables with fallback defaults shown in parentheses.

| Property | Env Var | Default | Description |
|---|---|---|---|
| `server.port` | — | `8080` | HTTP port the server listens on |
| `spring.datasource.url` | — | `jdbc:postgresql://localhost:5432/auth_db` | JDBC connection URL |
| `spring.datasource.username` | `DB_USERNAME` | `postgres` | Database user |
| `spring.datasource.password` | `DB_PASSWORD` | `postgres` | Database password |
| `spring.jpa.hibernate.ddl-auto` | — | `update` | Schema strategy (`update` / `validate` / `none`) |
| `spring.jpa.show-sql` | — | `false` | Print SQL to console |
| `logging.level.com.authboilerplate` | — | `DEBUG` | App log verbosity |

> **Production tip:** Change `ddl-auto` to `validate` (or use Flyway/Liquibase) once your schema is stable, to prevent accidental schema changes.

---

## How HTTP Basic Auth Works

HTTP Basic Authentication is a standard protocol defined in [RFC 7617](https://datatracker.ietf.org/doc/html/rfc7617).

1. The client concatenates username and password with a colon: `alice:mypassword123`
2. The result is Base64-encoded: `YWxpY2U6bXlwYXNzd29yZDEyMw==`
3. The encoded string is sent in the `Authorization` header on every request:
   ```
   Authorization: Basic YWxpY2U6bXlwYXNzd29yZDEyMw==
   ```
4. Spring Security's `BasicAuthenticationFilter` decodes the header, loads the user from the database, and verifies the password against the stored BCrypt hash.
5. If credentials are valid, the request proceeds. If not, `401 Unauthorized` is returned.

> **Note:** Basic Auth transmits credentials on every request. Always use **HTTPS in production** to prevent credentials from being intercepted in transit.

---

## Security Notes

| Concern | How it is addressed |
|---|---|
| Plain-text passwords | Never stored — BCrypt hash is saved instead |
| Brute-force attacks | BCrypt's cost factor (default 10) makes hashing slow deliberately |
| Credential interception | Use HTTPS in production (Basic Auth over plain HTTP is insecure) |
| Session fixation | Sessions are stateless (`STATELESS` policy) — no server-side session is created |
| CSRF | Disabled — not relevant for stateless REST APIs with no browser cookies |
| Duplicate accounts | Checked before insert — returns `409` rather than leaking existence via timing |

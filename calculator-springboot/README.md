# calculator-springboot

A clean, production-structured REST API for basic arithmetic built with Spring Boot. A single endpoint accepts two operands in the request body and an `operation` query parameter that controls which calculation to run. All inputs are validated, all errors are handled centrally, and every response follows a consistent JSON envelope.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Architecture Overview](#architecture-overview)
- [API Reference](#api-reference)
- [Request & Response Format](#request--response-format)
- [Error Handling](#error-handling)
- [Validation Rules](#validation-rules)
- [Logging](#logging)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration Reference](#configuration-reference)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.3 |
| Data validation | Jakarta Bean Validation |
| Boilerplate reduction | Lombok |
| Build tool | Maven |

---

## Project Structure

```
calculator-springboot/
├── .env.example                          # Environment variable template
├── pom.xml                               # Maven dependencies and build config
└── src/
    └── main/
        ├── java/com/calculator/
        │   │
        │   ├── CalculatorApplication.java        # Application entry point
        │   │
        │   ├── config/
        │   │   └── StringToOperationConverter.java   # Accepts lowercase ?operation= values
        │   │
        │   ├── controller/
        │   │   └── CalculatorController.java     # POST /api/v1/calculator/calculate
        │   │
        │   ├── dto/
        │   │   ├── CalculationRequest.java        # Request body: operandA, operandB
        │   │   ├── CalculationResult.java         # Response data: operands, operation, result
        │   │   └── ApiResponse.java               # Generic response envelope ApiResponse<T>
        │   │
        │   ├── enums/
        │   │   └── Operation.java                 # ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO, POWER
        │   │
        │   ├── exception/
        │   │   ├── DivisionByZeroException.java   # Thrown when operandB is 0 on divide/modulo
        │   │   └── GlobalExceptionHandler.java    # @RestControllerAdvice — maps exceptions to HTTP
        │   │
        │   └── service/
        │       └── CalculatorService.java         # Arithmetic logic via switch expression
        │
        └── resources/
            └── application.properties            # App configuration (reads from env vars)
```

### Layer Responsibilities

```
Request
  │
  ▼
CalculatorController      — reads ?operation= and request body, delegates to service
  │
  ▼
CalculatorService         — executes the arithmetic, raises domain exceptions
  │
  ▼
CalculationResult (DTO)   — structured result returned up the chain
  │
  ▼
ApiResponse<T>            — consistent JSON envelope sent to the client
```

`GlobalExceptionHandler` sits across all requests and maps every exception type to the correct HTTP response — no try/catch blocks in controller or service.

`StringToOperationConverter` is registered automatically by Spring and converts the `?operation=add` query string to `Operation.ADD` before the controller method is called.

---

## Architecture Overview

### Request Flow

```
POST /api/v1/calculator/calculate?operation=divide
  Body: { "operandA": 10, "operandB": 0 }
          │
          ├─ StringToOperationConverter converts "divide" → Operation.DIVIDE
          │    Unknown values → 400 Bad Request via GlobalExceptionHandler
          │
          ├─ Jakarta Validation validates CalculationRequest body
          │    Null / missing fields → 400 Bad Request via GlobalExceptionHandler
          │
          ▼
  CalculatorController.calculate()
          │
          ▼
  CalculatorService.calculate(Operation.DIVIDE, 10.0, 0.0)
          │
          └─ switch(DIVIDE) → operandB == 0 → throws DivisionByZeroException
                    │
                    ▼
          GlobalExceptionHandler.handleDivisionByZero()
                    │
                    ▼
          400 Bad Request
          { "success": false, "message": "Division by zero is not allowed", "data": null }
```

---

## API Reference

### Base URL

```
http://localhost:8080/api/v1/calculator
```

---

### POST `/calculate`

The single endpoint for all arithmetic operations. Select the operation via a required query parameter and provide the two numbers in the JSON body.

| Detail | Value |
|---|---|
| Method | `POST` |
| URL | `/api/v1/calculator/calculate` |
| Query param | `operation` — **required** |
| Content-Type | `application/json` |

**Supported `operation` values**

| Value | Formula | Error condition |
|---|---|---|
| `add` | `a + b` | — |
| `subtract` | `a - b` | — |
| `multiply` | `a * b` | — |
| `divide` | `a / b` | `400` if `b == 0` |
| `modulo` | `a % b` | `400` if `b == 0` |
| `power` | `a ** b` | — |

Both lowercase (`add`) and uppercase (`ADD`) values are accepted.

---

**200 OK**
```bash
curl -s -X POST "http://localhost:8080/api/v1/calculator/calculate?operation=add" \
  -H "Content-Type: application/json" \
  -d '{"operandA": 12, "operandB": 8}' | jq .
```
```json
{
  "success": true,
  "message": "add performed successfully",
  "data": {
    "operandA": 12.0,
    "operandB": 8.0,
    "operation": "addition",
    "result": 20.0
  }
}
```

**400 Bad Request — division by zero**
```bash
curl -s -X POST "http://localhost:8080/api/v1/calculator/calculate?operation=divide" \
  -H "Content-Type: application/json" \
  -d '{"operandA": 5, "operandB": 0}' | jq .
```
```json
{
  "success": false,
  "message": "Division by zero is not allowed",
  "data": null
}
```

**400 Bad Request — invalid operation**
```bash
curl -s -X POST "http://localhost:8080/api/v1/calculator/calculate?operation=sqrt" \
  -H "Content-Type: application/json" \
  -d '{"operandA": 9, "operandB": 3}' | jq .
```
```json
{
  "success": false,
  "message": "Invalid value 'sqrt' for parameter 'operation'. Accepted values: add, subtract, multiply, divide, modulo, power",
  "data": null
}
```

**400 Bad Request — missing operation param**
```bash
curl -s -X POST "http://localhost:8080/api/v1/calculator/calculate" \
  -H "Content-Type: application/json" \
  -d '{"operandA": 5, "operandB": 3}' | jq .
```
```json
{
  "success": false,
  "message": "Required query parameter 'operation' is missing",
  "data": null
}
```

**400 Bad Request — validation failure**
```bash
curl -s -X POST "http://localhost:8080/api/v1/calculator/calculate?operation=add" \
  -H "Content-Type: application/json" \
  -d '{"operandA": 5}' | jq .
```
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "operandB": "operandB is required"
  }
}
```

---

## Request & Response Format

### Request body

```json
{
  "operandA": <number>,
  "operandB": <number>
}
```

Both fields are required. Non-numeric values cause a `400` with a descriptive message.

### Success response

```json
{
  "success": true,
  "message": "<operation> performed successfully",
  "data": {
    "operandA": <number>,
    "operandB": <number>,
    "operation": "<operation label>",
    "result": <number>
  }
}
```

### Error response

```json
{
  "success": false,
  "message": "<error description>",
  "data": null
}
```

Every response uses the same `ApiResponse<T>` envelope. Clients only need to check `success`.

---

## Error Handling

All errors are centralised in `GlobalExceptionHandler` (`@RestControllerAdvice`) — no try/catch in controllers or services.

| Scenario | HTTP Status | Handler method |
|---|---|---|
| `operandB == 0` on `divide` or `modulo` | `400 Bad Request` | `handleDivisionByZero` |
| Null or missing body fields | `400 Bad Request` | `handleValidationErrors` |
| Unknown `operation` value | `400 Bad Request` | `handleTypeMismatch` |
| Missing `operation` query param | `400 Bad Request` | `handleMissingParam` |
| Malformed JSON body | `400 Bad Request` | `handleUnreadableMessage` |
| Any unhandled exception | `500 Internal Server Error` | `handleGenericException` |

---

## Validation Rules

| Input | Where validated | Rule |
|---|---|---|
| `operation` query param | `StringToOperationConverter` | Must be one of: `add`, `subtract`, `multiply`, `divide`, `modulo`, `power` (case-insensitive) |
| `operandA` | Jakarta `@NotNull` | Required — must be present in the request body |
| `operandB` | Jakarta `@NotNull` | Required — must be present in the request body |
| `operandB` when `operation=divide` | `CalculatorService` | Must not be `0` |
| `operandB` when `operation=modulo` | `CalculatorService` | Must not be `0` |

---

## Logging

SLF4J with Logback is used throughout (via Lombok `@Slf4j`).

**Log format:**
```
2026-02-26 10:42:01 [http-nio-8080-exec-1] INFO  c.c.controller.CalculatorController - Calculate request: operation=add, operandA=12.0, operandB=8.0
2026-02-26 10:42:01 [http-nio-8080-exec-1] INFO  c.c.service.CalculatorService - Calculating: 12.0 add 8.0
2026-02-26 10:42:01 [http-nio-8080-exec-1] DEBUG c.c.service.CalculatorService - addition: 12.0 8.0 = 20.0
```

| Logger | Default level | What is logged |
|---|---|---|
| `com.calculator` | `DEBUG` (via `LOG_LEVEL`) | All controller requests, service calls, warnings, errors |
| `org.springframework.web` | `INFO` | Spring MVC internals |

Set `LOG_LEVEL=INFO` in `.env` to suppress debug lines from the service layer in production.

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java JDK | 17 or later |
| Maven | 3.6 or later |

---

## Getting Started

### 1. Navigate to the project folder

```bash
cd calculator-springboot
```

### 2. Set up environment variables

```bash
cp .env.example .env
# Defaults work for local development — edit only if needed
export $(cat .env | xargs)
```

### 3. Build and run

```bash
mvn spring-boot:run
```

The API is available at `http://localhost:8080`.

### 4. Try it out

All operations share the same URL — only `?operation=` changes.

```bash
BASE="http://localhost:8080/api/v1/calculator/calculate"

curl -s -X POST "$BASE?operation=add"      -H "Content-Type: application/json" -d '{"operandA": 15, "operandB": 7}'  | jq .
curl -s -X POST "$BASE?operation=subtract" -H "Content-Type: application/json" -d '{"operandA": 15, "operandB": 7}'  | jq .
curl -s -X POST "$BASE?operation=multiply" -H "Content-Type: application/json" -d '{"operandA": 6,  "operandB": 7}'  | jq .
curl -s -X POST "$BASE?operation=divide"   -H "Content-Type: application/json" -d '{"operandA": 22, "operandB": 7}'  | jq .
curl -s -X POST "$BASE?operation=modulo"   -H "Content-Type: application/json" -d '{"operandA": 17, "operandB": 5}'  | jq .
curl -s -X POST "$BASE?operation=power"    -H "Content-Type: application/json" -d '{"operandA": 2,  "operandB": 10}' | jq .

# Error cases
curl -s -X POST "$BASE?operation=divide"   -H "Content-Type: application/json" -d '{"operandA": 5, "operandB": 0}'  | jq .  # 400 division by zero
curl -s -X POST "$BASE?operation=sqrt"     -H "Content-Type: application/json" -d '{"operandA": 9, "operandB": 3}'  | jq .  # 400 invalid operation
curl -s -X POST "$BASE"                    -H "Content-Type: application/json" -d '{"operandA": 5, "operandB": 3}'  | jq .  # 400 missing param
```

---

## Configuration Reference

| Property | Env Var | Default | Description |
|---|---|---|---|
| `server.port` | `SERVER_PORT` | `8080` | HTTP port the server listens on |
| `logging.level.com.calculator` | `LOG_LEVEL` | `DEBUG` | App log verbosity |

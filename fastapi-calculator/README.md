# fastapi-calculator

A clean, production-structured REST API for basic arithmetic built with FastAPI and Pydantic v2. A single endpoint accepts two operands in the request body and an `operation` query parameter that controls which calculation to run. All inputs are validated, all errors are handled centrally, and every response follows a consistent JSON envelope.

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
- [Interactive Docs](#interactive-docs)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Python 3.11+ |
| Framework | FastAPI 0.109 |
| Data validation | Pydantic v2 |
| Settings management | pydantic-settings |
| ASGI server | Uvicorn |
| Environment variables | python-dotenv |

---

## Project Structure

```
fastapi-calculator/
├── .env.example                   # Environment variable template
├── .gitignore
├── requirements.txt               # Python dependencies
├── main.py                        # App entry point, exception handlers, /health
│
└── app/
    ├── __init__.py
    │
    ├── api/                       # Transport layer — routing only
    │   ├── __init__.py
    │   ├── router.py              # Root API router  (prefix: /api)
    │   └── v1/
    │       ├── __init__.py
    │       ├── router.py          # v1 router         (prefix: /api/v1)
    │       └── routes/
    │           ├── __init__.py
    │           └── calculator.py  # Single /calculate endpoint
    │
    ├── core/                      # Cross-cutting concerns
    │   ├── __init__.py
    │   ├── config.py              # Settings loaded from env / .env file
    │   └── logging_config.py     # Log format and level setup
    │
    ├── exceptions/                # Custom exception types
    │   ├── __init__.py
    │   └── calculator_exceptions.py  # CalculatorError, DivisionByZeroError, InvalidOperandError
    │
    ├── models/                    # Pydantic schemas
    │   ├── __init__.py
    │   ├── enums.py               # Operation enum (add, subtract, multiply, divide, modulo, power)
    │   ├── request.py             # CalculationRequest
    │   └── response.py            # CalculationResult, ApiResponse[T]
    │
    └── services/                  # Business logic
        ├── __init__.py
        └── calculator_service.py  # calculate() dispatcher + private operation methods
```

### Layer Responsibilities

```
HTTP Request
     │
     ▼
routes/calculator.py     — validates query param + body, delegates to service
     │
     ▼
CalculatorService        — dispatches to correct operation, raises domain exceptions
     │
     ▼
CalculationResult        — structured Pydantic model returned up the chain
     │
     ▼
ApiResponse[T]           — consistent JSON envelope sent to the client
```

Exception handlers in `main.py` intercept `CalculatorError` and generic `Exception` at the framework level — no try/except needed in routes or services.

---

## Architecture Overview

### Request Flow

```
POST /api/v1/calculator/calculate?operation=divide
  Body: { "operand_a": 10, "operand_b": 0 }
          │
          ├─ FastAPI validates `operation` against Operation enum
          │    Unknown values → 422 Unprocessable Entity (automatic)
          │
          ├─ Pydantic validates CalculationRequest body
          │    NaN / Infinity / missing fields → 422 (automatic)
          │
          ▼
  calculator.calculate() route handler
          │
          ▼
  CalculatorService.calculate(Operation.DIVIDE, 10, 0)
          │
          └─ dispatch[Operation.DIVIDE] → _divide(10, 0)
                    │
                    └─ operand_b == 0 → raises DivisionByZeroError
                              │
                              ▼
                    calculator_error_handler() in main.py
                              │
                              ▼
                    400 Bad Request
                    { "success": false, "message": "Division by zero is not allowed", "data": null }
```

### Router Nesting

```
/api              ← api_router    (app/api/router.py)
  └── /v1         ← v1_router     (app/api/v1/router.py)
        └── /calculator            (app/api/v1/routes/calculator.py)
              └── POST /calculate?operation=<op>
```

To add a new resource (e.g. `/converter`), create `routes/converter.py` and register it in `v1/router.py` — nothing else changes.

---

## API Reference

### Base URL

```
http://localhost:8000/api/v1/calculator
```

---

### POST `/calculate`

The single endpoint for all arithmetic operations. The operation is selected via a required query parameter; the two operands are sent in the JSON body.

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

---

**200 OK**
```bash
curl -s -X POST "http://localhost:8000/api/v1/calculator/calculate?operation=add" \
  -H "Content-Type: application/json" \
  -d '{"operand_a": 12, "operand_b": 8}' | jq .
```
```json
{
  "success": true,
  "message": "Add performed successfully",
  "data": {
    "operand_a": 12.0,
    "operand_b": 8.0,
    "operation": "addition",
    "result": 20.0
  }
}
```

**400 Bad Request — division by zero**
```bash
curl -s -X POST "http://localhost:8000/api/v1/calculator/calculate?operation=divide" \
  -H "Content-Type: application/json" \
  -d '{"operand_a": 5, "operand_b": 0}' | jq .
```
```json
{
  "success": false,
  "message": "Division by zero is not allowed",
  "data": null
}
```

**422 Unprocessable Entity — unknown operation**
```bash
curl -s -X POST "http://localhost:8000/api/v1/calculator/calculate?operation=sqrt" \
  -H "Content-Type: application/json" \
  -d '{"operand_a": 9, "operand_b": 3}' | jq .
```
FastAPI automatically rejects values not in the `Operation` enum and returns a `422` that lists the accepted values.

---

### GET `/health`

```bash
curl -s http://localhost:8000/health | jq .
```
```json
{
  "status": "ok",
  "version": "1.0.0"
}
```

---

## Request & Response Format

### Request body

```json
{
  "operand_a": <number>,
  "operand_b": <number>
}
```

Both fields are required and must be finite numbers — `NaN` and `Infinity` are rejected by Pydantic before the service is called.

### Success response

```json
{
  "success": true,
  "message": "<operation> performed successfully",
  "data": {
    "operand_a": <number>,
    "operand_b": <number>,
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

Every response — success or error — uses the same `ApiResponse[T]` envelope. Clients only need to check `success`.

---

## Error Handling

All errors are caught centrally in `main.py` via `@app.exception_handler` decorators.

| Scenario | HTTP Status | Source |
|---|---|---|
| `operation=divide` or `operation=modulo` with `b == 0` | `400 Bad Request` | `DivisionByZeroError` raised in service |
| Any other calculator domain error | `400 Bad Request` | `CalculatorError` base class |
| Unknown `operation` value or missing / invalid body field | `422 Unprocessable Entity` | FastAPI + Pydantic (automatic) |
| Unhandled server-side exception | `500 Internal Server Error` | Generic `Exception` handler |

---

## Validation Rules

| Input | Where validated | Rule |
|---|---|---|
| `operation` query param | FastAPI (automatic) | Must be one of: `add`, `subtract`, `multiply`, `divide`, `modulo`, `power` |
| `operand_a` | Pydantic | Required, finite number (no NaN, no Infinity) |
| `operand_b` | Pydantic | Required, finite number (no NaN, no Infinity) |
| `operand_b` when `operation=divide` | `CalculatorService` | Must not be `0` — raises `DivisionByZeroError` → 400 |
| `operand_b` when `operation=modulo` | `CalculatorService` | Must not be `0` — raises `DivisionByZeroError` → 400 |

---

## Logging

Python's built-in `logging` module is used throughout. Configuration lives in `app/core/logging_config.py`.

**Log format:**
```
2026-02-26 10:42:01 [INFO] app.api.v1.routes.calculator - calculate request: 12.0 add 8.0
2026-02-26 10:42:01 [DEBUG] app.services.calculator_service - add: 12.0 + 8.0
```

| Logger | Default level | What is logged |
|---|---|---|
| `app.*` | `INFO` (set via `LOG_LEVEL`) | Route requests, warnings, errors |
| `app.services.*` | `DEBUG` | Per-operation arithmetic details |
| `uvicorn.access` | `WARNING` | Access logs suppressed to reduce noise |

Set `LOG_LEVEL=DEBUG` in `.env` to see the service-level debug lines.

---

## Prerequisites

| Requirement | Version |
|---|---|
| Python | 3.11 or later |
| pip | Latest |

---

## Getting Started

### 1. Navigate to the project folder

```bash
cd fastapi-calculator
```

### 2. Create and activate a virtual environment

```bash
python -m venv .venv
source .venv/bin/activate       # macOS / Linux
# .venv\Scripts\activate        # Windows
```

### 3. Install dependencies

```bash
pip install -r requirements.txt
```

### 4. Set up environment variables

```bash
cp .env.example .env
# Defaults work for local development — edit only if needed
```

### 5. Start the server

```bash
python main.py
```

Or with Uvicorn directly (enables auto-reload):

```bash
uvicorn main:app --reload --port 8000
```

The API is available at `http://localhost:8000`.

### 6. Try it out

All operations share the same URL — only `?operation=` changes.

```bash
BASE="http://localhost:8000/api/v1/calculator/calculate"

curl -s -X POST "$BASE?operation=add"      -H "Content-Type: application/json" -d '{"operand_a": 15, "operand_b": 7}'  | jq .
curl -s -X POST "$BASE?operation=subtract" -H "Content-Type: application/json" -d '{"operand_a": 15, "operand_b": 7}'  | jq .
curl -s -X POST "$BASE?operation=multiply" -H "Content-Type: application/json" -d '{"operand_a": 6,  "operand_b": 7}'  | jq .
curl -s -X POST "$BASE?operation=divide"   -H "Content-Type: application/json" -d '{"operand_a": 22, "operand_b": 7}'  | jq .
curl -s -X POST "$BASE?operation=modulo"   -H "Content-Type: application/json" -d '{"operand_a": 17, "operand_b": 5}'  | jq .
curl -s -X POST "$BASE?operation=power"    -H "Content-Type: application/json" -d '{"operand_a": 2,  "operand_b": 10}' | jq .

# Error cases
curl -s -X POST "$BASE?operation=divide"   -H "Content-Type: application/json" -d '{"operand_a": 5, "operand_b": 0}'  | jq .  # 400
curl -s -X POST "$BASE?operation=sqrt"     -H "Content-Type: application/json" -d '{"operand_a": 9, "operand_b": 3}'  | jq .  # 422
```

---

## Configuration Reference

Settings are defined in `app/core/config.py` and read from environment variables, with `.env` file support via `pydantic-settings`.

| Variable | Default | Description |
|---|---|---|
| `APP_NAME` | `Calculator API` | Title shown in the auto-generated API docs |
| `APP_VERSION` | `1.0.0` | Version shown in `/health` and API docs |
| `DEBUG` | `false` | Enables Uvicorn auto-reload when `true` |
| `HOST` | `0.0.0.0` | Interface the server binds to |
| `PORT` | `8000` | Port the server listens on |
| `LOG_LEVEL` | `INFO` | Logging verbosity (`DEBUG`, `INFO`, `WARNING`, `ERROR`) |

---

## Interactive Docs

FastAPI generates API documentation automatically from the code and type annotations.

| UI | URL |
|---|---|
| Swagger UI | http://localhost:8000/docs |
| ReDoc | http://localhost:8000/redoc |

The Swagger UI lets you pick the `operation` from a dropdown (auto-generated from the `Operation` enum) and execute requests directly from the browser.

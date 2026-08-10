# Transfer Platform

Backend API for tracking international money transfers (MoneyGram, Small
World) handled by Afriland First Bank agents, including monthly ceiling
("plafond") verification for Hors CEMAC transfers.

Spring Boot 3.5 / Java 17 / Spring Data JPA / Spring Security (JWT).

## Prerequisites

- Java 17
- No local Maven install needed — use the bundled wrapper (`./mvnw` /
  `mvnw.cmd`)
- PostgreSQL (only if you want to run against a real database instead of
  the default in-memory H2)

## Running

By default the app runs against an in-memory H2 database — no setup
required:

```bash
./mvnw spring-boot:run
```

The API is served at `http://localhost:8080`, and the H2 console is
available at `http://localhost:8080/h2-console` (JDBC URL
`jdbc:h2:mem:plafonds`, user `sa`, empty password).

To run against a real PostgreSQL database instead, activate the `prod`
profile (see `src/main/resources/application.yml` for the connection
details — update them to match your local Postgres instance):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

With `ddl-auto: update`, tables are created/updated automatically on
startup to match the JPA entities — no manual migrations needed.

## Seeded test data

On first startup (when no agents exist yet), `DataSeeder` creates one
test agent and one sample transfer:

| Field | Value |
|---|---|
| Telephone | `+237690000000` |
| Password | `password` |
| Role | `Instant Transfert` |

Use these credentials against `POST  http://localhost:8080` to get a JWT.

## Tests

```bash
./mvnw test
```

## API overview

All endpoints are under `/api`. Except for `/api/auth/**`,
`/api/referentiel`, and `/api/health`, endpoints require a
`Authorization: Bearer <token>` header obtained from login/register.

| Method | Path | Description |
|---|---|---|
| GET | `/api/health` | Health check (public) |
| GET | `/api/referentiel` | Reference data: piece types, countries, roles, channels (public) |
| POST | `/api/auth/register` | Create an agent account, returns a JWT |
| POST | `/api/auth/login` | Authenticate, returns a JWT |
| POST | `/api/transferts/verification` | Check monthly ceiling before executing a transfer |
| POST | `/api/transferts` | Execute a transfer |
| GET | `/api/transferts?q=` | Transfer history, optionally filtered |
| GET | `/api/transferts/annulables?q=` | Executed transfers eligible for cancellation |
| PATCH | `/api/transferts/{id}/annulation` | Cancel a transfer |
| GET | `/api/transferts/bilan` | Daily summary for the authenticated agent |

## Connecting a frontend

CORS is configured (`SecurityConfig`) to allow requests from
`http://localhost:4200`, matching the default Angular dev server port.
Point the frontend's API base URL at `http://localhost:8080/api`.

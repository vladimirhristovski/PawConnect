# PawConnect — Backend

Kotlin / Spring Boot API for PawConnect, a pet adoption & services platform
(adoption listings, applications, businesses: vet / groomer / pet-shop).
The Angular frontend lives in `../frontend`.

## Stack

|                |                                                                     |
|----------------|---------------------------------------------------------------------|
| Language       | Kotlin 2.3, Java 21 toolchain                                       |
| Framework      | Spring Boot 4.1 (`spring-boot-starter-webmvc`)                      |
| Persistence    | Spring Data JPA + Hibernate, PostgreSQL 18                          |
| Migrations     | Flyway (`src/main/resources/db/migration`)                          |
| Security       | Spring Security, JWT access tokens + hashed rotating refresh tokens |
| JSON           | Jackson 3 (`tools.jackson`) + `jackson-module-kotlin`               |
| Object storage | Azure Blob Storage (Azurite emulator locally)                       |
| Mail           | SMTP via `spring-boot-starter-mail` (Brevo relay)                   |
| Cache          | Caffeine (`@Cacheable` on reference-data lookups)                   |
| Tests          | JUnit 5, MockK, Testcontainers (PostgreSQL), RestAssured 6          |

## Prerequisites

- JDK 21
- Docker (for the Postgres + Azurite containers, and for the Testcontainers-based tests)

## Local setup

1. **Env file** — copy the template and fill in the blanks:

   ```bash
   cp .env.example .env
   ```

   `JWT_SECRET` must be at least 32 characters. `.env` is gitignored.
   Spring does **not** auto-load `.env` — in IntelliJ use the EnvFile plugin
   (or `export` the vars) so the run configuration picks them up.
   `docker-compose.yml` reads the same `.env`.

2. **Infrastructure** — start Postgres (`pawconnect-postgres`, port 5432) and
   Azurite (`pawconnect-azurite`, blob on port 10000):

   ```bash
   docker compose up -d
   ```

3. **Run the app:**

   ```bash
   ./gradlew bootRun
   ```

   Flyway applies `db/migration` (schema) and, outside production, the
   repeatable `db/migration-dev/R__dev_data.sql` seed.

## Tests

   ```bash
   ./gradlew test
   ```

Integration tests spin up a real PostgreSQL via Testcontainers, so Docker must
be running. Unit tests use MockK.

## Configuration reference

Non-secret defaults live in `application.yml`; secrets and host-specific values
come from the environment. Key variables (see `.env.example` for the full list):

| Variable                                                          | Purpose                                                           |
|-------------------------------------------------------------------|-------------------------------------------------------------------|
| `POSTGRES_*`                                                      | database name / user / password / port                            |
| `JWT_SECRET`                                                      | HMAC signing key for JWTs (≥ 32 chars)                            |
| `CORS_ALLOWED_ORIGINS`                                            | comma-separated allowed origins (default `http://localhost:4200`) |
| `FRONTEND_URL`                                                    | base URL used in password-reset links                             |
| `APP_MAIL_ENABLED`                                                | `false` locally — swaps `RealEmailService` for `NoOpEmailService` |
| `MAIL_FROM_ADDRESS`, `BREVO_SMTP_USERNAME`, `BREVO_SMTP_PASSWORD` | outbound SMTP                                                     |
| `AZURE_STORAGE_CONNECTION_STRING`, `AZURE_STORAGE_CONTAINER_NAME` | blob storage (Azurite connection string is in `.env.example`)     |

## Package layout

```
com.sorsix.pawconnect
├── api          REST controllers + request-parsing helpers (e.g. NearbySearch)
├── config       Spring @Configuration (security, Azure blob, HTTP clients)
├── domain       JPA entities, enums, status-code constants, entity extensions
│   └── base     BaseEntity → AuditableEntity → SoftDeletableEntity
├── dto
│   ├── request  inbound payloads
│   └── response outbound view models (`Response.from(entity)`)
├── exception    domain exceptions + GlobalExceptionHandler (RFC-9457 ProblemDetail)
├── repository   Spring Data repositories
├── security     JWT filter / service, CustomUserDetails
└── service      one service per aggregate; interface + impl for pluggable pieces
                 (email, blob storage, geocoding)
```

### Conventions

- Migrations under `V00x__` are **immutable** once applied — add a new file, never edit.
- Native-query repository methods map to DTOs **inside** the `@Transactional`
  service method (`open-in-view` is `false`).
- Partial updates: `request.field?.let { entity.field = it }` — never overwrite unconditionally.
- Errors are exception-based → `GlobalExceptionHandler` → `ProblemDetail`.

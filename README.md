# FinTogether Backend

FinTogether Backend is the Kotlin/Spring Boot REST API for the [FinTogether Flutter client](https://github.com/rafaelxvr/FinTogether-Frontend). It owns authentication, household budgeting rules, persistence, database migrations, analytics, synchronization endpoints, and operational safeguards.

## Capabilities

- JWT access and refresh tokens, current-user lookup, password reset, and BCrypt-backed credentials.
- Budgets, budget members, categories, accounts, transfers, and transaction history.
- Bulk transaction entry, search, installments, transaction splits, and settlement balances.
- Credit cards, invoices, card-linked accounts, and monthly card-cost analytics.
- Recurring transaction templates, fixed-expense summaries, and scheduled generation.
- Merchants, merchant search, top-merchant analytics, and archive/unarchive workflows.
- Savings goals, contributions, withdrawals, and contribution history.
- CSV/import profiles and import history.
- Dashboard summaries, forecasts, member spending, income sources, essential-spend breakdowns, and alerts.
- Liquibase migrations, request validation, centralized exception handling, OpenAPI documentation, and configurable rate limiting.

## Technology stack

- Kotlin 1.9.22 on Java 17
- Spring Boot 3.2.2
- Spring Web, Validation, Security, WebSocket, Mail, and Data JPA
- PostgreSQL 15+ for development and production
- H2 for the automated test profile
- Liquibase for schema versioning
- JJWT for access and refresh token handling
- Bucket4j for endpoint rate limiting
- SpringDoc OpenAPI/Swagger UI
- Gradle 8.5 through the checked-in wrapper

## Requirements

- JDK 17 or newer
- PostgreSQL 15 or Docker
- Bash, PowerShell, or a compatible shell
- Optional: Docker Desktop for the complete local stack

Check the Java and wrapper versions:

```bash
java -version
./gradlew --version
```

On Windows, use `gradlew.bat` in place of `./gradlew`.

## Quick start with Docker Compose

The included `docker-compose.yml` starts PostgreSQL and the API together:

```bash
docker compose up --build
```

The services are available at:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- PostgreSQL: `localhost:5432`, database `fintogether_dev`, user `postgres`, password `postgres`

Stop the stack with:

```bash
docker compose down
```

Add `-v` only when you intentionally want to remove the local PostgreSQL volume and its data.

## Local development

Create a PostgreSQL database:

```sql
CREATE DATABASE fintogether_dev;
```

The development profile expects the following defaults:

```text
DATABASE: fintogether_dev
HOST: localhost
PORT: 5432
USERNAME: postgres
PASSWORD: postgres
```

Update `src/main/resources/application-dev.yml` for a different local setup, or provide the production profile through environment variables. Run the application with:

```bash
./gradlew bootRun
```

The default active profile is `dev`. Select another profile explicitly:

```bash
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

PowerShell equivalent:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
./gradlew bootRun
```

## Configuration

`src/main/resources/application.yml` defines safe defaults and profile selection. Production configuration is environment driven:

| Variable | Purpose | Default |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` or `ACTIVE_PROFILE` | Spring profile selection | `dev` |
| `DATABASE_URL` | JDBC PostgreSQL URL | required by `prod` |
| `DATABASE_USERNAME` | Database user | required by `prod` |
| `DATABASE_PASSWORD` | Database password | required by `prod` |
| `JWT_SECRET` | HMAC signing secret | required by `prod` |
| `JWT_EXPIRATION` | Access-token lifetime in milliseconds | 86400000 |
| `JWT_REFRESH_EXPIRATION` | Refresh-token lifetime in milliseconds | 604800000 |
| `PORT` or `SERVER_PORT` | HTTP port | 8080 |
| `SMTP_HOST` | SMTP host | smtp.gmail.com |
| `SMTP_PORT` | SMTP port | 587 |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | SMTP credentials | empty |
| `EMAIL_ENABLED` | Enable password-reset email delivery | false |
| `EMAIL_FROM` | Sender address | noreply@fintogether.com |
| `APP_BASE_URL` | Frontend URL used in email links | http://localhost:3000 |
| `RATE_LIMIT_ENABLED` | Enable request throttling | true |

Never commit production credentials, JWT secrets, SMTP passwords, or signing material. The checked-in development and test values are local-only examples.

## API surface

All business endpoints are under `/api`. The principal controller groups are:

| Area | Base paths |
| --- | --- |
| Authentication | `/api/auth` |
| Users | `/api/users` |
| Budgets | `/api/budgets` |
| Dashboard | `/api/dashboard` |
| Accounts and transfers | `/api/budgets/{budgetId}/accounts` |
| Transactions | `/api/budgets/{budgetId}/transactions` |
| Categories | `/api/budgets/{budgetId}/categories` |
| Merchants | `/api/budgets/{budgetId}/merchants` |
| Recurring templates | `/api/budgets/{budgetId}/recurring-templates` |
| Savings goals | `/api/budgets/{budgetId}/savings-goals` |
| Credit cards and invoices | `/api/credit-cards` |
| Import workflows | `/api/budgets/{budgetId}/imports` |
| Splits and installments | `/api/transactions`, `/api/installment-groups` |

Authentication starts with `POST /api/auth/register` or `POST /api/auth/login`. Send the returned access token as `Authorization: Bearer <token>` for protected endpoints. Use Swagger UI for the current request and response schemas rather than copying stale examples into client code.

## Architecture

```text
src/main/kotlin/com/fintogether/
├── domain/           # Entities, value objects, exceptions, repository contracts
├── application/      # DTOs, mappers, and use-case/application services
├── infrastructure/   # JPA adapters, JWT security, scheduling, mail, configuration
└── presentation/     # REST controllers, API responses, and exception handling

src/main/resources/
├── application*.yml  # Shared and environment-specific configuration
└── db/changelog/     # Liquibase master file and numbered schema changes
```

The database schema is managed by Liquibase. Add a new numbered change set under `src/main/resources/db/changelog/changes/` and register it in the master changelog; do not edit an already-applied migration in a shared environment.

## Tests and build

Tests use the `test` Spring profile and an in-memory H2 database:

```bash
./gradlew test
```

Useful local checks:

```bash
./gradlew clean build
./gradlew test --tests "com.fintogether.application.service.AuthServiceTest"
./gradlew bootRun
```

Build the container image directly:

```bash
docker build -t fintogether-backend .
docker run --rm -p 8080:8080 fintogether-backend
```

The container image uses a Gradle build stage and a Java 17 runtime image. Supply database and JWT settings through the container environment.

## Security notes

- Use a unique, high-entropy `JWT_SECRET` outside development and tests.
- Keep database and SMTP credentials in deployment secrets.
- Keep rate limiting enabled for public authentication and password-reset endpoints.
- Use HTTPS in deployed environments and configure the frontend origin accordingly.
- Review actuator, logging, CORS, and email settings before exposing the service publicly.

Planning and specification documents are intentionally excluded from this public repository. This repository contains the API source, migrations, tests, Docker setup, and operational documentation needed to run the backend.

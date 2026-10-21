# Backend

Spring Boot REST API for Paseadores. See `docs/arquitectura.md` and `docs/modulos.md` for the domain design.

## Requirements

- Java 21
- Maven (or use the included wrapper: `./mvnw`)
- PostgreSQL 14+ (local or Docker)

## Local database (Docker)

```bash
docker run -d --name paseadores-db \
  -e POSTGRES_USER=paseadores \
  -e POSTGRES_PASSWORD=paseadores \
  -e POSTGRES_DB=paseadores \
  -p 5433:5432 \
  postgres:15-alpine
```

Port `5433` is used instead of the default `5432` to avoid clashing with other local Postgres instances.

## Environment variables

| Variable | Example (local Docker) |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/paseadores` |
| `DB_USER` | `paseadores` |
| `DB_PASSWORD` | `paseadores` |
| `JWT_SECRET` | any local dev string |

## Running

```bash
./mvnw spring-boot:run
```

Flyway runs automatically on startup — it applies `src/main/resources/db/migration/` (schema + zones catalog) against whatever database `DB_URL` points to. No manual migration step needed.

## Authentication

All endpoints require `Authorization: Bearer <token>` except `/api/auth/**` (not implemented yet — see the `auth` module). No token or an invalid one returns 403.

## Tests

```bash
./mvnw test
```

Database integration tests (`*DatabaseIntegrationTest`) are skipped unless `TPI_DB_TESTS=true` is set and the database is running.

## Architecture rules

Code is organized by feature (`auth`, `user`, `zone`, `pet`, `walk`, `review`), with `shared` for cross-cutting code. `ArchitectureTest` enforces these rules on every `./mvnw test` and in CI, so breaking one fails the build:

- A feature uses only its own repositories. To read or write another feature's data, call that feature's service (e.g. `AuthService` calls `ZoneService.existsById`, not `ZoneRepository`).
- Controllers never use repositories or JPA entities: they call services and return DTOs.
- Services know nothing about HTTP (`ResponseEntity`, `HttpServletRequest`, ...).
- `shared` never depends on a feature.
- No dependency cycles between features.

To check only these rules: `./mvnw test -Dtest=ArchitectureTest`.

## Notes

- Schema source of truth: `database/` at the repo root. Migrations here mirror it.
- `ddl-auto` is `none` — Hibernate never generates or alters the schema, only Flyway does.

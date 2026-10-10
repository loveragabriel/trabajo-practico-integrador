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

## Notes

- Schema source of truth: `database/` at the repo root. Migrations here mirror it.
- `ddl-auto` is `none` — Hibernate never generates or alters the schema, only Flyway does.

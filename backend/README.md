# Backend

The Paseadores API uses Java 21, Spring Boot and PostgreSQL 14 or later. The repository includes the Maven wrapper. See the [local setup guide](../docs/ejecucion-local.md) for Windows, Linux and macOS commands.

## Database and Configuration

Use an existing development database or start a local one with Docker:

```bash
docker run -d --name paseadores-db \
  -e POSTGRES_USER=paseadores \
  -e POSTGRES_PASSWORD=paseadores \
  -e POSTGRES_DB=paseadores \
  -p 127.0.0.1:5433:5432 \
  postgres:15-alpine
```

The example maps PostgreSQL to port 5433 on your machine so it can run alongside an instance on 5432. Set these values in the backend terminal or in an untracked `backend/.env` file:

| Variable | Example for the local container |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/paseadores` |
| `DB_USER` | `paseadores` |
| `DB_PASSWORD` | `paseadores` |
| `JWT_SECRET` | A local signing secret |

From `backend/`, run `bash mvnw spring-boot:run`. Flyway applies the [migrations](src/main/resources/db/migration/) when the app starts. Hibernate does not create or change the schema.

## API Access

Registration, login and neighborhood catalog reads are available without a token. Protected API routes require `Authorization: Bearer <token>`. A missing or invalid token returns `401`; a signed-in user without permission receives `403`. See [Swagger UI](http://localhost:8080/swagger-ui/index.html) while the API is running.

## Tests and Architecture

All endpoints require `Authorization: Bearer <token>` except `/api/auth/**` (register and login — see the `auth` module). No token or an invalid one returns 403.

`ArchitectureTest` checks that controllers call services, each feature uses only its own repositories, and feature dependencies have no cycles. It also keeps HTTP concerns out of services and feature dependencies out of `shared`. See the [architecture notes](../docs/arquitectura.md) and [module guide](../docs/modulos.md).

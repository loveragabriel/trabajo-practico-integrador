# Local Setup

We run Spring Boot on port 8080 and Vite on port 5173. Start PostgreSQL before starting the backend.

## Requirements

Use JDK 21, PostgreSQL 14 or later, Node.js 24 and pnpm 11.25.0. The repository includes the Maven wrapper.

```bash
java -version
node -v
pnpm -v
```

If pnpm is not installed, run `npm install -g pnpm@11.25.0`.

## Database

If you already have a Paseadores database, keep its name and data. For a fresh installation, create an empty database once:

```bash
createdb -h localhost -p 5432 -U postgres paseadores
```

If `createdb` is not on your PATH, connect to the `postgres` database in pgAdmin and run `CREATE DATABASE paseadores;`. Flyway creates the tables and loads the neighborhoods when Spring Boot starts. Do not run the standalone schema scripts against the same database.

## Terminal 1: Backend

From the repository root in Windows Git Bash, adjust the database name, port and user for your installation. If your `backend/.env` already contains all four values, you can skip the exports.

```bash
export DB_URL='jdbc:postgresql://localhost:5432/paseadores'
export DB_USER='postgres'
read -r -s -p 'PostgreSQL password: ' DB_PASSWORD
echo
export DB_PASSWORD
export JWT_SECRET='paseadores-local-development-key-2026'
cd backend
MSYS_NO_PATHCONV=1 cmd.exe /c "mvnw.cmd spring-boot:run"
```

On Linux or macOS, use `bash mvnw spring-boot:run` instead of the last command. If you use `backend/.env`, start the backend from that directory and keep the file out of Git. The example JWT secret is for local use only.

Keep the terminal open. The API is at [localhost:8080](http://localhost:8080), and the API contract is in [Swagger UI](http://localhost:8080/swagger-ui/index.html).

## Terminal 2: Frontend

Open another terminal at the repository root:

```bash
cd frontend
pnpm install --frozen-lockfile
pnpm dev
```

Open [localhost:5173](http://localhost:5173). If a different project is using port 5173, stop it with `Ctrl+C` in its own terminal or start Paseadores with `pnpm dev --port 5174`. The Vite proxy still uses port 8080 for the API.

Check the API and the frontend proxy with:

```bash
curl -f http://localhost:8080/api/zones
curl -f http://localhost:5173/api/zones
```

Both should return the same neighborhood catalog. If you chose port 5174, use that port for the second command. Walk search, the schedule and bookings require a signed-in account; protected API routes return `401` without a valid token.

## Demo Walkthrough

1. Sign in as a walker, set a service neighborhood in **My profile** (`Mi perfil`), and publish a future walk with one spot.
2. In a separate browser profile, sign up as an owner, add a pet and find the walk by neighborhood and date. Open the details and book the spot.
3. In **My bookings** (`Mis reservas`), cancel from **Upcoming** (`Próximas`) while more than two hours remain. Confirm it appears in **History** (`Historial`), then book again.
4. Start the walk as the walker. Confirm the owner sees it under **In progress** (`En curso`). Complete it as the walker, then open **History** as the owner and choose **Review the walk** (`Calificar el paseo`).

The map needs access to OpenStreetMap tiles. It shows an approximate neighborhood location.

## Tests

From `frontend`:

```bash
pnpm build
pnpm lint
node --import ./tests/register-hooks.mjs --test tests/*.test.ts
```

From `backend`, run `bash mvnw test` on Linux or macOS, or `MSYS_NO_PATHCONV=1 cmd.exe /c "mvnw.cmd test"` in Windows Git Bash. To include the database integration tests, point the database variables at a separate test database and set `TPI_DB_TESTS=true` for that command. Some tests write records, so use a database you do not share with the app.

To stop either server, press `Ctrl+C` in its terminal.

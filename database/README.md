# Database

Paseadores uses PostgreSQL 14 or later. When you run the application, Flyway applies the [application migrations](../backend/src/main/resources/db/migration/). Start with an empty database and follow the [local setup guide](../docs/ejecucion-local.md).

## Standalone SQL and Schema

| File | Purpose |
|---|---|
| [01_schema.sql](01_schema.sql) | Creates types, tables, constraints and indexes |
| [02_seed.sql](02_seed.sql) | Adds the initial Buenos Aires neighborhood catalog |
| [03_verify.sql](03_verify.sql) | Runs manual integrity checks |
| [schema.dbml](schema.dbml) | Editable model for diagramming |

To inspect the standalone schema without the app, use a separate empty database and run these commands from `database/`:

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f 01_schema.sql
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f 02_seed.sql
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f 03_verify.sql
```

For these commands, `DATABASE_URL` is a PostgreSQL connection string for `psql`. The backend instead uses a JDBC URL in `DB_URL` and separate `DB_USER` and `DB_PASSWORD` values. Do not apply the standalone schema to a database managed by Flyway.

The [data model guide](../docs/modelo-datos.md) explains the tables and how bookings and reviews keep their history.

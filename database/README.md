# Database

PostgreSQL schema, seed data and verification queries for the Paseadores project.

## Requirements

- PostgreSQL 14+

## Files

| File | Purpose |
|---|---|
| `01_schema.sql` | DDL — enums, tables, constraints, indexes |
| `02_seed.sql` | Initial zones catalog (CABA neighborhoods) |
| `03_verify.sql` | Manual integrity checks — all queries should return 0 rows |

## Usage

Run in order against an empty database:

```bash
psql "$DATABASE_URL" -f 01_schema.sql
psql "$DATABASE_URL" -f 02_seed.sql
psql "$DATABASE_URL" -f 03_verify.sql
```

`DATABASE_URL` follows the standard libpq format (libpq: PostgreSQL's own client library, used by `psql` and JDBC drivers):

```
postgresql://<user>:<password>@<host>:<port>/<database>
```

This maps to the backend's own connection variables (see root `README.md`): `DB_USER`, `DB_PASSWORD`, `DB_URL`.

## Notes

- Flyway is an optional future integration (see `docs/arquitectura.md`, decision 7) — not confirmed as installed. Until/unless it's adopted, the backend connects to a database already initialized by running these scripts directly.
- No cascading deletes: history (walks, bookings, reviews) is preserved even when a related row becomes inactive.

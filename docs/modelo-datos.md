# Relational Data Model

We use PostgreSQL. The application schema is defined in the [Flyway migrations](../backend/src/main/resources/db/migration/). The [standalone schema](../database/01_schema.sql), [editable DBML](../database/schema.dbml) and [diagram](er-diagram.png) are useful for reviewing the design.

## Tables and Relationships

| Table | Key | What it stores |
|---|---|---|
| `zones` | `id` (UUID) | A predefined neighborhood and its city. |
| `users` | `id` (UUID) | Identity, email, password hash, contact details, role, home neighborhood and active status. Walkers can also have a default price and capacity, plus rating totals. |
| `walker_zones` | `(walker_id, zone_id)` | Service neighborhoods for a walker. A walker can cover several neighborhoods; a neighborhood can have several walkers. |
| `pets` | `id` (UUID) | A pet owned by one user, with its name and optional breed, size and notes. An owner may have several pets. |
| `walks` | `id` (UUID) | A walk published by one walker in one neighborhood. Stores time, duration, capacity, price, meeting point and status. |
| `walk_pets` | `id` (UUID) | A pet's booking on one walk, including status, booking time and notes. A walk can have several bookings, and a pet can join different walks. |
| `reviews` | `id` (UUID) | An owner's rating of a walk, with an optional comment and a date. |

## Roles and States

| Type | Values |
|---|---|
| `user_role` | `CLIENT`, `WALKER` |
| `pet_size` | `SMALL`, `MEDIUM`, `LARGE` (optional) |
| `walk_status` | `PUBLISHED`, `FULL`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `booking_status` | `JOINED`, `CANCELLED` |

Capacity changes move a scheduled walk between `PUBLISHED` and `FULL`. The walker can start it with at least one active booking, then complete it from `IN_PROGRESS`. A cancelled walk also cancels its active bookings. Completed and cancelled walks are final states. PostgreSQL enums restrict values; the service checks transitions.

## Database Constraints and Service Rules

- Foreign keys require referenced records to exist. The service checks roles and ownership separately.
- Email is unique without regard to case. Neighborhood and city, walker and neighborhood, walk and pet, and walk and review author are also unique pairs.
- Duration and capacity must be positive. Prices must be non-negative, and review ratings must be integers from one to five.
- Booking and cancellation lock the walk row and recheck capacity. Rebooking updates the existing row. Each booking request covers one pet.
- The service checks coverage and overlapping walks before publishing. It only allows owners to review a completed walk they took part in.
- There are no cascading deletes. Cancellation keeps the history.

We use `NUMERIC` for prices in Argentine pesos and store dates and times as local timestamps for Buenos Aires. The database does not contain map polygons or meeting point coordinates.

# Architecture and Design Decisions

React and TypeScript provide the screens. Vite builds the app and proxies requests to the Spring Boot REST API during development. We use one backend application with a layered structure and one PostgreSQL database.

| Layer | Responsibility |
|---|---|
| React | Displays pages, forms, schedules and maps; reads the API responses |
| Controllers | Read HTTP requests, validate input and return responses |
| Services | Apply rules, check access and coordinate transactions |
| Repositories | Read and write data through Spring Data JPA or JDBC |
| PostgreSQL | Stores records and enforces keys, uniqueness and check constraints |
| Shared code | Holds configuration, security and common error handling |

The backend is organized by domain. The frontend uses `features/` for pages, components, API calls and hooks, `app/` for routing and layout, and `shared/` for code that does not depend on a feature.

## Design Decisions

1. Owners and walkers share one `users` table. A fixed role distinguishes `CLIENT` and `WALKER`; the walker defaults apply only to walkers.
2. Walkers publish walks and owners book directly. Published walks supply the availability, so we do not keep a separate availability calendar.
3. `walk_pets` is a booking entity with a status and timestamp. Rebooking a cancelled spot reuses the same row.
4. We use a predefined neighborhood catalog. Leaflet displays approximate positions from the frontend; the database does not store map polygons.
5. Each walk keeps its own price and capacity. Changing the walker profile does not change a published walk.
6. We keep history when a booking or walk is cancelled. The schema does not cascade deletes.
7. Flyway applies the application migrations at startup. The standalone SQL and editable model are available in [database/](../database/README.md).
8. An owner can review a completed walk once, even after booking for several pets. We update the walker's rating when we save the review.

## Access and Consistency

Spring Security checks JWTs. The backend uses the signed-in account to check role and ownership, and stores password hashes using BCrypt. Walker profiles shown to other signed-in users omit email and phone numbers.

When a walker publishes, a transaction-scoped PostgreSQL advisory lock keeps two publications for the same walker from checking the schedule at the same time. When an owner books or cancels, the backend locks the walk row before checking or updating availability. A walk's date and time represent local time in Buenos Aires.

See the [module guide](modulos.md) and [data model](modelo-datos.md) for details.

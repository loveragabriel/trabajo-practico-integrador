# Frontend

The Paseadores web app uses React, TypeScript, Vite, Tailwind CSS and Leaflet. We use Node.js 24 and pnpm 11.25.0 for development.

## Run and Test

From `frontend/`:

```bash
pnpm install --frozen-lockfile
pnpm dev
```

Vite serves the app on port 5173 and forwards `/api` requests to Spring Boot on port 8080. Start the backend first as described in the [local setup guide](../docs/ejecucion-local.md).

| Command | Purpose |
|---|---|
| `pnpm dev` | Start the development server |
| `pnpm build` | Type-check and build into `dist/` |
| `pnpm preview` | Preview the production build |
| `pnpm lint` | Run Oxlint |
| `node --import ./tests/register-hooks.mjs --test tests/*.test.ts` | Run the frontend test suite |

## Source Layout

- `src/app/` wires together the router, layout, providers and top-level pages.
- `src/features/` groups the pages, components, hooks and API calls for authentication, profiles, pets, walks, bookings, reviews and the map.
- `src/shared/` contains reusable components, types and the HTTP client. The client adds the JWT to authenticated requests.

Use `src/app/paths.ts` for route paths and the `@/` alias for imports from `src/`. Tailwind v4 is imported and configured in `src/index.css`.

## Routes

| Access | Main routes |
|---|---|
| Visitors | `/`, `/login`, `/register` |
| Signed-in users | `/profile`, `/walks`, `/walks/:walkId`, `/walkers/:walkerId` |
| Pet owners | `/pets`, `/pets/new`, `/pets/:petId/edit`, `/bookings` |
| Walkers | `/my-walks`, `/my-walks/new`, `/my-walks/:walkId/published` |

Route guards help users find the right screens. The backend still checks role and ownership before returning protected data or accepting changes.

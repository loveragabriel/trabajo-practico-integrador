# Frontend

Web app for Paseadores — TypeScript + React, built with Vite and styled with Tailwind CSS v4. Leaflet + OpenStreetMap will be used for the zone map. See `docs/modulos.md` for what each module does.

## Requirements

- Node.js 20.19+ or 22.12+
- pnpm (`npm install -g pnpm`, or `corepack enable`)

## Running

```bash
pnpm install
pnpm dev        # http://localhost:5173
```

During development, requests to `/api/*` are proxied to the backend at `http://localhost:8080` (see `vite.config.ts`), so the app calls relative URLs like `/api/walks` without CORS setup.

## Scripts

| Command | What it does |
|---|---|
| `pnpm dev` | Dev server with hot reload |
| `pnpm build` | Type-check (`tsc -b`) and production build into `dist/` |
| `pnpm preview` | Serve the production build locally |
| `pnpm lint` | Lint with Oxlint |

## Styling

Tailwind CSS v4 is wired through the `@tailwindcss/vite` plugin and imported in `src/index.css` (`@import 'tailwindcss';`). There is no `tailwind.config.js`; theme customizations go in `src/index.css` with `@theme`.

## Structure

Feature-based: each module owns everything it needs, mirroring the backend packages. `src/main.tsx` renders `src/app/App.tsx`.

```
src/
├── app/                      # wires the app together
│   ├── App.tsx               # providers + router
│   ├── router.tsx            # route tree and access guards
│   ├── paths.ts              # every URL of the app
│   ├── AppLayout.tsx         # header + role-based navigation
│   └── pages/                # pages that belong to no module: Home, NotFound
├── features/
│   ├── auth/
│   │   ├── index.ts          # public API: AuthProvider, useAuth, RequireAuth, GuestOnly
│   │   ├── pages/            # Login, Register
│   │   ├── guards/           # RequireAuth, GuestOnly (route access)
│   │   ├── hooks/            # useAuth
│   │   └── context/          # AuthContext (types), AuthProvider (session state)
│   ├── walks/pages/          # WalkSearch, WalkDetail, MyWalks, PublishWalk
│   ├── bookings/pages/       # MyBookings (client's schedule and history)
│   ├── pets/pages/           # Pets, PetForm
│   ├── profile/pages/        # Profile, WalkerProfile
│   ├── reviews/              # (to do) rating form and review list
│   └── map/                  # (to do) zone catalog and coverage map (Leaflet)
└── shared/                   # business-agnostic code
    ├── api/client.ts         # HTTP client (attaches the JWT)
    ├── components/           # generic UI: PageHeader...
    └── types/
```

As a module grows, its folder gains `components/`, `hooks/`, `api.ts` and `types.ts` next to `pages/`. Create folders when there is code to put in them. Hooks that belong to a module go in its `hooks/`; only business-agnostic hooks (e.g. `useDebounce`) go in `shared/hooks/`.

### Where does a file go?

1. Belongs to one module → `features/<module>/`.
2. Used by several modules and knows nothing about the business → `shared/`.
3. Wires the app (routes, providers, layout) → `app/`.

### Dependency rules

- A feature may import from `shared/`.
- Code outside a feature imports it **only through its `index.ts`** (its public API): `import { useAuth } from '@/features/auth'`, never `'@/features/auth/hooks/useAuth'`. A feature gets an `index.ts` once something outside it needs it.
- Inside a feature, files import each other by their direct path.
- `app/router.tsx` is the only place that imports pages directly, to build the routes.

### Routes

All URLs live in `src/app/paths.ts`. For routes with params use `generatePath(paths.walkDetail, { walkId })`.

| Access | Routes |
|---|---|
| Public | `/`, `/walks`, `/walks/:walkId`, `/walkers/:walkerId` |
| Guests only | `/login`, `/register` |
| Any signed-in user | `/profile` |
| `CLIENT` | `/pets`, `/pets/new`, `/pets/:petId/edit`, `/bookings` |
| `WALKER` | `/my-walks`, `/my-walks/new` |

### Imports

Use the `@/` alias (maps to `src/`, configured in `vite.config.ts` and `tsconfig.app.json`): `import { PageHeader } from '@/shared/components/PageHeader'`. Relative imports only within the same folder.

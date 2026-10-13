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

Current entry point: `src/main.tsx` renders `src/App.tsx`. As modules are implemented, code is organized as follows:

```
src/
├── app/                 # App entry, router, providers
├── pages/               # Route-level screens
├── features/            # One folder per functional module
│   ├── auth/            # Login and registration
│   ├── profile/         # Own profile, walker coverage zones and defaults
│   ├── map/             # Zone catalog and coverage map (Leaflet)
│   ├── pets/            # Owner's pets
│   ├── walks/           # Publish walks, search by zone, join/cancel bookings
│   └── reviews/         # Rate completed walks
└── shared/
    ├── api/             # HTTP client (attaches the JWT)
    ├── components/      # Reusable UI components
    ├── hooks/
    ├── types/
    └── utils/
```

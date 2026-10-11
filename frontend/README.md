# Frontend

Web app for Paseadores — TypeScript + React (Vite), Leaflet + OpenStreetMap for the zone map. See `docs/modulos.md` for what each module does.

> ⚠️ The Vite project is not scaffolded yet. Only the folder structure is in place.

## Structure

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

## Running

```bash
npm install
npm run dev     # http://localhost:5173
```

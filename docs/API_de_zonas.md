# Neighborhood Catalog and API Errors

Registration, profiles, coverage and search share the same neighborhood catalog. The frontend uses the IDs returned by the backend.

| Endpoint | Response |
|---|---|
| `GET /api/zones` | Returns `{id, name, city}` entries sorted by name, or an empty list. |
| `GET /api/zones/{id}` | Returns a neighborhood. An unknown UUID returns `404`; an invalid UUID format returns `400`. |

Both endpoints are public and the catalog is read-only. The API does not return coordinates or polygons; the map uses approximate positions from the frontend.

## API Errors

The API returns a common `ApiError` response without stack traces or credentials. `RequestFormatExceptionHandler` handles malformed requests and unsupported HTTP methods.

| Situation | Status |
|---|---|
| Invalid parameter format, missing required parameter or invalid JSON | `400` |
| Missing or invalid session on a protected route | `401` |
| Signed-in user lacks permission | `403` |
| Unsupported method on an existing route | `405`, with an `Allow` header |
| Unsupported content type | `415` |

## Frontend

The `features/map` module includes the catalog request, `useZones`, reusable selectors and `ZoneCoverageMap`. The catalog hook tracks loading and errors, supports retries and ignores stale responses.

See [walk search](avance-busqueda.md) and the [local setup guide](ejecucion-local.md).

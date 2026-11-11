# Publishing a Walk

The publishing screen is at `/my-walks/new`. It loads service neighborhoods and default values from `GET /api/walkers/me`. The walker's home neighborhood is not automatically part of their coverage. If no service neighborhoods are configured, the screen explains what is missing.

## Request

`POST /api/walks` requires a JWT for an active walker. The server identifies the walker from the session.

```json
{
  "zoneId": "replace-with-the-service-neighborhood-uuid",
  "startTime": "2030-01-01T11:00",
  "durationMinutes": 45,
  "capacity": 3,
  "pricePerPet": 2500.50,
  "meetingPoint": "Main entrance to the park"
}
```

Times are local to Buenos Aires. The walk must start at least two hours later and last at least 30 minutes, in 15-minute increments. Capacity must be a positive integer, price must be non-negative with up to two decimal places, and a meeting point is required.

On success, the API returns `201 Created`, a walk response with its ID and a `Location: /api/walks/{id}` header. The walk starts as `PUBLISHED`. Invalid fields return `400`, missing authentication returns `401`, a role without permission returns `403`, and an overlapping walk returns `409`.

For the same walker, scheduled walks and walks in progress block overlapping times. One walk may start at the exact time the previous one ends. A transaction-scoped advisory lock makes the overlap check and insert run one at a time for each walker.

## Try It

1. Follow [local setup](ejecucion-local.md), then sign in as a walker and configure at least one service neighborhood under **My profile** (`Mi perfil`).
2. Open **Publish walk** (`Publicar paseo`) and enter a covered neighborhood, future start time, duration, capacity, price and meeting point.
3. Check the confirmation and **My walks** (`Mis paseos`). The walk should also appear in an owner's search if the filters match.
4. Try an overlapping time with the same walker. The API should reject it. The PostgreSQL integration tests also cover two concurrent publication attempts.

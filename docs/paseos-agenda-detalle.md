# Walk Schedule and Details

In **My walks** (`Mis paseos`), a walker can switch between upcoming walks and history. An owner can use **My bookings** (`Mis reservas`) to see upcoming, in-progress and past bookings. Both views identify the account from the JWT.

## API

| Request | Access and result |
|---|---|
| `GET /api/walks` | A signed-in account can search for walks with available spots. |
| `GET /api/walks/mine` | An active walker can read their own schedule. |
| `GET /api/walks/{id}` | Returns walk details if the signed-in account can access them. |
| `GET /api/walks/{id}/pets` | The walker who owns the walk can see the pets booked on it. |
| `PATCH /api/walks/{id}/status` | The walker who owns the walk can start, complete or cancel it, subject to the current state. |
| `GET /api/walks/bookings/mine` | A pet owner can list their bookings under `UPCOMING`, `IN_PROGRESS` or `HISTORY`. |

The walker schedule accepts `period=UPCOMING` or `HISTORY`, a zero-based `page` and `size` from 1 to 50. Defaults are `UPCOMING`, `0` and `12`. The response includes `items`, `page`, `size` and `hasNext`. Upcoming walks are scheduled or in progress and have not reached the scheduled end. History contains completed, cancelled and ended intervals. Viewing the history does not change a walk's status.

Signed-in users can view future `PUBLISHED` or `FULL` walks. Other walk details are available to the walker who published them and owners with a `JOINED` booking for their own pet. An inaccessible or missing walk returns `404`. Details include time, meeting point, price, capacity, state and walker summary.

## Booking and Review Flow

Owners can book from the details screen, cancel eligible bookings and rebook when there is room. Once the walker starts a walk, the booking appears under **In progress**. After completion, a participating owner can select **Review the walk** from **History**. Walkers see their active bookings on the walk details screen.

See [publishing a walk](publicacion-paseos.md) and [local setup and tests](ejecucion-local.md).

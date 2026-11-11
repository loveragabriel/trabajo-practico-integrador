# Modules and Responsibilities

The backend packages are under `com.tpi.paseadores`. The frontend groups pages, components, API calls and hooks under `frontend/src/features/`. Coverage uses the `user` package; bookings and search share the `walk` package.

| Module | Backend | Frontend | What we check |
|---|---|---|---|
| Authentication | `auth` | `features/auth` | Owners and walkers can register with an existing neighborhood and sign in. Invalid credentials and inactive accounts are rejected. Password hashes are not returned. |
| Users and profiles | `user` | `features/profile` | A user can edit their own contact details and home neighborhood. They cannot change roles from the profile or edit another account. |
| Walker coverage | `user` | `features/profile`, `features/map` | A walker can set service neighborhoods, a reference price and default capacity. The walker profile shown to other signed-in users omits private contact details. |
| Pets | `pet` | `features/pets` | An owner can add, view and edit their own pets. Booking for another owner's pet is rejected. |
| Neighborhoods and map | `zone` | `features/map` | Registration, coverage and search use the same read-only catalog. The map provides an approximate reference for each neighborhood. |
| Walks and schedule | `walk` | `features/walks` | Walkers can publish in their service areas, see their schedule and manage walk states. An overlapping time or an attempt to manage someone else's walk is rejected. |
| Bookings | `walk` | `features/walks`, `features/bookings` | Each request reserves one spot for one owned pet. Two concurrent requests cannot both take the last spot. Cancelling more than two hours before a scheduled walk releases capacity. |
| Search | `walk` | `features/walks` | The search returns future published walks with capacity. It excludes past, full and cancelled walks. |
| Reviews | `review` | `features/reviews`, `features/profile` | A participating owner can review a completed walk once. Saving the review updates the walker's average and review count. |
| Shared code | `shared` | `app`, `shared` | The client attaches the JWT where needed, and API errors follow a common format. |

## How the Modules Work Together

A walker sets up coverage before publishing a walk. An owner needs a pet before booking. Search shows available spots, and bookings check capacity again inside the transaction. A completed walk then becomes eligible for a review from an owner who took part.

The MVP does not let users transfer pets, edit a published walk's schedule, or edit or remove a review. The [local setup guide](ejecucion-local.md) shows how to try the full sequence.

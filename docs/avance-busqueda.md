# Walk Search and Map

Once signed in, owners can find published future walks by neighborhood, date or time. Search only includes walks with available spots and an active walker. Only `JOINED` bookings count toward capacity.

A result shows the neighborhood, start time, duration, meeting point, price per pet, remaining spots and walker rating. It does not show the walker's private contact details. The frontend keeps filters and pagination in the URL so the same results can be reopened after a refresh or a visit to the walk details.

## API

`GET /api/walks` requires a signed-in account.

| Parameter | Meaning |
|---|---|
| `zoneId` | Neighborhood UUID. If omitted, the API uses the account's home neighborhood. |
| `startsFrom` | Inclusive start of the range, in local Buenos Aires time. |
| `startsTo` | Exclusive end of the range. It must be later than `startsFrom` when both are supplied. |
| `page` | Zero-based page number, default `0`. |
| `size` | Page size from 1 to 50, default `12`. |

The response contains `items`, `page`, `size` and `hasNext`. Results are sorted by start time and UUID. Invalid filters return `400` in the common `ApiError` format.

## Map and Availability

The map shows approximate neighborhood locations. The meeting point is the text the walker entered when publishing the walk. Availability can change after a search, so the booking transaction checks it again before confirming.

To check the flow, choose a neighborhood and a date, reload the results, and open a walk. Past, full and cancelled walks should not appear as bookable. The [local setup guide](ejecucion-local.md) has the full walkthrough.

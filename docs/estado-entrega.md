# Delivery Status

## What You Can Try

| Area | What you can do in the app |
|---|---|
| Accounts and profiles | Sign up and sign in as an owner or a walker. Owners can add their first pet during sign-up. Walkers can update their profile and service areas. |
| Walks and schedule | Publish a walk in a neighborhood you cover, check upcoming and past walks, and start or finish a walk. |
| Search and bookings | Filter by neighborhood, date or time, book for one of your pets, cancel within the allowed window, and rebook if a spot is available. |
| Booking history and reviews | Find a booking under Upcoming, In progress or History. From History, you can review a completed walk you took part in. |
| Checks | Tests cover overlapping walks, permissions, the last available spot, status changes and how bookings are grouped. You cannot join a walk once its scheduled start time arrives. |

The [local setup guide](ejecucion-local.md) has the commands and a walkthrough. The [final report](Informe_Final_Proyecto.pdf) includes the data model and screenshots for both roles.

## Stage 3 Review

We expanded the report after the tutor’s feedback. It now explains the problem and its documentary evidence, the two actors, modules, entities, technical decisions and relevant use cases. It separates current restrictions from possible future work and describes the tools we used, including AI assistance. The [point-by-point response](correcciones-etapa-3.md) links each observation to the revised report.

The [demo video](https://drive.google.com/file/d/1tM3KPhU4TORwfLzzkiR_hNMquNya0CiE/view?usp=sharing) and [public app](https://dog-walk-app.onrender.com/) have published links. Before the exam, we still need to confirm access without a signed-in team account, repeat the public walkthrough and check database persistence.

In the November 24 review, all 51 frontend tests, the production build and lint passed. The build reports a JavaScript bundle above 500 kB, which remains a performance improvement. The latest [backend](https://github.com/loveragabriel/trabajo-practico-integrador/actions/runs/43103149189) and [frontend](https://github.com/loveragabriel/trabajo-practico-integrador/actions/runs/43099890827) CI runs succeeded. We could not repeat Maven locally because Maven Central name resolution failed before compilation. The earlier report’s 211 backend tests remain a previous result, and database integration tests still need an explicit run against a separate test database.

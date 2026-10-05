# ballpark-ticketing-be

Backend for a ballpark ticket booking service.

## Stack

- Java 25 (toolchain; Gradle downloads it automatically if it is not installed)
- Spring Boot 4.1 (Spring Framework 7), Spring Web MVC, Spring Data JPA (Hibernate 7)
- MySQL 8.4
- JUnit 5 + Testcontainers

## Packages

```
com.ballpark.ticketing
├── game         Section, Seat, Game, GameSeat
├── reservation  Reservation, ReservationSeat
├── payment      Payment
└── common       shared JPA config and base entity
```

## Configuration notes

- Status columns (`game`, `game_seat`, `reservation`, `payment`) are `varchar(20)`, not MySQL native `enum`: `preferred_enum_jdbc_type: VARCHAR` in `application.yml`. Adding a status value does not need an `ALTER TABLE` (ISSUE-03).
- Times are Korean local time (`Asia/Seoul`). Every timestamp reads from the `Clock` bean in `TimeConfig`, not the JVM or DB default, so the server's time zone does not change stored values (ISSUE-06).
- `spring.jpa.open-in-view: false`: lazy loading only happens inside a transaction, so an entity is never lazily loaded from a controller or during JSON serialization, and a DB connection is not held for the whole request.

## Admin API

| Method | Path | Body |
|---|---|---|
| `POST` | `/api/admin/sections` | `{ "name": "Infield 101", "grade": "R", "price": 30000 }` |
| `POST` | `/api/admin/sections/{sectionId}/seats` | `{ "rowCount": 20, "seatsPerRow": 30 }` — fills a `rowCount` x `seatsPerRow` grid |
| `POST` | `/api/admin/games` | `{ "homeTeam": "...", "awayTeam": "...", "startAt": "...", "ticketOpenAt": "..." }` — also creates one `GameSeat` per existing `Seat` |

Bulk seat/game-seat creation goes through `JdbcTemplate` batch inserts, not `JpaRepository.saveAll()`. Both `Seat` and
`GameSeat` use `GenerationType.IDENTITY`, and Hibernate disables JDBC batching for that strategy (it needs each row's
generated key right away), so `saveAll()` on these tables is just N individual round trips. See
`docs/experiments/v1-02-admin-bulk-insert.md` for the measured difference.

## Query API

| Method | Path | Notes |
|---|---|---|
| `GET` | `/api/games?page=&size=` | Paged list, sorted by `startAt` by default. Max page size 100 (`spring.data.web.pageable.max-page-size`). |
| `GET` | `/api/games/{gameId}` | Detail, including `gameSeatCount`. |
| `GET` | `/api/games/{gameId}/sections` | Per-section seat counts (total/available/held/sold) for that game. |
| `GET` | `/api/games/{gameId}/seats` | Full seat map for that game (one row per `GameSeat`). |

`GameSeat.seat` and `Seat.section` are both lazy `@ManyToOne`s, so walking the entities (`gameSeat.getSeat().getSection()`)
for every row is a classic N+1. The section-availability and seat-map queries instead project straight into a DTO with a
single `join` query (`GameSeatRepository`), so each endpoint runs exactly one SQL statement regardless of seat count. See
`docs/experiments/v1-03-seat-map-n-plus-1.md` for the measured query counts (51 vs. 1 for 50 seats).

## Reservation / payment / cancellation API

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/games/{gameId}/reservations` | Body `{ "gameSeatIds": [...] }` (1–4 seats), header `X-User-Id`. Holds each seat and creates a `PENDING` reservation. |
| `POST` | `/api/reservations/{reservationId}/payments` | Body `{ "success": true\|false }` — mock payment, no real PG. `true` confirms the reservation and sells the seats; `false` just marks the payment `FAILED` so the reservation stays `PENDING` and can be retried. |
| `POST` | `/api/reservations/{reservationId}/cancel` | Pending reservation: cancels it and releases its held seats. Confirmed reservation: refunds the payment in full (mock, `PAID` → `REFUNDED`), cancels it, and sets its seats back to `AVAILABLE`. Confirmed reservations can only be cancelled before the game starts (`RESERVATION-007` otherwise). |
| `PATCH` | `/api/admin/games/{gameId}/open` | Admin: `SCHEDULED` → `OPEN`. Needed before any reservation can be made against that game. |

Rules enforced in `ReservationService`:
- The game must be `OPEN` (`GAME-003` otherwise).
- Every requested `GameSeat` must belong to the game in the path, not just exist somewhere (`RESERVATION-004`) — this is #1 from the issue tracker.
- A user can hold at most 4 seats per game, counted across all of that user's non-cancelled reservations for it, not just the current request (`RESERVATION-005`).

## Query API

Every error is returned as RFC 9457 `application/problem+json`. Besides the standard fields, `code` carries the application error code (`ErrorCode`), and validation failures add `errors`.

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "선점할 수 없는 좌석입니다.",
  "instance": "/api/games/1/reservations",
  "code": "SEAT-002"
}
```

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "입력값이 올바르지 않습니다.",
  "instance": "/api/games/1/reservations",
  "code": "COMMON-001",
  "errors": [
    { "field": "seatIds", "reason": "크기가 0에서 4 사이여야 합니다" }
  ]
}
```

- Domain rule violations throw `BusinessException(ErrorCode)`. The HTTP status comes from the `ErrorCode`.
- Unexpected exceptions return `COMMON-999` without exposing the internal message, and are logged with the stack trace.

## Demo seed

`seed` 프로필을 켜면 구역 30개, 좌석 22,536석, 경기 5개(예매 오픈 2개)가 들어간다. 시딩은 구역이 이미 있으면
건너뛴다. 

```bash
./gradlew bootRun --args='--spring.profiles.active=local,seed'
```

프론트엔드 지도가 구역 이름 체계(중앙석/1·3루 필드석/1·3루 외야석 x A~C x 1·2블록)를 해석하므로 구역 이름은
바꾸지 않는다. 측정 결과는 `docs/experiments/v1-05-seeding.md`.

## Running locally

Requirements: Docker. A local JDK 21+ is enough to launch Gradle.

```bash
docker compose up -d          # MySQL on localhost:3306 (db/user/password: ballpark)
./gradlew bootRun             # runs with the `local` profile by default
curl localhost:8080/actuator/health
```

Stop MySQL with `docker compose down` (add `-v` to drop the data volume).

## Tests

```bash
./gradlew test
```

Tests run with the `test` profile and start a throwaway MySQL container through Testcontainers, so Docker must be running.

# Car Rental — technical exercise

Simulated car rental system: reserve a car of a given type (Sedan, SUV, Van) at a given
date/time for N days, with a limited number of cars per type.

Java 21, Spring Boot 3.5, Spring Data JPA, H2 (in-memory), JUnit 5.

```
mvn test                 # run all tests
mvn spring-boot:run      # start on :8080
```

## API

| Method | Path | Description |
|---|---|---|
| POST | `/api/reservations` | `{"carType":"SUV","startAt":"2030-06-01T10:00:00","days":3}` → 201 + Location, 409 if none available |
| GET | `/api/reservations/{id}` | Get reservation |
| DELETE | `/api/reservations/{id}` | Cancel reservation (frees the car) |
| GET | `/api/availability?carType=SUV&startAt=...&days=3` | Number of free cars for the period |

Errors are returned as RFC 7807 `ProblemDetail` (400 validation, 404 not found, 409 no availability).

## Structure

```
api/          REST controller, request/response DTOs, exception → HTTP mapping
service/      ReservationService — use case + transaction + concurrency control
domain/       Car, Reservation, RentalPeriod (value object), CarType
repository/   Spring Data JPA repositories (overlap query, pessimistic lock)
config/       fleet seeding from application.yml, Clock bean
```

## Key design decisions

**1. Reservations are assigned to a concrete car, not counted per type.**
Fleet size = number of `Car` rows. The invariant is simple and checkable:
*no car has two overlapping ACTIVE reservations*. A per-type counter can't express
"busy at different times" and drifts on bugs/cancellations.

**2. `RentalPeriod` is an immutable value object with a half-open interval `[start, end)`.**
Invariants (end after start, 1..30 days) are enforced in the constructor, so an invalid
period cannot exist. Half-open means back-to-back rentals don't conflict:
a car returned at 10:00 can be picked up at 10:00. Overlap rule:
`a.start < b.end && b.start < a.end`.

**3. Availability check runs in the database.**
`findBusyCarIds` returns IDs of cars with an overlapping ACTIVE reservation; the service
picks the first car not in that set. Reservation history is never loaded into memory.
Composite index on `(car_id, start_at, end_at)`.

**4. Concurrency: no overbooking under parallel requests.**
"Find a free car, then insert" is a check-then-act race: two requests can both take the
last free car. The service first takes a pessimistic lock (`SELECT ... FOR UPDATE`) on the
cars of the requested type, so check and insert are atomic per type. Different types
don't block each other. `ConcurrentReservationTest` fires 20 parallel requests at 3 SUVs
and asserts exactly 3 succeed and no car is used twice.

**5. Time is injected (`Clock`).** The "cannot start in the past" rule is deterministic in tests.

**6. Layer boundaries.** Entities never leave the controller (DTOs). Bean Validation
checks request shape; business rules live in domain/service so they hold for any caller.

## Tests

| Test | Level | What it proves |
|---|---|---|
| `RentalPeriodTest` | pure unit | overlap edge cases: identical, partial, contained, back-to-back, 1 minute overlap; invalid days |
| `ReservationServiceTest` | integration (H2) | reservation by type/date/days, fleet limit, independent types, back-to-back reuse, cancellation frees car, past dates rejected |
| `ConcurrentReservationTest` | integration, multi-threaded | no overbooking under contention (repeated 5×) |
| `ReservationControllerTest` | web slice (MockMvc) | HTTP contract, validation → 400, errors → 404/409 |

The service is tested against a real DB rather than mocked repositories, because part of
the business rule (the overlap query) is in JPQL — mocking it would test nothing.

## Known limitations and trade-offs

- **Lock granularity.** Locking all cars of a type serializes reservations of that type.
  Fine for a fleet of this size; at scale, alternatives are optimistic locking with a
  version per car + retry, or a DB-level guarantee such as a PostgreSQL exclusion
  constraint (`EXCLUDE USING gist (car_id WITH =, tsrange(start_at, end_at) WITH &&)`),
  which makes overbooking impossible regardless of application code.
- **Time zones.** `LocalDateTime` assumes one rental location / time zone. Multi-location
  would need `ZonedDateTime`/`Instant` plus location on the car.
- **Car allocation** is "first free car by id". A strategy (e.g. least-used car, minimize
  fragmentation of the schedule) could be extracted behind an interface.
- **Max rental length (30 days) is an assumption, not from the spec.** Added as a sanity
  limit against input errors (e.g. `days: 3000` silently blocking a car for years). Kept in
  one constant, `RentalPeriod.MAX_DAYS`, which the request validation also uses; long-term
  rentals would be a separate product with its own rules anyway.
- **Not included:** customers/authentication, pricing, pickup/return lifecycle,
  idempotency keys for POST retries, pagination, DB migrations (Flyway) —
  H2 with `ddl-auto` is used for simplicity.

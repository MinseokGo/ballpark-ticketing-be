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

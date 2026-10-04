# CLAUDE.md

이 파일은 Claude Code(CLI)가 이 저장소에서 작업할 때 따르는 가이드다. 사람이 읽어도 되도록 쓴다.

## 프로젝트

야구장 좌석 예매 서비스의 백엔드. v1에서 일반 CRUD로 예매 흐름을 완성하고, v2~v7에서 동시성·Redis·대기열·결제·확장 문제를 하나씩 드러내고 해결하며 수치로 비교한다.

- 원칙: **필요해지기 전에 기술을 넣지 않는다.** v1은 동시성을 의도적으로 고려하지 않는다.
- 설계 문서(저장소 밖, Obsidian): `/Users/minseokgo/Documents/Obsidian/야구장 예매/`
  - `00. Design/0. ERD/('26.10.04) v1..md`: v1 ERD 설계서 (테이블 명세, 설계 결정 DR-01~08, 미결 사항)
  - DESIGN.md(최종 목표 설계), ROADMAP.md(버전별 실행 계획)는 사용자가 따로 가지고 있다. 범위가 애매하면 추측하지 말고 물어본다.

## 기술 스택

- Java 25 툴체인 (Gradle이 자동으로 내려받음, Gradle 실행은 JDK 21+), Gradle Kotlin DSL
- Spring Boot 4.1.1 (Spring Framework 7, Hibernate 7, Jackson 3)
- MySQL 8.4, JUnit 5 + Testcontainers
- v1에서 쓰지 않는 것: QueryDSL, Redis, Kafka, Flyway(#4에서 검토)

## 명령어

```bash
docker compose up -d                 # 로컬 MySQL (localhost:3306, ballpark/ballpark)
./gradlew bootRun                    # local 프로필로 실행
./gradlew build                      # 컴파일 + 전체 테스트 (Docker 필요)
./gradlew test --tests '*GameSeatTest'   # 특정 테스트만
```

작업을 끝냈다고 말하기 전에 `./gradlew build`가 통과하는지 확인한다. Docker가 꺼져 있으면 `open -a Docker`로 켠다.

## 패키지 구조

```
com.ballpark.ticketing
├── game         Section, Seat, Game, GameSeat
├── reservation  Reservation, ReservationSeat
├── payment      Payment
└── common       config(JpaAuditingConfig), entity(BaseTimeEntity), exception(ErrorCode, BusinessException, GlobalExceptionHandler)
```

API를 추가할 때는 도메인 패키지 안에 controller, service, repository, dto를 둔다.

## 코드 규칙

### 엔티티
- setter를 두지 않는다. 상태 변경은 의미 있는 메서드로만 한다 (`hold()`, `sell()`, `release()`, `confirm()`, `cancel()`).
- 상태 전이 검증은 엔티티 안에서 하고, 위반하면 `BusinessException(ErrorCode.XXX)`를 던진다. `IllegalStateException`을 쓰지 않는다.
- 연관관계는 모두 `@ManyToOne(fetch = LAZY, optional = false)` 단방향이 기본. 양방향은 생명주기를 함께 관리할 때만 (Reservation–ReservationSeat).
- `@Enumerated(EnumType.STRING)`. 기본 생성자는 `protected`.
- 금액은 원 단위 `long`.
- v1에서는 `version`, `heldUntil`, `idempotencyKey`를 넣지 않는다. 필요해지는 버전에서 이유와 함께 추가한다.
- `spring.jpa.open-in-view: false`. 지연 로딩은 트랜잭션 안에서만. 조회 API는 fetch join 또는 DTO 직접 조회로 N+1을 막는다.

### API
- 엔티티를 그대로 반환하지 않는다. Request/Response DTO를 분리한다 (record 권장).
- 입력 검증은 Bean Validation (`@Valid`, `@NotEmpty`, `@Size(max = 4)` 등).
- 사용자 식별은 `X-User-Id` 헤더 (인증은 v1 범위 밖).
- 에러 응답은 `GlobalExceptionHandler`가 ProblemDetail(RFC 9457)로 통일한다. 새 에러는 `ErrorCode`에 추가한다.
  - 코드 형식: 도메인 접두어 + 번호 (`COMMON-001`, `SEAT-002`, `GAME-001`, `RESERVATION-001`, `PAYMENT-001`)
  - 메시지는 한국어. 상태가 안 맞으면 409, 입력이 잘못되면 400, 없으면 404.

### 스타일
- 들여쓰기는 탭. import는 `com.ballpark` → `jakarta` → `java` → `lombok` → `org` 순.
- 주석은 "왜"가 필요한 곳에만, 한국어로.

## 테스트

| 종류 | 방법 |
|---|---|
| 도메인 단위 | 순수 JUnit. 상태 전이와 `ErrorCode`까지 검증 |
| JPA / 서비스 통합 | `@Import(TestcontainersConfiguration.class)` + `@ActiveProfiles("test")`. `@DataJpaTest`는 `@AutoConfigureTestDatabase(replace = NONE)` 추가, 감사(auditing)가 필요하면 `JpaAuditingConfig`도 import |
| 컨트롤러 | `@WebMvcTest` + `MockMvc` |

테스트 데이터의 팀 이름은 가상의 이름만 쓴다 (Seoul Comets, Busan Gulls, Daegu Owls 등).

### Spring Boot 4 주의점
- 테스트 어노테이션 패키지가 바뀌었다.
  - `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`
  - `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`
  - `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`
  - `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`
- 스타터가 모듈화됐다 (`spring-boot-starter-webmvc`, 각 스타터의 `-test` 모듈). Boot 3 블로그 예제의 의존성 이름과 다를 수 있다.
- Jackson 3의 패키지는 `tools.jackson.*` (어노테이션은 `com.fasterxml.jackson.annotation` 유지).
- `@Nullable`은 `org.jspecify.annotations.Nullable`.
- Hibernate 7은 MySQL에서 `EnumType.STRING` 컬럼을 네이티브 `enum` 타입으로 만든다 (#3).

## Git / GitHub 규칙

- **커밋, PR, 이슈는 모두 한국어로 쓴다.**
- **커밋 작성자를 직접 지정하지 않는다.** 이 Mac의 git 설정(`MinseokGo <rhalstjr1999@naver.com>`)을 그대로 쓴다. 다른 이메일을 넣으면 GitHub에서 다른 계정(MinseokUhm)으로 표시된다.
- 브랜치: `main` + 기능 브랜치 (`feat/v1-admin-api`, `fix/...`, `docs/...`) → PR로 머지. `main`에 직접 push하지 않는다.
- 커밋은 역할별로 나누고, 접두어 + 자세한 본문을 쓴다.
  - 접두어: `build`, `chore`, `feat(도메인)`, `fix`, `refactor`, `test`, `docs`
  - 본문: 무엇을, **왜** 바꿨는지, 관련 이슈 번호
  - ❌ `락 추가` ✅ `feat: 좌석 선점에 조건부 UPDATE 적용 (S2 p99 2.3s → 0.4s, docs/experiments/v3 참고)`
- PR 본문: Before / After / How / 테스트 결과. 실험이 있으면 결과 요약.
- 라벨은 직접 붙인다. 이슈와 PR 모두.
  - 타입: `타입: 기능`, `타입: 버그`, `타입: 리팩터링`, `타입: 설계`, `타입: 설정`, `타입: 문서`, `타입: 테스트`
  - 도메인: `도메인: 경기·좌석`, `도메인: 예약`, `도메인: 결제`, `도메인: 공통`
  - 우선순위: `우선순위: 높음`, `우선순위: 보통`, `우선순위: 낮음`
  - 버전: `버전: v1`, `버전: v2`
- 작업 중 발견한 후속 과제는 라벨을 붙여 이슈로 등록한다.
- 버전이 끝나면 `git tag v1.0`, `v2.0` …

## 진행 상황 (v1)

| 단계 | 내용 | 상태 |
|---|---|---|
| 0 | 프로젝트 뼈대, v1 엔티티 7개 | 완료 (PR #7) |
| 1 | 공통 예외 처리 (ErrorCode, ProblemDetail) | PR #8 |
| 2 | 관리자 API: 구역 등록, 좌석 일괄 등록, 경기 등록 시 GameSeat 자동 생성 (`saveAll` vs JDBC batch 시간 측정 기록) | 완료 (PR #10) |
| 3 | 조회 API: 경기 목록(페이징), 상세, 구역별 잔여석, 좌석맵 (N+1 확인 후 해결) | 완료 (PR #11) |
| 4 | 예매, 결제(Mock), 취소 API: 1인 경기당 최대 4매, 오픈 전 예매 불가, #1 경기 일치 검증 | 진행 중 |
| 5 | 시딩(구역 30, 좌석 22,000, 경기 5), `docs/experiments/v1-*.md`, `git tag v1.0` | |

열린 이슈: #1 예약 좌석–경기 일치 검증, #2 좌석 동시 선점 제어, #3 enum 컬럼 타입, #4 마이그레이션 도구, #5 결제 취소·환불, #6 시간대 통일. 최신 상태는 `gh issue list`로 확인한다.

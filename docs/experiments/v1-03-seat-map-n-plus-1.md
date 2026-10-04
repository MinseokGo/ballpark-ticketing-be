# v1 3단계 — 좌석맵 조회: N+1 확인과 해결

| 항목 | 내용 |
|---|---|
| 측정일 | 2026-10-04 |
| 대상 | `GET /api/games/{gameId}/seats` (좌석맵) |
| 환경 | Testcontainers MySQL 8.4 (로컬 Docker), `hibernate.generate_statistics=true` |
| 테스트 | `SeatMapNPlusOneTest.fetchJoinProjectionAvoidsTheNPlusOneThatLazyLoadingCauses` |

## 배경

좌석맵은 경기 하나의 `GameSeat`를 전부 내려주면서 각 칸에 좌석 위치(행·열)와 구역 정보(이름, 등급)를
같이 보여줘야 한다. `GameSeat.seat`와 `Seat.section`은 모두 `@ManyToOne(fetch = LAZY)`라, `GameSeat`를
엔티티로 조회한 뒤 화면에 필요한 값을 얻으려고 `seat.getSection()`처럼 연관관계를 그대로 따라가면 좌석
수만큼 추가 SELECT가 나가는 전형적인 N+1이 된다.

## 방법

구역 하나에 좌석 50개(5행 × 10열)를 만들고 경기를 등록해 `GameSeat` 50건을 준비한 뒤, 같은 데이터를
두 가지 방식으로 읽어 Hibernate 통계(`Statistics.getPrepareStatementCount()`, 실제 DB로 나간 SQL 개수)를
비교했다.

1. **엔티티 순회**: `GameSeatRepository.findAll()`로 50건을 읽고, 각 건에 `gameSeat.getSeat().getSection()`을
   호출해 지연 로딩을 강제로 터뜨린다.
2. **join DTO 프로젝션**: `GameSeatRepository.findSeatMapByGameId(gameId)` — `GameSeat`-`Seat`-`Section`을
   JPQL에서 바로 `join`하고, 필요한 컬럼만 `SeatMapItemResponse` 생성자 표현식으로 선택한다(엔티티를 받지
   않으므로 지연 로딩이 끼어들 수 없다).

## 결과

| 방식 | SQL 실행 횟수 |
|---|---|
| 엔티티 순회 (N+1) | 51 |
| join DTO 프로젝션 | 1 |

51번 = `findAll` 1번 + 좌석(Seat) 지연 로딩 50번. 테스트에서는 구역(Section)이 이미 같은 트랜잭션에서
`sectionRepository.save()`로 영속화돼 있어 1차 캐시에 걸려 있었기 때문에 `Section` 로딩은 추가 쿼리로
잡히지 않았다 — 즉 51은 최선의 경우이고, 실제 요청(새 세션, 여러 구역이 섞인 경기)에서는 구역 수만큼
더 늘어날 수 있다.

## 결론 및 적용

- `GET /api/games/{gameId}/seats`(`GameController.getSeatMap`)는 `GameSeatRepository.findSeatMapByGameId`를
  쓴다. 엔티티를 거치지 않고 JPQL 생성자 표현식으로 필요한 컬럼만 한 번에 조회해, 좌석 수와 무관하게 SQL
  1번으로 끝난다.
- 같은 이유로 `GET /api/games/{gameId}/sections`(구역별 잔여석)도 `GameSeat`를 엔티티로 조회해 메모리에서
  구역별로 묶는 대신, `findSectionAvailabilityByGameId`에서 `group by` + 조건부 `sum`으로 DB에서 바로
  집계한다. SQL 1번으로 끝나는 건 동일한 구조라 별도로 재지 않았다.
- 교훈: `GameSeat`처럼 양이 많고 지연 로딩 연관관계가 여러 단계로 이어지는 엔티티는, 조회 API에서 엔티티를
  그대로 순회하지 말고 처음부터 DTO 프로젝션으로 받는다. `@EntityGraph`나 `join fetch`로 엔티티를 그대로
  가져오는 방법도 N+1은 피하지만, 쓰지 않을 컬럼(예: Section의 price)까지 전부 읽어오므로 좌석맵처럼
  "화면에 보여줄 값만 필요한" 조회에는 생성자 표현식 프로젝션이 더 가볍다.

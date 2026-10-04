# v1 2단계 — 관리자 API 대량 등록: saveAll vs JDBC batch

| 항목 | 내용 |
|---|---|
| 측정일 | 2026-10-04 |
| 대상 | 좌석(Seat) 대량 등록, 경기 등록 시 GameSeat 자동 생성 |
| 환경 | Testcontainers MySQL 8.4 (로컬 Docker), `rewriteBatchedStatements=true` |
| 테스트 | `SeatBulkInsertBenchmarkTest.comparesSaveAllAgainstJdbcBatchInsert` |

## 배경

관리자 API의 "좌석 일괄 등록"과 "경기 등록 시 GameSeat 자동 생성"은 한 번의 요청으로 수백~수만 건의 행을 만든다.
`Seat`, `GameSeat` 모두 `GenerationType.IDENTITY`를 쓰는데, Hibernate는 IDENTITY 전략에서 **INSERT마다 생성된 키를
즉시 읽어야 하므로 JDBC 배치를 적용하지 않는다.** 즉 `JpaRepository.saveAll()`을 불러도 내부적으로는 한 건씩
INSERT가 나간다. 이게 실제로 얼마나 차이 나는지 숫자로 확인하고, 그 결과로 구현 방식을 정했다.

## 방법

같은 구조(Section 1개 아래 100행 × 30열 = 3,000개 좌석)를 두 가지 방식으로 삽입해 소요 시간을 측정했다.

1. **saveAll**: `Seat` 엔티티 3,000개를 만들어 `SeatRepository.saveAll(List<Seat>)` 호출
2. **JDBC batch**: `JdbcTemplate.batchUpdate(sql, positions, 500, ...)`로 500건씩 청크 배치 삽입 (`SeatJdbcRepository`)

두 방식 모두 같은 MySQL 컨테이너, 같은 트랜잭션 경계(`@Transactional` 테스트, 끝나면 롤백) 안에서 측정했다.
MySQL Connector/J는 `rewriteBatchedStatements=true`가 없으면 JDBC 배치를 멀티-row INSERT로 재작성하지 않고
한 건씩 보내므로, 이 옵션을 `application.yml`과 `TestcontainersConfiguration`(테스트 컨테이너)에 모두 켜 두었다.

## 결과

3,000건 삽입 기준, 2회 반복 측정(ms):

| 방식 | 1차 | 2차 |
|---|---|---|
| `saveAll` | 2,352 | 1,661 |
| JDBC batch (청크 500) | 51 | 44 |

**JDBC batch가 saveAll보다 약 35~46배 빠르다.** 건당 시간으로 보면 saveAll은 약 0.55~0.78ms/건(각 건마다
개별 라운드트립), JDBC batch는 약 0.015~0.017ms/건이다.

## 결론 및 적용

- **좌석 일괄 등록**(`AdminSeatController` → `SeatService.createBulk`)과 **경기 등록 시 GameSeat 자동 생성**
  (`AdminGameController` → `GameService.create`)은 모두 `SeatJdbcRepository` / `GameSeatJdbcRepository`의
  JDBC batch 삽입을 쓴다. `saveAll`은 엔티티 생성 편의는 있지만 IDENTITY 전략에서는 배치의 이점이 전혀 없어
  채택하지 않았다.
- 배치 청크 크기는 500으로 고정했다(`BATCH_CHUNK_SIZE`). 5단계 시딩(좌석 22,000건)에서 한 번에 너무 큰 배치를
  보내 `max_allowed_packet`을 넘기지 않도록 하기 위함이다. 더 큰 규모로 시딩할 때 재측정해 조정할 수 있다.
- `rewriteBatchedStatements=true`가 없으면 JDBC batch의 이점이 사라진다(Connector/J가 배치를 멀티-row INSERT로
  합치지 못하고 개별 전송하기 때문). 로컬 프로필과 테스트 컨테이너 양쪽에 반드시 켜 둬야 한다.
- 이 벤치마크 수치 자체는 테스트 환경(로컬 Docker, 컨테이너 리소스)에 따라 달라질 수 있으므로, 테스트 코드에는
  성능 단언(assertion)을 넣지 않고 결과 건수만 검증한다. 수치는 이 문서에만 기록한다.

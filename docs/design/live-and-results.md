# 실시간 경기 흐름과 경기 결과 설계

- 상태: 구현 중 (`feat/live-and-results`)
- 대상: 백엔드 `game` 도메인, 프론트엔드 홈·일정·예매·마이페이지
- 버전: 미정 (로드맵 확정 후 `버전: vN` 라벨을 붙인다)

## 1. 원칙

- 데이터의 주인은 백엔드 하나다. 프론트의 실시간 값은 시뮬레이션(`liveMock`)이고, 이 API가 생기면 교체한다.
- 입력 원천은 관리자 입력이다. 외부 중계 피드는 범위 밖이다.
- 상태 변화는 이벤트 로그로 남긴다. 현재 상태는 경기 행에 두고, 로그는 재접속 복구와 결과 기록에 쓴다.
- 점수와 이닝은 절대값으로 보낸다. 이벤트를 두 번 받아도 화면 상태가 같다(멱등).

## 2. 데이터 모델

### `game` 추가 컬럼
| 컬럼 | 설명 |
|---|---|
| `progress` | `NOT_STARTED` / `LIVE` / `FINISHED` / `CANCELLED`. 예매 상태(`status`)와 분리한다. |
| `inning`, `half` | 현재 이닝과 `TOP`/`BOTTOM` |
| `home_score`, `away_score` | 현재 점수 |
| `event_seq` | 경기 안에서 마지막으로 매긴 이벤트 번호 |

승패(`HOME`/`AWAY`/`DRAW`)는 저장하지 않고 점수로 계산한다. 정정이 생겨도 점수만 고치면 된다.

### `game_event` (신규)
| 컬럼 | 설명 |
|---|---|
| `game_id`, `seq` | `(game_id, seq)` 유니크. `seq`는 경기마다 1부터 증가 |
| `type` | `GAME_STARTED`, `INNING_CHANGED`, `SCORE_CHANGED`, `SCORE_CORRECTED`, `GAME_FINISHED`, `GAME_CANCELLED` |
| `inning`, `half`, `home_score`, `away_score` | 이벤트 시점의 값 |
| `created_at` | 감사 시각(Asia/Seoul Clock) |

## 3. 상태 규칙 (Game 엔티티)

| 이벤트 | 허용 상태 | 규칙 |
|---|---|---|
| `GAME_STARTED` | `NOT_STARTED` | 1회, 이닝 1 초로 시작 |
| `INNING_CHANGED` | `LIVE` | 이닝·초/말 위치가 앞으로만 간다 (`GAME-008`) |
| `SCORE_CHANGED` | `LIVE` | 점수는 줄지 않는다 (`GAME-009`) |
| `SCORE_CORRECTED` | `LIVE` | 잘못 입력한 점수를 고친다. 줄어들 수 있다 |
| `GAME_FINISHED` | `LIVE` | 마지막 점수로 종료. 이후 이벤트 거부 |
| `GAME_CANCELLED` | `NOT_STARTED`, `LIVE` | 우천 등. 이후 이벤트 거부 |

종료(`FINISHED`)·취소(`CANCELLED`) 경기에는 어떤 이벤트도 받지 않는다 (`GAME-007`). 진행 중이 아닌 경기에 진행 이벤트를 보내면 `GAME-005`.

## 4. API

### 관리자 (쓰기)
- `POST /api/admin/games/{gameId}/events` — 본문 `{type, inning?, half?, homeScore?, awayScore?}`. 응답 201 + `LiveEventResponse`.
- 한 트랜잭션에서 경기 행을 `PESSIMISTIC_WRITE`로 잠그고 `seq`를 올린다.
- 필수 값이 빠지면 `COMMON-001`(400).

### 공개 (읽기)
- `GET /api/games/{gameId}/live` — 스냅샷 `{progress, inning, half, homeScore, awayScore, seq}`.
- `GET /api/games/{gameId}/live/stream` — SSE(`text/event-stream`).
  - 이벤트 `id`는 `seq`다. 재접속은 `Last-Event-ID` 헤더로 그 이후 이벤트를 로그에서 다시 보낸다.
  - 처음 연결하면 로그 전체를 먼저 보낸다. 경기 수가 적어 전체 로그가 작다.
  - 15초마다 주석 heartbeat를 보낸다.
  - 종료·취소 이벤트를 보내면 연결을 닫는다.
  - 구독을 먼저 등록하고 리플레이한다. 그 사이에 온 이벤트는 두 번 갈 수 있지만 절대값이라 문제없다.
- `GET /api/games?progress=` — 목록 필터. 목록 응답에 `progress`, `homeScore`, `awayScore`, `winner`를 포함한다.
- `GET /api/games/{gameId}` — 상세 응답에도 같은 필드를 포함한다.

### 실시간 연결 관리 (`GameLiveEventHub`)
- 경기별 `SseEmitter` 목록을 메모리에 둔다. 인스턴스가 하나인 동안만 유효하다.
- 여러 인스턴스로 가면 Redis pub/sub을 그때 도입한다. 필요해지기 전에는 넣지 않는다.
- 이벤트 전송은 커밋 이후에 한다(`afterCommit`). 롤백된 이벤트가 나가지 않게 하려는 것이다.

## 5. 프론트엔드 적용 (계획)

| 화면 | 방식 |
|---|---|
| 홈 "지금 진행 중" | 목록을 15~20초마다 폴링. `liveMock` 제거 |
| 예매 화면 | 스냅샷 + SSE. 끊기면 10초 폴링 |
| 팀별 일정 | 종료 경기에 점수와 승/패/무 표시 |
| 홈 "최근 결과" | `progress=FINISHED` 최근 경기 |
| 마이페이지 | 내 예매 경기가 끝났으면 결과 표시 |

## 6. 테스트 계획

- 도메인: 전이 규칙(시작 전 이벤트 거부, 종료 후 거부, 점수·이닝 역행 거부, 정정 허용, 승패 계산).
- 서비스 통합: `seq` 증가, `Last-Event-ID` 이후 이벤트만 반환, 커밋 전 이벤트 미전송.
- 컨트롤러: 스냅샷 응답, SSE 응답 헤더, 관리자 입력 400·409.

## 7. 열린 질문

1. 버전 위치: 어느 버전에 넣을지 로드맵에서 정한다.
2. 우천 취소: `GAME_CANCELLED` 때 확정 예약을 자동 환불할지. 제안은 "경기 취소 시 전액 자동 환불"이다. DR-09 정책과 맞춰야 한다.
3. 결과 정정: 종료 후 점수를 고칠 수 있게 할지. 현재는 막는다.

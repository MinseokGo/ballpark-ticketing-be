#!/usr/bin/env bash
# 데모용 진행 데이터를 넣는다: 지난 경기 2개(종료, 결과 있음)와 지금 진행 중인 경기 2개.
# seed 프로필로 경기 목록을 먼저 만든 뒤 한 번만 실행한다. 지난 경기는 실행할 때마다 새로 만들어지므로 여러 번 실행하지 않는다.
# 진행 중 경기는 아직 NOT_STARTED인 예정 경기에서 고른다.
#   BASE_URL=http://localhost:8080 ./scripts/seed-live-results.sh
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

post_json() { curl -sf -X POST "$BASE_URL$1" -H 'Content-Type: application/json' -d "$2" > /dev/null; }
event() { post_json "/api/admin/games/$1/events" "$2"; }

# 진행 상태가 NOT_STARTED인 예정 경기 id를 앞에서부터 고른다.
upcoming_ids=$(curl -sf "$BASE_URL/api/games?size=100&sort=startAt" | python3 -c '
import json, sys
games = json.load(sys.stdin)["content"]
print(" ".join(str(g["id"]) for g in games if g["progress"] == "NOT_STARTED"))
')
read -r live_a live_b _ <<< "$upcoming_ids"

# 지난 경기 두 개를 만든다(예매 마감, 종료 상태). 일정은 데모용 임의 값이다.
past_a=$(curl -sf -X POST "$BASE_URL/api/admin/games" -H 'Content-Type: application/json' \
  -d '{"homeTeam":"롯데 자이언츠","awayTeam":"NC 다이노스","startAt":"2026-10-01T18:30:00","ticketOpenAt":"2026-09-20T11:00:00"}' \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')
past_b=$(curl -sf -X POST "$BASE_URL/api/admin/games" -H 'Content-Type: application/json' \
  -d '{"homeTeam":"한화 이글스","awayTeam":"SSG 랜더스","startAt":"2026-10-02T18:30:00","ticketOpenAt":"2026-09-21T11:00:00"}' \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')

event "$past_a" '{"type":"GAME_STARTED"}'
event "$past_a" '{"type":"INNING_CHANGED","inning":1,"half":"BOTTOM"}'
event "$past_a" '{"type":"SCORE_CHANGED","homeScore":2,"awayScore":0}'
event "$past_a" '{"type":"INNING_CHANGED","inning":5,"half":"TOP"}'
event "$past_a" '{"type":"SCORE_CHANGED","homeScore":3,"awayScore":4}'
event "$past_a" '{"type":"GAME_FINISHED","homeScore":3,"awayScore":5}'

event "$past_b" '{"type":"GAME_STARTED"}'
event "$past_b" '{"type":"SCORE_CHANGED","homeScore":1,"awayScore":1}'
event "$past_b" '{"type":"INNING_CHANGED","inning":4,"half":"BOTTOM"}'
event "$past_b" '{"type":"GAME_FINISHED","homeScore":4,"awayScore":4}'

# 지금 진행 중인 예정 경기 두 개
if [ -n "${live_a:-}" ]; then
  event "$live_a" '{"type":"GAME_STARTED"}'
  event "$live_a" '{"type":"INNING_CHANGED","inning":3,"half":"BOTTOM"}'
  event "$live_a" '{"type":"SCORE_CHANGED","homeScore":2,"awayScore":1}'
fi
if [ -n "${live_b:-}" ]; then
  event "$live_b" '{"type":"GAME_STARTED"}'
  event "$live_b" '{"type":"INNING_CHANGED","inning":6,"half":"TOP"}'
  event "$live_b" '{"type":"SCORE_CHANGED","homeScore":4,"awayScore":4}'
fi

echo "done: finished=$past_a,$past_b live=${live_a:-none},${live_b:-none}"

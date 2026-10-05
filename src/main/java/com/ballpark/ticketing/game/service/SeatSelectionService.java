package com.ballpark.ticketing.game.service;

import com.ballpark.ticketing.game.dto.SeatSelectionResponse;
import com.ballpark.ticketing.game.live.SeatStatusHub;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 고르는 중인 좌석 표시. 메모리에만 두고, 일정 시간(30초) 갱신이 없으면 사라진다.
 * 예매를 잡지 않는다. 다른 사람에게 "지금 누가 보고 있다"를 보여 주기 위한 신호다.
 */
@Service
public class SeatSelectionService {

	private static final Duration TTL = Duration.ofSeconds(30);
	private static final int MAX_SEATS = 4;

	private record Selection(List<Long> gameSeatIds, Instant expiresAt) {
	}

	private final Map<Long, Map<Long, Selection>> byGame = new ConcurrentHashMap<>();
	private final SeatStatusHub seatStatusHub;

	public SeatSelectionService(SeatStatusHub seatStatusHub) {
		this.seatStatusHub = seatStatusHub;
	}

	/** 사용자의 고르는 중 좌석을 바꾼다. 빈 목록이면 지운다. */
	public void update(Long gameId, Long userId, List<Long> gameSeatIds) {
		Map<Long, Selection> users = byGame.computeIfAbsent(gameId, key -> new ConcurrentHashMap<>());
		if (gameSeatIds == null || gameSeatIds.isEmpty()) {
			users.remove(userId);
		} else {
			List<Long> seats = gameSeatIds.stream().distinct().limit(MAX_SEATS).toList();
			users.put(userId, new Selection(seats, Instant.now().plus(TTL)));
		}
		seatStatusHub.publishSelections(gameId, snapshot(gameId));
	}

	/** 지금 고르는 중인 좌석 목록. 만료된 항목은 뺀다. */
	public List<SeatSelectionResponse> snapshot(Long gameId) {
		Map<Long, Selection> users = byGame.getOrDefault(gameId, Map.of());
		Instant now = Instant.now();
		List<SeatSelectionResponse> result = new ArrayList<>();
		users.forEach((userId, selection) -> {
			if (selection.expiresAt().isAfter(now)) {
				selection.gameSeatIds().forEach(seatId -> result.add(new SeatSelectionResponse(seatId, userId)));
			}
		});
		return result;
	}

	/** 만료된 선택을 치우고, 변화가 있던 경기에만 알린다. */
	@Scheduled(fixedRate = 5_000)
	public void sweep() {
		Instant now = Instant.now();
		for (Map.Entry<Long, Map<Long, Selection>> game : byGame.entrySet()) {
			Set<Long> expired = new java.util.HashSet<>();
			game.getValue().forEach((userId, selection) -> {
				if (!selection.expiresAt().isAfter(now)) expired.add(userId);
			});
			if (!expired.isEmpty()) {
				expired.forEach(game.getValue()::remove);
				seatStatusHub.publishSelections(game.getKey(), snapshot(game.getKey()));
			}
		}
	}
}

package com.ballpark.ticketing.game.simulator;

import com.ballpark.ticketing.game.Player;
import com.ballpark.ticketing.game.repository.PlayerRepository;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 시뮬레이션 이벤트에 나올 선수를 고른다. 명단이 비어 있으면 빈 값을 돌려준다. */
@Component
@RequiredArgsConstructor
class RosterPicker {

	private static final String PITCHER_POSITION = "투수";

	private final PlayerRepository playerRepository;

	Optional<Long> anyPlayer(String team, RandomGenerator random) {
		return pick(playerRepository.findByTeamNameOrderByBackNumberAsc(team), random);
	}

	Optional<Long> pitcher(String team, RandomGenerator random) {
		List<Player> pitchers = playerRepository.findByTeamNameOrderByBackNumberAsc(team).stream()
				.filter(player -> PITCHER_POSITION.equals(player.getPosition()))
				.toList();
		return pick(pitchers, random);
	}

	private Optional<Long> pick(List<Player> players, RandomGenerator random) {
		if (players.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(players.get(random.nextInt(players.size())).getId());
	}
}

package com.ballpark.ticketing.game.service;

import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.dto.GameCreateRequest;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.game.repository.GameRepository;
import com.ballpark.ticketing.game.repository.GameSeatJdbcRepository;
import com.ballpark.ticketing.game.repository.SeatRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GameService {

	private final GameRepository gameRepository;
	private final SeatRepository seatRepository;
	private final GameSeatJdbcRepository gameSeatJdbcRepository;

	public GameService(
			GameRepository gameRepository, SeatRepository seatRepository,
			GameSeatJdbcRepository gameSeatJdbcRepository) {
		this.gameRepository = gameRepository;
		this.seatRepository = seatRepository;
		this.gameSeatJdbcRepository = gameSeatJdbcRepository;
	}

	/**
	 * 경기를 등록하고, 등록된 모든 좌석에 대해 경기별 좌석(GameSeat)을 AVAILABLE 상태로 일괄 생성한다.
	 */
	public GameResponse create(GameCreateRequest request) {
		Game game = gameRepository.save(
				new Game(request.homeTeam(), request.awayTeam(), request.startAt(), request.ticketOpenAt()));
		List<Long> seatIds = seatRepository.findAllIds();
		int gameSeatCount = gameSeatJdbcRepository.batchInsert(game.getId(), seatIds);
		return GameResponse.of(game, gameSeatCount);
	}
}

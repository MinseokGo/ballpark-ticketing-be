package com.ballpark.ticketing.game.service;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.dto.GameCreateRequest;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.game.repository.GameRepository;
import com.ballpark.ticketing.game.repository.GameSeatJdbcRepository;
import com.ballpark.ticketing.game.repository.GameSeatRepository;
import com.ballpark.ticketing.game.repository.SeatRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class GameService {

	private final GameRepository gameRepository;
	private final SeatRepository seatRepository;
	private final GameSeatRepository gameSeatRepository;
	private final GameSeatJdbcRepository gameSeatJdbcRepository;

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

	/**
	 * 예매를 오픈한다(SCHEDULED -> OPEN). 예매·결제 API는 이 상태가 아니면 좌석을 선점할 수 없다.
	 */
	public GameResponse openTicketing(Long gameId) {
		Game game = gameRepository.findById(gameId).orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));
		game.openTicketing();
		return GameResponse.of(game, (int) gameSeatRepository.countByGame_Id(gameId));
	}
}

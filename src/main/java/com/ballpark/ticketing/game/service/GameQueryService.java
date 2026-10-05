package com.ballpark.ticketing.game.service;

import com.ballpark.ticketing.common.dto.PageResponse;
import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameProgress;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.game.dto.GameSummaryResponse;
import com.ballpark.ticketing.game.dto.SeatMapItemResponse;
import com.ballpark.ticketing.game.dto.SectionAvailabilityResponse;
import com.ballpark.ticketing.game.repository.GameRepository;
import com.ballpark.ticketing.game.repository.GameSeatRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GameQueryService {

	private final GameRepository gameRepository;
	private final GameSeatRepository gameSeatRepository;

	public GameQueryService(GameRepository gameRepository, GameSeatRepository gameSeatRepository) {
		this.gameRepository = gameRepository;
		this.gameSeatRepository = gameSeatRepository;
	}

	public PageResponse<GameSummaryResponse> listGames(Pageable pageable) {
		return listGames(null, pageable);
	}

	public PageResponse<GameSummaryResponse> listGames(GameProgress progress, Pageable pageable) {
		Page<Game> games = progress == null
				? gameRepository.findAll(pageable)
				: gameRepository.findByProgress(progress, pageable);
		return PageResponse.from(games.map(GameSummaryResponse::from));
	}

	public GameResponse getGame(Long gameId) {
		Game game = findGame(gameId);
		return GameResponse.of(game, (int) gameSeatRepository.countByGame_Id(gameId));
	}

	public List<SectionAvailabilityResponse> getSectionAvailability(Long gameId) {
		findGame(gameId);
		return gameSeatRepository.findSectionAvailabilityByGameId(gameId);
	}

	public List<SeatMapItemResponse> getSeatMap(Long gameId) {
		findGame(gameId);
		return gameSeatRepository.findSeatMapByGameId(gameId);
	}

	private Game findGame(Long gameId) {
		return gameRepository.findById(gameId).orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));
	}
}

package com.ballpark.ticketing.game.service;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameEvent;
import com.ballpark.ticketing.game.GameEventType;
import com.ballpark.ticketing.game.Player;
import com.ballpark.ticketing.game.repository.PlayerRepository;
import com.ballpark.ticketing.game.dto.LiveEventCreateRequest;
import com.ballpark.ticketing.game.dto.LiveEventResponse;
import com.ballpark.ticketing.game.dto.LiveStateResponse;
import com.ballpark.ticketing.game.live.GameLiveEventHub;
import com.ballpark.ticketing.game.repository.GameEventRepository;
import com.ballpark.ticketing.game.repository.GameRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GameLiveService {

	private final GameRepository gameRepository;
	private final GameEventRepository gameEventRepository;
	private final GameLiveEventHub hub;
	private final PlayerRepository playerRepository;

	public GameLiveService(
			GameRepository gameRepository, GameEventRepository gameEventRepository, GameLiveEventHub hub,
			PlayerRepository playerRepository) {
		this.gameRepository = gameRepository;
		this.gameEventRepository = gameEventRepository;
		this.hub = hub;
		this.playerRepository = playerRepository;
	}

	/** 관리자 진행 이벤트를 기록하고, 커밋 후 구독자에게 보낸다. */
	public LiveEventResponse record(Long gameId, LiveEventCreateRequest request) {
		Game game = gameRepository.findByIdForUpdate(gameId)
				.orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));
		Player player = findPlayerOf(game, request.playerId());
		GameEventType type = request.type();
		switch (type) {
			case GAME_STARTED -> game.start();
			case INNING_CHANGED -> game.changeInning(required(request.inning()), required(request.half()));
			case SCORE_CHANGED -> game.advanceScore(required(request.homeScore()), required(request.awayScore()));
			case SCORE_CORRECTED -> game.correctScore(required(request.homeScore()), required(request.awayScore()));
			case GAME_FINISHED -> game.finish(required(request.homeScore()), required(request.awayScore()));
			case GAME_CANCELLED -> game.cancel();
		}
		GameEvent event = gameEventRepository.save(new GameEvent(
				game, game.nextEventSeq(), type, game.getInning(), game.getHalf(),
				game.getHomeScore(), game.getAwayScore(), player, request.detail()));
		LiveEventResponse response = LiveEventResponse.from(event);
		hub.publishAfterCommit(gameId, response);
		return response;
	}

	@Transactional(readOnly = true)
	public LiveStateResponse snapshot(Long gameId) {
		return LiveStateResponse.from(findGame(gameId));
	}

	/** 재접속 복구용. seq보다 큰 이벤트를 번호 순서대로 돌려준다. */
	@Transactional(readOnly = true)
	public List<LiveEventResponse> eventsAfter(Long gameId, int seq) {
		findGame(gameId);
		return gameEventRepository.findByGame_IdAndSeqGreaterThanOrderBySeqAsc(gameId, seq).stream()
				.map(LiveEventResponse::from)
				.toList();
	}

	/** 선수는 이 경기의 두 팀 중 한 팀 소속이어야 한다. */
	private Player findPlayerOf(Game game, Long playerId) {
		if (playerId == null) {
			return null;
		}
		Player player = playerRepository.findById(playerId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PLAYER_NOT_FOUND));
		if (!player.getTeamName().equals(game.getHomeTeam()) && !player.getTeamName().equals(game.getAwayTeam())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
		return player;
	}

	/** 재접속과 기록 화면을 위한 전체 로그(번호 순). */
	@Transactional(readOnly = true)
	public List<LiveEventResponse> allEvents(Long gameId) {
		return eventsAfter(gameId, 0);
	}

	private Game findGame(Long gameId) {
		return gameRepository.findById(gameId).orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));
	}

	private static <T> T required(T value) {
		if (value == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
		return value;
	}
}

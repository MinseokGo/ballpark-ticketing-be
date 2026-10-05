package com.ballpark.ticketing.game.service;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameEvent;
import com.ballpark.ticketing.game.GameEventType;
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

	public GameLiveService(GameRepository gameRepository, GameEventRepository gameEventRepository, GameLiveEventHub hub) {
		this.gameRepository = gameRepository;
		this.gameEventRepository = gameEventRepository;
		this.hub = hub;
	}

	/** 관리자 진행 이벤트를 기록하고, 커밋 후 구독자에게 보낸다. */
	public LiveEventResponse record(Long gameId, LiveEventCreateRequest request) {
		Game game = gameRepository.findByIdForUpdate(gameId)
				.orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));
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
				game.getHomeScore(), game.getAwayScore()));
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

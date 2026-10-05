package com.ballpark.ticketing.game.service;

import com.ballpark.ticketing.game.GameProgress;
import com.ballpark.ticketing.game.dto.StandingResponse;
import com.ballpark.ticketing.game.repository.GameRepository;
import com.ballpark.ticketing.game.schedule.KboTeams;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StandingService {

	private final GameRepository gameRepository;

	public StandingService(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	/** 10개 구단 전체의 순위. 경기가 없는 팀도 0승 0패로 포함한다. */
	public List<StandingResponse> standings() {
		return StandingCalculator.calculate(KboTeams.NAMES,
				gameRepository.findResultsByProgress(GameProgress.FINISHED));
	}
}

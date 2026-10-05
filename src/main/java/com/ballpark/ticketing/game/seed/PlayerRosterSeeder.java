package com.ballpark.ticketing.game.seed;

import com.ballpark.ticketing.game.Player;
import com.ballpark.ticketing.game.repository.PlayerRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 구단별 선수 명단을 DB에 넣는다. 이름은 전부 가상이다(실제 선수 이름과 소속·기록을 연결하지 않으려고 만들었다).
 * 명단이 이미 있으면 아무것도 하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerRosterSeeder implements ApplicationRunner {

	static final List<String> TEAMS = com.ballpark.ticketing.game.schedule.KboTeams.NAMES;
	private static final int PLAYERS_PER_TEAM = 14;
	private static final List<String> SURNAMES = List.of(
			"가", "고", "남", "도", "류", "문", "백", "서", "신", "오", "전", "채", "추", "하");
	private static final List<String> GIVEN_NAMES = List.of(
			"도윤", "서준", "지호", "하린", "수아", "민재", "태윤", "예린", "준서", "시우", "윤아", "건우", "다은", "현우");
	private static final List<String> POSITIONS = List.of(
			"투수", "투수", "투수", "포수", "내야수", "내야수", "내야수", "내야수", "외야수", "외야수", "외야수", "지명타자", "내야수", "외야수");

	private final PlayerRepository playerRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (playerRepository.count() > 0) {
			return;
		}
		List<Player> players = new ArrayList<>();
		for (int team = 0; team < TEAMS.size(); team++) {
			for (int i = 0; i < PLAYERS_PER_TEAM; i++) {
				String name = SURNAMES.get((team * 7 + i * 3) % SURNAMES.size())
						+ GIVEN_NAMES.get((team * 5 + i * 11) % GIVEN_NAMES.size());
				players.add(new Player(TEAMS.get(team), name, i + 1, POSITIONS.get(i)));
			}
		}
		playerRepository.saveAll(players);
		log.info("[roster] 선수 {}명 등록 ({}개 구단)", players.size(), TEAMS.size());
	}
}

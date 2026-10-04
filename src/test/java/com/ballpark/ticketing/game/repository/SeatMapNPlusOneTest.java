package com.ballpark.ticketing.game.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballpark.ticketing.TestcontainersConfiguration;
import com.ballpark.ticketing.game.GameSeat;
import com.ballpark.ticketing.game.Section;
import com.ballpark.ticketing.game.dto.GameCreateRequest;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.game.dto.SeatBulkCreateRequest;
import com.ballpark.ticketing.game.dto.SeatMapItemResponse;
import com.ballpark.ticketing.game.service.GameService;
import com.ballpark.ticketing.game.service.SeatService;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 3단계(조회 API) 설계 과제: 좌석맵 조회가 N+1을 일으키는지 확인하고 고친다.
 * GameSeat에서 Seat, Seat에서 Section으로 가는 연관관계는 모두 LAZY라, 엔티티를 그대로 순회하며
 * seat.getSection()을 부르면 좌석 수만큼 추가 SELECT가 나간다. GameSeatRepository.findSeatMapByGameId는
 * 처음부터 join으로 필요한 컬럼만 한 번에 선택해 이 문제를 피한다. 결과는
 * docs/experiments/v1-03-seat-map-n-plus-1.md에 기록한다.
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class SeatMapNPlusOneTest {

	private static final int SEAT_COUNT = 50;

	@Autowired
	private SectionRepository sectionRepository;

	@Autowired
	private SeatService seatService;

	@Autowired
	private GameService gameService;

	@Autowired
	private GameSeatRepository gameSeatRepository;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Test
	void fetchJoinProjectionAvoidsTheNPlusOneThatLazyLoadingCauses() {
		Section section = sectionRepository.save(new Section("Benchmark N+1", "R", 10_000));
		seatService.createBulk(section.getId(), new SeatBulkCreateRequest(5, 10));
		GameResponse game = gameService.create(new GameCreateRequest(
				"Seoul Comets", "Busan Gulls",
				LocalDateTime.of(2026, 11, 1, 18, 30), LocalDateTime.of(2026, 10, 25, 11, 0)));

		Statistics statistics = statistics();
		statistics.clear();
		List<GameSeat> naive = gameSeatRepository.findAll();
		for (GameSeat gameSeat : naive) {
			// Seat, Section이 LAZY라 각 호출이 추가 SELECT를 유발한다.
			gameSeat.getSeat().getSection().getName();
		}
		long naiveQueryCount = statistics.getPrepareStatementCount();

		statistics.clear();
		List<SeatMapItemResponse> optimized = gameSeatRepository.findSeatMapByGameId(game.id());
		long optimizedQueryCount = statistics.getPrepareStatementCount();

		log.info("[v1-03] 엔티티 순회(N+1): {}건 조회에 SQL {}번", SEAT_COUNT, naiveQueryCount);
		log.info("[v1-03] join DTO 프로젝션: {}건 조회에 SQL {}번", SEAT_COUNT, optimizedQueryCount);

		assertThat(naive).hasSize(SEAT_COUNT);
		assertThat(optimized).hasSize(SEAT_COUNT);
		assertThat(optimizedQueryCount).isEqualTo(1);
		assertThat(naiveQueryCount).isGreaterThan(SEAT_COUNT);
	}

	private Statistics statistics() {
		return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
	}
}

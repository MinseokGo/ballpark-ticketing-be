package com.ballpark.ticketing.common.seed;

import com.ballpark.ticketing.game.dto.GameCreateRequest;
import com.ballpark.ticketing.game.dto.GameResponse;
import com.ballpark.ticketing.game.dto.SeatBulkCreateRequest;
import com.ballpark.ticketing.game.dto.SectionCreateRequest;
import com.ballpark.ticketing.game.dto.SectionResponse;
import com.ballpark.ticketing.game.repository.SectionRepository;
import com.ballpark.ticketing.game.service.GameService;
import com.ballpark.ticketing.game.service.SeatService;
import com.ballpark.ticketing.game.service.SectionService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 로컬 데모용 시드. {@code --spring.profiles.active=local,seed}로 띄울 때만 돈다.
 * 구역 30개(중앙석·1루/3루 필드석·1루/3루 외야석 x A~C x 앞·뒤 2블록), 좌석 22,536석, 경기 5개.
 * 구역 이름 체계는 프론트 지도(StadiumMap)가 해석하므로 바꾸지 말 것.
 * 구단 이름은 실제 KBO 구단을 쓰되 일정은 데모용 임의 값이다(CLAUDE.md 테스트 절의 예외).
 */
@Slf4j
@Component
@Profile("seed")
public class DemoDataSeeder implements ApplicationRunner {

	private static final String[] TIERS = {"A", "B", "C"};
	private static final String[] HALVES = {"1", "2"};

	private final SectionRepository sectionRepository;
	private final SectionService sectionService;
	private final SeatService seatService;
	private final GameService gameService;

	public DemoDataSeeder(
			SectionRepository sectionRepository, SectionService sectionService,
			SeatService seatService, GameService gameService) {
		this.sectionRepository = sectionRepository;
		this.sectionService = sectionService;
		this.seatService = seatService;
		this.gameService = gameService;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (sectionRepository.count() > 0) {
			log.info("[seed] 구역이 이미 있어서 시드를 건너뜀");
			return;
		}
		long start = System.nanoTime();

		List<Zone> zones = zones();
		for (Zone zone : zones) {
			SectionResponse section = sectionService.create(new SectionCreateRequest(zone.name(), zone.tier(), zone.price()));
			seatService.createBulk(section.id(), new SeatBulkCreateRequest(zone.rows(), zone.seatsPerRow()));
		}
		long afterSeats = System.nanoTime();

		List<GameResponse> games = List.of(
				createGame("두산 베어스", "LG 트윈스", "2026-10-10T18:30:00", "2026-10-05T11:00:00"),
				createGame("KIA 타이거즈", "삼성 라이온즈", "2026-10-11T18:30:00", "2026-10-05T11:00:00"),
				createGame("SSG 랜더스", "롯데 자이언츠", "2026-10-15T18:30:00", "2026-10-12T11:00:00"),
				createGame("한화 이글스", "NC 다이노스", "2026-10-16T18:30:00", "2026-10-13T11:00:00"),
				createGame("KT 위즈", "키움 히어로즈", "2026-10-17T18:30:00", "2026-10-14T11:00:00"));
		gameService.openTicketing(games.get(0).id());
		gameService.openTicketing(games.get(1).id());
		long end = System.nanoTime();

		int seats = zones.stream().mapToInt(zone -> zone.rows() * zone.seatsPerRow()).sum();
		log.info(
				"[seed] 구역 {}개, 좌석 {}석, 경기 {}개 생성 — 구역·좌석 {}ms, 경기 {}ms, 합계 {}ms",
				zones.size(), seats, games.size(),
				Duration.ofNanos(afterSeats - start).toMillis(),
				Duration.ofNanos(end - afterSeats).toMillis(),
				Duration.ofNanos(end - start).toMillis());
	}

	private GameResponse createGame(String home, String away, String startAt, String ticketOpenAt) {
		return gameService.create(new GameCreateRequest(
				home, away, LocalDateTime.parse(startAt), LocalDateTime.parse(ticketOpenAt)));
	}

	/** 계열별 A/B/C 층 행 수·블록당 열 수·가격. 한 층은 앞/뒤 2블록으로 나눈다. */
	private static List<Zone> zones() {
		List<Zone> zones = new ArrayList<>();
		addFamily(zones, "중앙석", new int[] {20, 22, 24}, new int[] {26, 30, 34}, new long[] {50_000, 40_000, 30_000});
		addFamily(zones, "1루 필드석", new int[] {18, 20, 22}, new int[] {24, 28, 32}, new long[] {35_000, 28_000, 22_000});
		addFamily(zones, "3루 필드석", new int[] {18, 20, 22}, new int[] {24, 28, 32}, new long[] {35_000, 28_000, 22_000});
		addFamily(zones, "1루 외야석", new int[] {16, 18, 20}, new int[] {48, 54, 60}, new long[] {15_000, 12_000, 9_000});
		addFamily(zones, "3루 외야석", new int[] {16, 18, 20}, new int[] {48, 54, 60}, new long[] {15_000, 12_000, 9_000});
		return zones;
	}

	private static void addFamily(List<Zone> zones, String family, int[] rows, int[] seatsPerHalf, long[] prices) {
		for (int tier = 0; tier < TIERS.length; tier++) {
			for (String half : HALVES) {
				zones.add(new Zone(
						family + " " + TIERS[tier] + "-" + half, TIERS[tier], prices[tier],
						rows[tier], seatsPerHalf[tier]));
			}
		}
	}

	private record Zone(String name, String tier, long price, int rows, int seatsPerRow) {
	}
}

package com.ballpark.ticketing.game.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ballpark.ticketing.TestcontainersConfiguration;
import com.ballpark.ticketing.game.Seat;
import com.ballpark.ticketing.game.SeatPosition;
import com.ballpark.ticketing.game.Section;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 2단계(관리자 API) 설계 과제: 좌석 대량 등록을 JPA saveAll과 JdbcTemplate 배치 삽입 중 무엇으로 할지 수치로 비교한다.
 * Seat는 GenerationType.IDENTITY라 Hibernate가 INSERT마다 생성된 키를 즉시 읽어야 하므로 saveAll도 배치로 묶이지 않는다.
 * 결과는 docs/experiments/v1-02-admin-bulk-insert.md에 기록한다. 성능 수치는 테스트 환경에 따라 달라지므로
 * 이 테스트는 "둘 다 올바르게 삽입되는가"만 검증하고, 수행 시간은 로그로만 남긴다.
 * 다른 서비스 테스트와 컨텍스트(컨테이너)를 공유하므로 @Transactional로 삽입 데이터를 롤백해 서로 오염시키지 않는다.
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class SeatBulkInsertBenchmarkTest {

	private static final int ROW_COUNT = 100;
	private static final int SEATS_PER_ROW = 30;
	private static final int SEAT_COUNT_PER_METHOD = ROW_COUNT * SEATS_PER_ROW;

	@Autowired
	private SectionRepository sectionRepository;

	@Autowired
	private SeatRepository seatRepository;

	@Autowired
	private SeatJdbcRepository seatJdbcRepository;

	@Test
	void comparesSaveAllAgainstJdbcBatchInsert() {
		Section saveAllSection = sectionRepository.save(new Section("Benchmark saveAll", "R", 10_000));
		Section jdbcSection = sectionRepository.save(new Section("Benchmark JDBC batch", "R", 10_000));

		long saveAllElapsedMs = measureSaveAll(saveAllSection);
		long jdbcBatchElapsedMs = measureJdbcBatch(jdbcSection);

		log.info("[v1-02] saveAll {}건 삽입: {}ms", SEAT_COUNT_PER_METHOD, saveAllElapsedMs);
		log.info("[v1-02] JDBC batch {}건 삽입: {}ms", SEAT_COUNT_PER_METHOD, jdbcBatchElapsedMs);

		assertThat(seatRepository.count()).isEqualTo(SEAT_COUNT_PER_METHOD * 2L);
	}

	private long measureSaveAll(Section section) {
		List<Seat> seats = new ArrayList<>(SEAT_COUNT_PER_METHOD);
		for (int rowNo = 1; rowNo <= ROW_COUNT; rowNo++) {
			for (int seatNo = 1; seatNo <= SEATS_PER_ROW; seatNo++) {
				seats.add(new Seat(section, rowNo, seatNo));
			}
		}
		long start = System.nanoTime();
		seatRepository.saveAll(seats);
		return (System.nanoTime() - start) / 1_000_000;
	}

	private long measureJdbcBatch(Section section) {
		List<SeatPosition> positions = new ArrayList<>(SEAT_COUNT_PER_METHOD);
		for (int rowNo = 1; rowNo <= ROW_COUNT; rowNo++) {
			for (int seatNo = 1; seatNo <= SEATS_PER_ROW; seatNo++) {
				positions.add(new SeatPosition(rowNo, seatNo));
			}
		}
		long start = System.nanoTime();
		seatJdbcRepository.batchInsert(section.getId(), positions);
		return (System.nanoTime() - start) / 1_000_000;
	}
}

package com.ballpark.ticketing.game.repository;

import com.ballpark.ticketing.game.SeatPosition;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Seat는 IDENTITY 생성 전략이라 JPA saveAll이 배치로 묶이지 않는다(Hibernate가 INSERT마다 생성된 키를 즉시 읽어야 하기 때문).
 * 대량 등록에서는 JdbcTemplate.batchUpdate로 우회한다. docs/experiments/v1-02-admin-bulk-insert.md 참고.
 */
@Repository
public class SeatJdbcRepository {

	private static final String INSERT_SQL = "INSERT INTO seat (section_id, row_no, seat_no) VALUES (?, ?, ?)";
	private static final int BATCH_CHUNK_SIZE = 500;

	private final JdbcTemplate jdbcTemplate;

	public SeatJdbcRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public int batchInsert(Long sectionId, List<SeatPosition> positions) {
		if (positions.isEmpty()) {
			return 0;
		}
		jdbcTemplate.batchUpdate(INSERT_SQL, positions, BATCH_CHUNK_SIZE, (ps, position) -> {
			ps.setLong(1, sectionId);
			ps.setInt(2, position.rowNo());
			ps.setInt(3, position.seatNo());
		});
		return positions.size();
	}
}

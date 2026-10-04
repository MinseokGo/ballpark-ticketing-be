package com.ballpark.ticketing.game.repository;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * GameSeat도 IDENTITY 생성 전략이라 saveAll 배치가 적용되지 않는다. SeatJdbcRepository와 같은 이유.
 */
@Repository
public class GameSeatJdbcRepository {

	private static final String INSERT_SQL =
			"INSERT INTO game_seat (game_id, seat_id, status) VALUES (?, ?, 'AVAILABLE')";
	private static final int BATCH_CHUNK_SIZE = 500;

	private final JdbcTemplate jdbcTemplate;

	public GameSeatJdbcRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public int batchInsert(Long gameId, List<Long> seatIds) {
		if (seatIds.isEmpty()) {
			return 0;
		}
		jdbcTemplate.batchUpdate(INSERT_SQL, seatIds, BATCH_CHUNK_SIZE, (ps, seatId) -> {
			ps.setLong(1, gameId);
			ps.setLong(2, seatId);
		});
		return seatIds.size();
	}
}

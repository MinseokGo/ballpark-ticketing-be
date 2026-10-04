package com.ballpark.ticketing.game.repository;

import com.ballpark.ticketing.game.Seat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SeatRepository extends JpaRepository<Seat, Long> {

	@Query("select s.id from Seat s")
	List<Long> findAllIds();
}

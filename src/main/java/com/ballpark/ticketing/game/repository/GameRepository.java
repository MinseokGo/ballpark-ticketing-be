package com.ballpark.ticketing.game.repository;

import com.ballpark.ticketing.game.Game;
import com.ballpark.ticketing.game.GameProgress;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRepository extends JpaRepository<Game, Long> {

	Page<Game> findByProgress(GameProgress progress, Pageable pageable);

	long countByStartAtBetween(LocalDateTime from, LocalDateTime to);

	// 진행 이벤트를 기록할 때 경기 행을 잠가서, 같은 경기의 seq가 겹치지 않게 한다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select g from Game g where g.id = :id")
	Optional<Game> findByIdForUpdate(@Param("id") Long id);
}

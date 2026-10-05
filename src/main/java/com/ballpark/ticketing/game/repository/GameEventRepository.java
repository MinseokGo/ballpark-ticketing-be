package com.ballpark.ticketing.game.repository;

import com.ballpark.ticketing.game.GameEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameEventRepository extends JpaRepository<GameEvent, Long> {

	List<GameEvent> findByGame_IdAndSeqGreaterThanOrderBySeqAsc(Long gameId, int seq);
}

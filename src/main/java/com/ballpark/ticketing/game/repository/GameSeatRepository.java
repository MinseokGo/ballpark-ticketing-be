package com.ballpark.ticketing.game.repository;

import com.ballpark.ticketing.game.GameSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSeatRepository extends JpaRepository<GameSeat, Long> {
}

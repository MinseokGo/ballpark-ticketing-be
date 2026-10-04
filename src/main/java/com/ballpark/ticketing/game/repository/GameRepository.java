package com.ballpark.ticketing.game.repository;

import com.ballpark.ticketing.game.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
}

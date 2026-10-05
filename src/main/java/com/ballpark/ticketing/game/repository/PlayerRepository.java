package com.ballpark.ticketing.game.repository;

import com.ballpark.ticketing.game.Player;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {

	List<Player> findByTeamNameOrderByBackNumberAsc(String teamName);
}

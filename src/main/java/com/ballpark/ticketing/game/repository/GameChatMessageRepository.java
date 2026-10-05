package com.ballpark.ticketing.game.repository;

import com.ballpark.ticketing.game.GameChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameChatMessageRepository extends JpaRepository<GameChatMessage, Long> {

	List<GameChatMessage> findTop50ByGame_IdOrderByIdDesc(Long gameId);

	List<GameChatMessage> findByGame_IdAndIdGreaterThanOrderByIdAsc(Long gameId, Long id);
}
